package com.example.portfoliomanager.data.repository

import com.example.portfoliomanager.data.local.UserPreferencesRepository
import com.example.portfoliomanager.data.mock.MockPortfolioData
import com.example.portfoliomanager.data.model.AccountSummary
import com.example.portfoliomanager.data.model.Balances
import com.example.portfoliomanager.data.model.GeographyAllocation
import com.example.portfoliomanager.data.model.MarginRequirements
import com.example.portfoliomanager.data.model.PortfolioData
import com.example.portfoliomanager.data.model.Position
import com.example.portfoliomanager.data.model.SectorAllocation
import com.example.portfoliomanager.data.model.Trade
import com.example.portfoliomanager.data.parser.BrokerCsvParserRegistry
import com.example.portfoliomanager.data.remote.AppLogger
import com.example.portfoliomanager.data.remote.IbkrFlexApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

interface PortfolioRepository {
    /**
     * Fetches portfolio data from IBKR Flex API or saved CSV data if valid.
     * Falls back seamlessly to rich mock portfolio data if credentials and CSV are missing or if API fetch fails.
     */
    suspend fun getPortfolioData(
        token: String? = null,
        queryId: String? = null,
        forceRefresh: Boolean = false
    ): Result<PortfolioData>

    /**
     * Flow emitting portfolio data results.
     */
    fun getPortfolioDataFlow(
        token: String? = null,
        queryId: String? = null
    ): Flow<Result<PortfolioData>>

    /**
     * Saves user's IBKR API credentials.
     */
    suspend fun saveIbkrCredentials(token: String, queryId: String)

    /**
     * Emits stored IBKR API credentials (token to queryId).
     */
    fun getIbkrCredentials(): Flow<Pair<String, String>>

    /**
     * Imports broker CSV/XLSX content, parses it, combines it with existing imports, and persists it locally.
     * Returns a pair of Broker Name (or combined brokers) to [PortfolioData].
     */
    suspend fun importPortfolioCsv(csvContent: String): Result<Pair<String, PortfolioData>>

    /**
     * Clears saved imported CSV/XLSX data.
     */
    suspend fun clearImportedCsvData()

    /**
     * Resets manual trade additions, edits, and deleted trade filters without removing saved CSV/XLSX imports.
     */
    suspend fun resetManualEditsAndDeletedTrades(): Result<PortfolioData>

    /**
     * Emits saved imported CSV content string.
     */
    fun getSavedImportedCsv(): Flow<String?>

    /**
     * Attaches user preferences repository for persistence if not already attached.
     */
    fun attachPreferencesRepository(preferencesRepository: UserPreferencesRepository)

    /**
     * Adds a manual purchase/trade to a position and persists it.
     */
    suspend fun addManualTrade(positionId: String, trade: Trade): Result<PortfolioData>

    /**
     * Edits/updates an existing trade and persists changes.
     */
    suspend fun editTrade(trade: Trade): Result<PortfolioData>

    /**
     * Deletes a trade by ID and persists changes.
     */
    suspend fun deleteTrade(tradeId: String): Result<PortfolioData>

    /**
     * Saves or removes an alternative ticker for a position symbol and refreshes portfolio data.
     */
    suspend fun saveTickerOverride(originalSymbol: String, alternativeTicker: String): Result<PortfolioData>
}

class PortfolioRepositoryImpl(
    private val apiClient: IbkrFlexApiClient = IbkrFlexApiClient(),
    private var preferencesRepository: UserPreferencesRepository? = null,
    private val parserRegistry: BrokerCsvParserRegistry = BrokerCsvParserRegistry(),
    private val marketDataRepository: MarketDataRepository = MarketDataRepositoryImpl()
) : PortfolioRepository {

    companion object {
        private const val IMPORT_DELIMITER = "\n---BROKER_IMPORT_SPLITTER---\n"
    }

    private var cachedPortfolioData: PortfolioData? = null

    override fun attachPreferencesRepository(preferencesRepository: UserPreferencesRepository) {
        if (this.preferencesRepository == null) {
            this.preferencesRepository = preferencesRepository
        }
    }

    override suspend fun getPortfolioData(
        token: String?,
        queryId: String?,
        forceRefresh: Boolean
    ): Result<PortfolioData> {
        if (!forceRefresh && cachedPortfolioData != null) {
            return Result.success(cachedPortfolioData!!)
        }

        val rawDataResult = run {
            val savedCsv = preferencesRepository?.importedCsvFlow?.first()
            if (!savedCsv.isNullOrBlank()) {
                val parseResult = parseSavedImportedFiles(savedCsv)
                if (parseResult.isSuccess) {
                    parseResult
                } else {
                    fetchFromApiOrMock(token, queryId)
                }
            } else {
                fetchFromApiOrMock(token, queryId)
            }
        }

        val enrichedResult = rawDataResult.map { rawData ->
            val dataWithOverrides = applySavedTickerOverrides(rawData)
            if (rawData.isMockData) {
                dataWithOverrides
            } else {
                marketDataRepository.enrichPortfolioWithLivePrices(dataWithOverrides)
            }
        }

        return enrichedResult.map { enrichedData ->
            val dataWithManualTrades = applySavedManualTrades(enrichedData)
            cachedPortfolioData = dataWithManualTrades
            dataWithManualTrades
        }
    }

    private suspend fun applySavedTickerOverrides(data: PortfolioData): PortfolioData {
        val overrides = preferencesRepository?.tickerOverridesFlow?.first() ?: emptyMap()
        if (overrides.isEmpty()) return data

        val updatedPositions = data.positions.map { pos ->
            val alt = overrides[pos.symbol.trim().uppercase()]
            if (!alt.isNullOrBlank()) {
                pos.copy(alternativeTicker = alt)
            } else {
                pos
            }
        }
        return data.copy(positions = updatedPositions)
    }

    private fun parseSavedImportedFiles(rawSavedString: String): Result<PortfolioData> {
        val fileContents = rawSavedString.split(IMPORT_DELIMITER)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (fileContents.isEmpty()) {
            return Result.failure(IllegalArgumentException("Žiadne uložené výpisy."))
        }

        val contentsByBroker = mutableMapOf<String, MutableList<String>>()
        for (content in fileContents) {
            try {
                val result = parserRegistry.parse(content)
                contentsByBroker.getOrPut(result.brokerName) { mutableListOf() }.add(content)
            } catch (e: Exception) {
                AppLogger.e("PortfolioRepository", "Failed to parse saved import file: ${e.message}", e)
            }
        }

        if (contentsByBroker.isEmpty()) {
            return Result.failure(IllegalArgumentException("Nepodarilo sa načítať uložené výpisy."))
        }

        val parseResults = mutableListOf<BrokerCsvParserRegistry.ParseResult>()
        for ((brokerName, contents) in contentsByBroker) {
            if (brokerName.equals("Trading 212", ignoreCase = true)) {
                // For Trading 212, join all contents and parse them together (with trade deduplication)
                val combinedContent = contents.joinToString("\n")
                try {
                    val result = parserRegistry.parse(combinedContent)
                    parseResults.add(result)
                } catch (e: Exception) {
                    AppLogger.e("PortfolioRepository", "Failed to parse combined Trading 212 files: ${e.message}", e)
                }
            } else {
                // For other brokers, parse the latest file
                val latestContent = contents.last()
                try {
                    val result = parserRegistry.parse(latestContent)
                    parseResults.add(result)
                } catch (e: Exception) {
                    AppLogger.e("PortfolioRepository", "Failed to parse $brokerName file: ${e.message}", e)
                }
            }
        }

        if (parseResults.isEmpty()) {
            return Result.failure(IllegalArgumentException("Nepodarilo sa spracovať žiadny výpis."))
        }

        val mergedData = mergePortfolioData(parseResults)
        return Result.success(mergedData)
    }

    private suspend fun fetchFromApiOrMock(
        token: String?,
        queryId: String?
    ): Result<PortfolioData> {
        val effectiveToken = token?.trim()?.ifEmpty { null }
            ?: preferencesRepository?.credentialsFlow?.first()?.token?.ifEmpty { null }

        val effectiveQueryId = queryId?.trim()?.ifEmpty { null }
            ?: preferencesRepository?.credentialsFlow?.first()?.queryId?.ifEmpty { null }

        if (effectiveToken.isNullOrEmpty() || effectiveQueryId.isNullOrEmpty()) {
            val mockData = MockPortfolioData.getSamplePortfolio()
            cachedPortfolioData = mockData
            return Result.success(mockData)
        }

        return try {
            val liveData = apiClient.fetchPortfolioData(effectiveToken, effectiveQueryId)
            cachedPortfolioData = liveData
            Result.success(liveData)
        } catch (e: Exception) {
            AppLogger.e("PortfolioRepository", "Failed to fetch IBKR live portfolio data: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getPortfolioDataFlow(
        token: String?,
        queryId: String?
    ): Flow<Result<PortfolioData>> = flow {
        emit(getPortfolioData(token, queryId, forceRefresh = true))
    }

    override suspend fun saveIbkrCredentials(token: String, queryId: String) {
        preferencesRepository?.saveCredentials(token, queryId)
        cachedPortfolioData = null
    }

    override fun getIbkrCredentials(): Flow<Pair<String, String>> {
        return preferencesRepository?.credentialsFlow?.map { Pair(it.token, it.queryId) }
            ?: flow { emit(Pair("", "")) }
    }

    override suspend fun importPortfolioCsv(csvContent: String): Result<Pair<String, PortfolioData>> {
        return try {
            val newParseResult = parserRegistry.parse(csvContent)

            val existingSavedString = preferencesRepository?.importedCsvFlow?.first() ?: ""
            val existingContents = existingSavedString.split(IMPORT_DELIMITER)
                .map { it.trim() }
                .filter { it.isNotBlank() }

            // Filter existing contents:
            // For Trading 212, keep existing saved files unless identical to new input.
            // For other brokers, replace previous files for that broker.
            val filteredContents = existingContents.filter { content ->
                try {
                    val existingResult = parserRegistry.parse(content)
                    if (existingResult.brokerName.equals(newParseResult.brokerName, ignoreCase = true)) {
                        if (newParseResult.brokerName.equals("Trading 212", ignoreCase = true)) {
                            content.trim() != csvContent.trim()
                        } else {
                            false
                        }
                    } else {
                        true
                    }
                } catch (_: Exception) {
                    true
                }
            }

            val updatedContents = filteredContents + csvContent.trim()
            val combinedSavedString = updatedContents.joinToString(IMPORT_DELIMITER)

            val parseSavedResult = parseSavedImportedFiles(combinedSavedString)
            val mergedPortfolio = parseSavedResult.getOrThrow()

            // Unmark any imported trade IDs from deletedTrades so re-imported trades are not suppressed
            val importedTradeIds = mergedPortfolio.positions.flatMap { it.trades }.map { it.id }.toSet()
            if (importedTradeIds.isNotEmpty()) {
                val currentDeletedJson = preferencesRepository?.deletedTradesFlow?.first() ?: ""
                val currentDeletedIds = deserializeDeletedTradeIds(currentDeletedJson)
                val newDeletedIds = currentDeletedIds - importedTradeIds
                if (newDeletedIds.size != currentDeletedIds.size) {
                    preferencesRepository?.saveDeletedTrades(serializeDeletedTradeIds(newDeletedIds))
                }
            }

            val dataWithOverrides = applySavedTickerOverrides(mergedPortfolio)
            val enrichedData = marketDataRepository.enrichPortfolioWithLivePrices(dataWithOverrides)
            val finalData = applySavedManualTrades(enrichedData)

            preferencesRepository?.saveImportedCsv(combinedSavedString)
            cachedPortfolioData = finalData

            val brokerNames = updatedContents.mapNotNull { content ->
                try { parserRegistry.parse(content).brokerName } catch (_: Exception) { null }
            }.distinct()

            val displayBrokerName = if (brokerNames.size > 1) {
                "spojených výpisov (" + brokerNames.joinToString(", ") + ")"
            } else {
                newParseResult.brokerName
            }

            Result.success(Pair(displayBrokerName, finalData))
        } catch (e: Exception) {
            AppLogger.e("PortfolioRepository", "Failed to import CSV/XLSX: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun mergePortfolioData(results: List<BrokerCsvParserRegistry.ParseResult>): PortfolioData {
        val brokerNames = results.map { it.brokerName }.distinct()
        val rawPositions = results.flatMap { it.portfolioData.positions }

        // Group positions by base symbol (e.g. QDV5.DE and QDV5 group together)
        val mergedPositions = rawPositions.groupBy { it.symbol.trim().uppercase().substringBefore('.') }.map { (baseSymbol, posList) ->
            if (posList.size == 1) {
                posList.first()
            } else {
                val primaryPos = posList.find { it.symbol.contains('.') && it.quantity > 0 }
                    ?: posList.find { it.quantity > 0 }
                    ?: posList.find { it.symbol.contains('.') }
                    ?: posList.first()

                val allTrades = posList.flatMap { it.trades }.distinctBy { it.id }

                if (allTrades.isNotEmpty()) {
                    val buyTrades = allTrades.filter { it.buySell.uppercase() == "BUY" }
                    val sellTrades = allTrades.filter { it.buySell.uppercase() == "SELL" }
                    val totalQty = maxOf(0.0, buyTrades.sumOf { it.quantity } - sellTrades.sumOf { it.quantity })
                    val buyCost = buyTrades.sumOf { (it.quantity * it.price) + it.commission }
                    val totalBuyQty = buyTrades.sumOf { it.quantity }
                    val costPrice = if (totalBuyQty > 0) buyCost / totalBuyQty else primaryPos.costBasisPrice
                    val totalMktVal = totalQty * primaryPos.markPrice
                    val totalPnl = totalMktVal - (totalQty * costPrice)
                    val pnlPct = if (totalQty * costPrice > 0) (totalPnl / (totalQty * costPrice)) * 100.0 else 0.0

                    primaryPos.copy(
                        quantity = totalQty,
                        costBasisPrice = costPrice,
                        marketValue = totalMktVal,
                        totalPnl = totalPnl,
                        pnlPercentage = pnlPct,
                        trades = allTrades.sortedByDescending { it.date }
                    )
                } else {
                    val totalQty = posList.maxOf { it.quantity }
                    val totalMktVal = totalQty * primaryPos.markPrice
                    val totalPnl = posList.sumOf { it.totalPnl }
                    val costPrice = primaryPos.costBasisPrice
                    val pnlPct = if (totalQty * costPrice > 0) (totalPnl / (totalQty * costPrice)) * 100.0 else 0.0

                    primaryPos.copy(
                        quantity = totalQty,
                        costBasisPrice = costPrice,
                        marketValue = totalMktVal,
                        totalPnl = totalPnl,
                        pnlPercentage = pnlPct,
                        trades = emptyList()
                    )
                }
            }
        }

        val totalCash = results.sumOf { it.portfolioData.balances.totalCash }
        val totalMktVal = mergedPositions.sumOf { it.marketValue }
        val netLiquidation = totalMktVal + totalCash
        val totalUnrealizedPnl = mergedPositions.filter { it.quantity > 0.0 }.sumOf { it.totalPnl }
        val totalRealizedPnl = results.sumOf { it.portfolioData.balances.realizedPnl }
        val combinedTotalPnl = totalUnrealizedPnl + totalRealizedPnl
        val portfolioCostBasis = (netLiquidation - combinedTotalPnl).takeIf { it > 0.0 }
            ?: mergedPositions.filter { it.quantity > 0.0 }.sumOf { it.costBasisPrice * it.quantity }.takeIf { it > 0.0 }
            ?: 1.0
        val totalPnlPct = (combinedTotalPnl / portfolioCostBasis) * 100.0
        val currency = results.firstOrNull()?.portfolioData?.accountSummary?.currency ?: "EUR"

        val sectorAllocations = calculateSectorAllocations(mergedPositions)
        val geographyAllocations = calculateGeographyAllocations(mergedPositions)

        return PortfolioData(
            accountSummary = AccountSummary(
                accountId = "MULTI-BROKER",
                accountName = "Kombinované portfólio (" + brokerNames.joinToString(", ") + ")",
                currency = currency,
                netLiquidation = netLiquidation,
                totalCashValue = totalCash,
                settledCash = totalCash,
                accruedCash = 0.0,
                buyingPower = totalCash,
                dailyPnl = 0.0,
                dailyPnlPercentage = 0.0,
                totalPnl = combinedTotalPnl,
                totalPnlPercentage = totalPnlPct
            ),
            balances = Balances(
                totalCash = totalCash,
                settledCash = totalCash,
                accruedCash = 0.0,
                unrealizedPnl = totalUnrealizedPnl,
                realizedPnl = totalRealizedPnl,
                netLiquidation = netLiquidation
            ),
            marginSummary = MarginRequirements(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            positions = mergedPositions,
            sectorAllocations = sectorAllocations,
            geographyAllocations = geographyAllocations,
            isMockData = false,
            referenceCode = "MULTI-IMPORT"
        )
    }

    private fun calculateSectorAllocations(positions: List<Position>): List<SectorAllocation> {
        val totalValue = positions.sumOf { it.marketValue }
        if (totalValue <= 0.0) return emptyList()

        return positions.groupBy { it.sector }.map { (sector, posList) ->
            val sectorVal = posList.sumOf { it.marketValue }
            val pct = (sectorVal / totalValue) * 100.0
            SectorAllocation(
                sector = sector,
                value = sectorVal,
                percentage = pct,
                colorHex = mapSectorColor(sector)
            )
        }.sortedByDescending { it.value }
    }

    private fun calculateGeographyAllocations(positions: List<Position>): List<GeographyAllocation> {
        val totalValue = positions.sumOf { it.marketValue }
        if (totalValue <= 0.0) return emptyList()

        return positions.groupBy { it.country }.map { (country, posList) ->
            val countryVal = posList.sumOf { it.marketValue }
            val pct = (countryVal / totalValue) * 100.0
            GeographyAllocation(
                country = country,
                value = countryVal,
                percentage = pct
            )
        }.sortedByDescending { it.value }
    }

    private fun mapSectorColor(sector: String): String {
        return when (sector) {
            "Technology" -> "#4CAF50"
            "Consumer Discretionary" -> "#2196F3"
            "Consumer Staples" -> "#FF9800"
            "Healthcare" -> "#E91E63"
            "Telecommunications" -> "#9C27B0"
            "Broad ETF" -> "#00BCD4"
            else -> "#607D8B"
        }
    }

    override suspend fun clearImportedCsvData() {
        preferencesRepository?.clearImportedCsv()
        preferencesRepository?.clearManualTrades()
        preferencesRepository?.clearDeletedTrades()
        cachedPortfolioData = null
    }

    override suspend fun resetManualEditsAndDeletedTrades(): Result<PortfolioData> {
        return try {
            preferencesRepository?.clearManualTrades()
            preferencesRepository?.clearDeletedTrades()
            cachedPortfolioData = null
            getPortfolioData(forceRefresh = true)
        } catch (e: Exception) {
            AppLogger.e("PortfolioRepository", "Failed to reset manual edits: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getSavedImportedCsv(): Flow<String?> {
        return preferencesRepository?.importedCsvFlow ?: flow { emit(null) }
    }

    override suspend fun saveTickerOverride(
        originalSymbol: String,
        alternativeTicker: String
    ): Result<PortfolioData> {
        return try {
            preferencesRepository?.saveTickerOverride(originalSymbol, alternativeTicker)
            cachedPortfolioData = null
            getPortfolioData(forceRefresh = true)
        } catch (e: Exception) {
            AppLogger.e("PortfolioRepository", "Failed to save ticker override: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun addManualTrade(
        positionId: String,
        trade: Trade
    ): Result<PortfolioData> {
        return try {
            val savedJson = preferencesRepository?.manualTradesFlow?.first() ?: ""
            val existingTrades = deserializeTrades(savedJson)
            val updatedTrades = (existingTrades + trade).distinctBy { it.id }
            val newJson = serializeTrades(updatedTrades)
            preferencesRepository?.saveManualTrades(newJson)

            val deletedJson = preferencesRepository?.deletedTradesFlow?.first() ?: ""
            val deletedIds = deserializeDeletedTradeIds(deletedJson)
            if (trade.id in deletedIds) {
                val newDeletedJson = serializeDeletedTradeIds(deletedIds - trade.id)
                preferencesRepository?.saveDeletedTrades(newDeletedJson)
            }

            val baseData = fetchRawBaseData()
            val enrichedData = applySavedManualTrades(baseData, listOf(trade))
            cachedPortfolioData = enrichedData
            Result.success(enrichedData)
        } catch (e: Exception) {
            AppLogger.e("PortfolioRepository", "Failed to add manual trade: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun editTrade(
        trade: Trade
    ): Result<PortfolioData> {
        return try {
            val savedJson = preferencesRepository?.manualTradesFlow?.first() ?: ""
            val existingTrades = deserializeTrades(savedJson).toMutableList()
            val index = existingTrades.indexOfFirst { it.id == trade.id }
            if (index >= 0) {
                existingTrades[index] = trade
            } else {
                existingTrades.add(trade)
            }
            val newJson = serializeTrades(existingTrades)
            preferencesRepository?.saveManualTrades(newJson)

            val deletedJson = preferencesRepository?.deletedTradesFlow?.first() ?: ""
            val deletedIds = deserializeDeletedTradeIds(deletedJson)
            if (trade.id in deletedIds) {
                val newDeletedJson = serializeDeletedTradeIds(deletedIds - trade.id)
                preferencesRepository?.saveDeletedTrades(newDeletedJson)
            }

            val baseData = fetchRawBaseData()
            val enrichedData = applySavedManualTrades(baseData, listOf(trade))
            cachedPortfolioData = enrichedData
            Result.success(enrichedData)
        } catch (e: Exception) {
            AppLogger.e("PortfolioRepository", "Failed to edit trade: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteTrade(
        tradeId: String
    ): Result<PortfolioData> {
        return try {
            val savedJson = preferencesRepository?.manualTradesFlow?.first() ?: ""
            val existingTrades = deserializeTrades(savedJson).filterNot { it.id == tradeId }
            val newJson = serializeTrades(existingTrades)
            preferencesRepository?.saveManualTrades(newJson)

            val deletedJson = preferencesRepository?.deletedTradesFlow?.first() ?: ""
            val deletedIds = deserializeDeletedTradeIds(deletedJson) + tradeId
            val newDeletedJson = serializeDeletedTradeIds(deletedIds)
            preferencesRepository?.saveDeletedTrades(newDeletedJson)

            val baseData = fetchRawBaseData()
            val enrichedData = applySavedManualTrades(baseData)
            cachedPortfolioData = enrichedData
            Result.success(enrichedData)
        } catch (e: Exception) {
            AppLogger.e("PortfolioRepository", "Failed to delete trade: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchRawBaseData(): PortfolioData {
        val savedCsv = preferencesRepository?.importedCsvFlow?.first()
        val rawData = if (!savedCsv.isNullOrBlank()) {
            parseSavedImportedFiles(savedCsv).getOrElse { fetchFromApiOrMock(null, null).getOrElse { MockPortfolioData.getSamplePortfolio() } }
        } else {
            fetchFromApiOrMock(null, null).getOrElse { MockPortfolioData.getSamplePortfolio() }
        }
        val dataWithOverrides = applySavedTickerOverrides(rawData)
        return if (rawData.isMockData) {
            dataWithOverrides
        } else {
            try {
                marketDataRepository.enrichPortfolioWithLivePrices(dataWithOverrides)
            } catch (e: Exception) {
                dataWithOverrides
            }
        }
    }

    private suspend fun applySavedManualTrades(data: PortfolioData, extraTrades: List<Trade> = emptyList()): PortfolioData {
        val savedJson = preferencesRepository?.manualTradesFlow?.first() ?: ""
        val deletedJson = preferencesRepository?.deletedTradesFlow?.first() ?: ""
        val savedTrades = (deserializeTrades(savedJson) + extraTrades).distinctBy { it.id }
        val deletedTradeIds = deserializeDeletedTradeIds(deletedJson)

        if (savedTrades.isEmpty() && deletedTradeIds.isEmpty()) return data

        val tradesBySymbol = savedTrades.groupBy { it.symbol.trim().uppercase() }

        val updatedPositions = data.positions.map { pos ->
            val posSymbol = pos.symbol.trim().uppercase()
            val posBaseSymbol = posSymbol.substringBefore('.')
            val savedPosTrades = tradesBySymbol[posSymbol]
                ?: tradesBySymbol[posBaseSymbol]
                ?: tradesBySymbol.entries.find { (key, _) ->
                    key == posSymbol || key.substringBefore('.') == posBaseSymbol
                }?.value
                ?: emptyList()

            val activeBaseTrades = pos.trades.filterNot { it.id in deletedTradeIds }

            val finalTrades = activeBaseTrades.map { baseTrade ->
                savedPosTrades.find { it.id == baseTrade.id } ?: baseTrade
            }.toMutableList()

            savedPosTrades.forEach { savedTrade ->
                if (savedTrade.id !in deletedTradeIds) {
                    val alreadyHasId = finalTrades.any { it.id == savedTrade.id }
                    val isDuplicateOfBase = finalTrades.any { base ->
                        val sameSymbol = base.symbol.equals(savedTrade.symbol, ignoreCase = true) ||
                                base.symbol.substringBefore('.').equals(savedTrade.symbol.substringBefore('.'), ignoreCase = true)
                        val sameType = base.buySell.equals(savedTrade.buySell, ignoreCase = true)
                        val sameDate = base.date == savedTrade.date ||
                                (base.date.length >= 10 && savedTrade.date.length >= 10 && base.date.take(10) == savedTrade.date.take(10))
                        val sameQty = kotlin.math.abs(base.quantity - savedTrade.quantity) < 0.0001
                        val samePrice = kotlin.math.abs(base.price - savedTrade.price) < 0.01

                        sameSymbol && sameType && sameDate && sameQty && samePrice
                    }
                    if (!alreadyHasId && !isDuplicateOfBase) {
                        finalTrades.add(savedTrade)
                    }
                }
            }

            if (finalTrades == pos.trades) {
                pos
            } else {
                val buyTrades = finalTrades.filter { it.buySell.uppercase() == "BUY" }
                val sellTrades = finalTrades.filter { it.buySell.uppercase() == "SELL" }
                val tradeQty = maxOf(0.0, buyTrades.sumOf { it.quantity } - sellTrades.sumOf { it.quantity })

                val newQty = if (pos.trades.isNotEmpty()) {
                    tradeQty
                } else {
                    if (kotlin.math.abs(tradeQty - pos.quantity) < 0.001) {
                        tradeQty
                    } else if (tradeQty > 0) {
                        pos.quantity + tradeQty
                    } else {
                        pos.quantity
                    }
                }

                val buyCost = buyTrades.sumOf { (it.quantity * it.price) + it.commission }
                val totalBuyQty = buyTrades.sumOf { it.quantity }
                val newCostBasisPrice = if (totalBuyQty > 0) buyCost / totalBuyQty else pos.costBasisPrice
                val newMarketValue = newQty * pos.markPrice
                val newTotalPnl = newMarketValue - (newQty * newCostBasisPrice)
                val newPnlPct = if (newQty * newCostBasisPrice > 0) (newTotalPnl / (newQty * newCostBasisPrice)) * 100.0 else 0.0

                pos.copy(
                    quantity = newQty,
                    costBasisPrice = newCostBasisPrice,
                    marketValue = newMarketValue,
                    totalPnl = newTotalPnl,
                    pnlPercentage = newPnlPct,
                    trades = finalTrades
                )
            }
        }

        // Add brand-new positions for manual trades of symbols that do not exist in data.positions
        val existingSymbolsUpper = updatedPositions.map { it.symbol.trim().uppercase() }.toSet()
        val newPositions = mutableListOf<Position>()

        for ((symbolUpper, savedPosTrades) in tradesBySymbol) {
            val matchesExisting = existingSymbolsUpper.any {
                it == symbolUpper || it.substringBefore('.') == symbolUpper.substringBefore('.')
            }
            if (!matchesExisting) {
                val activeTrades = savedPosTrades.filterNot { it.id in deletedTradeIds }
                if (activeTrades.isNotEmpty()) {
                    val buyTrades = activeTrades.filter { it.buySell.uppercase() == "BUY" }
                    val sellTrades = activeTrades.filter { it.buySell.uppercase() == "SELL" }
                    val newQty = maxOf(0.0, buyTrades.sumOf { it.quantity } - sellTrades.sumOf { it.quantity })
                    if (newQty > 0 || activeTrades.isNotEmpty()) {
                        val totalBuyQty = buyTrades.sumOf { it.quantity }
                        val buyCost = buyTrades.sumOf { (it.quantity * it.price) + it.commission }
                        val costBasisPrice = if (totalBuyQty > 0) buyCost / totalBuyQty else (activeTrades.firstOrNull()?.price ?: 0.0)
                        val markPrice = activeTrades.lastOrNull()?.price ?: costBasisPrice
                        val marketValue = newQty * markPrice
                        val totalPnl = marketValue - (newQty * costBasisPrice)
                        val pnlPct = if (newQty * costBasisPrice > 0) (totalPnl / (newQty * costBasisPrice)) * 100.0 else 0.0

                        newPositions.add(
                            Position(
                                id = "manual_pos_$symbolUpper",
                                symbol = symbolUpper,
                                description = symbolUpper,
                                assetCategory = com.example.portfoliomanager.data.model.AssetCategory.STK,
                                quantity = newQty,
                                costBasisPrice = costBasisPrice,
                                markPrice = markPrice,
                                marketValue = marketValue,
                                dailyPnl = 0.0,
                                totalPnl = totalPnl,
                                pnlPercentage = pnlPct,
                                sector = "Other",
                                country = "US",
                                currency = "EUR",
                                trades = activeTrades
                            )
                        )
                    }
                }
            }
        }

        val allPositions = updatedPositions + newPositions
        val totalCash = data.balances.totalCash
        val totalMktVal = allPositions.sumOf { it.marketValue }
        val netLiquidation = totalMktVal + totalCash
        val totalPnl = allPositions.sumOf { it.totalPnl }
        val totalCostBasis = allPositions.sumOf { it.quantity * it.costBasisPrice }
        val totalPnlPct = if (totalCostBasis > 0.0) (totalPnl / totalCostBasis) * 100.0 else 0.0

        return data.copy(
            accountSummary = data.accountSummary.copy(
                netLiquidation = netLiquidation,
                totalPnl = totalPnl,
                totalPnlPercentage = totalPnlPct
            ),
            balances = data.balances.copy(
                netLiquidation = netLiquidation,
                unrealizedPnl = totalPnl
            ),
            positions = allPositions,
            sectorAllocations = calculateSectorAllocations(allPositions),
            geographyAllocations = calculateGeographyAllocations(allPositions)
        )
    }

    private fun serializeTrades(trades: List<Trade>): String {
        return try {
            val jsonArray = JSONArray()
            for (trade in trades) {
                val obj = JSONObject().apply {
                    put("id", trade.id)
                    put("symbol", trade.symbol)
                    put("date", trade.date)
                    put("buySell", trade.buySell)
                    put("quantity", trade.quantity)
                    put("price", trade.price)
                    put("commission", trade.commission)
                    put("totalAmount", trade.totalAmount)
                }
                jsonArray.put(obj)
            }
            jsonArray.toString()
        } catch (_: Exception) {
            trades.joinToString(separator = ";") { trade ->
                "${trade.id}|${trade.symbol}|${trade.date}|${trade.buySell}|${trade.quantity}|${trade.price}|${trade.commission}|${trade.totalAmount}"
            }
        }
    }

    private fun deserializeTrades(jsonString: String): List<Trade> {
        if (jsonString.isBlank()) return emptyList()
        if (jsonString.contains("|")) {
            try {
                return jsonString.split(";").mapNotNull { part ->
                    val fields = part.split("|")
                    if (fields.size >= 8) {
                        Trade(
                            id = fields[0],
                            symbol = fields[1],
                            date = fields[2],
                            buySell = fields[3],
                            quantity = fields[4].toDoubleOrNull() ?: 0.0,
                            price = fields[5].toDoubleOrNull() ?: 0.0,
                            commission = fields[6].toDoubleOrNull() ?: 0.0,
                            totalAmount = fields[7].toDoubleOrNull() ?: 0.0
                        )
                    } else null
                }
            } catch (_: Exception) {}
        }
        val list = mutableListOf<Trade>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Trade(
                        id = obj.optString("id", "manual_$i"),
                        symbol = obj.optString("symbol", ""),
                        date = obj.optString("date", ""),
                        buySell = obj.optString("buySell", "BUY"),
                        quantity = obj.optDouble("quantity", 0.0),
                        price = obj.optDouble("price", 0.0),
                        commission = obj.optDouble("commission", 0.0),
                        totalAmount = obj.optDouble("totalAmount", 0.0)
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e("PortfolioRepository", "Failed to deserialize trades JSON: ${e.message}", e)
        }
        return list
    }

    private fun serializeDeletedTradeIds(ids: Set<String>): String {
        return try {
            val jsonArray = JSONArray()
            ids.forEach { jsonArray.put(it) }
            jsonArray.toString()
        } catch (_: Exception) {
            ids.joinToString(separator = ",")
        }
    }

    private fun deserializeDeletedTradeIds(jsonString: String): Set<String> {
        if (jsonString.isBlank()) return emptySet()
        if (!jsonString.startsWith("[")) {
            return jsonString.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        }
        val set = mutableSetOf<String>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                set.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            AppLogger.e("PortfolioRepository", "Failed to deserialize deleted trades JSON: ${e.message}", e)
        }
        return set
    }
}
