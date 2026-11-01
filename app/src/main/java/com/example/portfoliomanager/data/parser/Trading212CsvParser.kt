package com.example.portfoliomanager.data.parser

import com.example.portfoliomanager.data.model.AccountSummary
import com.example.portfoliomanager.data.model.AssetCategory
import com.example.portfoliomanager.data.model.Balances
import com.example.portfoliomanager.data.model.GeographyAllocation
import com.example.portfoliomanager.data.model.MarginRequirements
import com.example.portfoliomanager.data.model.PortfolioData
import com.example.portfoliomanager.data.model.Position
import com.example.portfoliomanager.data.model.SectorAllocation
import com.example.portfoliomanager.data.model.Trade

/**
 * Parser for Trading 212 CSV exports.
 */
class Trading212CsvParser : BrokerCsvParser {

    override val brokerName: String = "Trading 212"

    override fun canParse(csvContent: String): Boolean {
        if (csvContent.isBlank()) return false
        val firstLines = csvContent.lines().take(10).joinToString("\n").lowercase()
        return firstLines.contains("trading 212") ||
                (firstLines.contains("action") && (firstLines.contains("isin") || firstLines.contains("ticker")) &&
                        (firstLines.contains("no. of shares") || firstLines.contains("price / share")))
    }

    private data class HoldingAccumulator(
        val ticker: String,
        val isin: String,
        val name: String,
        var priceCurrency: String,
        var accumulatedShares: Double = 0.0,
        var accumulatedCostEur: Double = 0.0,
        var accumulatedCostNative: Double = 0.0,
        var latestPricePerShare: Double = 0.0,
        var latestFxRate: Double = 1.0,
        val trades: MutableList<Trade> = mutableListOf()
    )

    override fun parse(csvContent: String): PortfolioData {
        require(canParse(csvContent)) { "Supplied CSV does not match Trading 212 CSV format." }

        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return createEmptyPortfolioData()
        }

        // 1. Identify header line and column indices
        var headerIdx = -1
        var actionIdx = -1
        var timeIdx = -1
        var isinIdx = -1
        var tickerIdx = -1
        var nameIdx = -1
        var idIdx = -1
        var sharesIdx = -1
        var priceIdx = -1
        var priceCurrIdx = -1
        var fxIdx = -1
        var totalIdx = -1
        var feeIdx = -1

        for ((idx, line) in lines.withIndex()) {
            val tokens = parseCsvLine(line)
            val lowerTokens = tokens.map { it.lowercase().trim() }
            if (lowerTokens.contains("action") && (lowerTokens.contains("isin") || lowerTokens.contains("ticker"))) {
                headerIdx = idx
                for ((colIdx, headerToken) in lowerTokens.withIndex()) {
                    when (headerToken) {
                        "action" -> actionIdx = colIdx
                        "isin" -> isinIdx = colIdx
                        "ticker" -> tickerIdx = colIdx
                        "name" -> nameIdx = colIdx
                        "id" -> idIdx = colIdx
                        "no. of shares", "shares" -> sharesIdx = colIdx
                        "price / share" -> priceIdx = colIdx
                        "currency (price / share)" -> priceCurrIdx = colIdx
                        "exchange rate" -> fxIdx = colIdx
                        "total" -> totalIdx = colIdx
                        "currency conversion fee" -> feeIdx = colIdx
                        else -> {
                            if (headerToken.startsWith("time")) timeIdx = colIdx
                            else if (priceIdx == -1 && headerToken.contains("price") && !headerToken.contains("currency")) priceIdx = colIdx
                            else if (sharesIdx == -1 && headerToken.contains("shares")) sharesIdx = colIdx
                        }
                    }
                }
                break
            }
        }

        if (headerIdx == -1 || actionIdx == -1) {
            return createEmptyPortfolioData()
        }

        // 2. Accumulate transactions
        val holdingsMap = mutableMapOf<String, HoldingAccumulator>()
        val seenTransactionIds = mutableSetOf<String>()
        var totalSpentOnBuysEur = 0.0
        var totalReceivedFromSellsEur = 0.0
        var totalDepositsEur = 0.0
        var totalWithdrawalsEur = 0.0
        var totalDividendsEur = 0.0

        for (i in (headerIdx + 1) until lines.size) {
            val line = lines[i]
            val tokens = parseCsvLine(line)
            val lowerTokens = tokens.map { it.lowercase().trim() }

            // Re-evaluate header if another CSV file header line is encountered (e.g. concatenated CSVs)
            if (lowerTokens.contains("action") && (lowerTokens.contains("isin") || lowerTokens.contains("ticker"))) {
                for ((colIdx, headerToken) in lowerTokens.withIndex()) {
                    when (headerToken) {
                        "action" -> actionIdx = colIdx
                        "isin" -> isinIdx = colIdx
                        "ticker" -> tickerIdx = colIdx
                        "name" -> nameIdx = colIdx
                        "id" -> idIdx = colIdx
                        "no. of shares", "shares" -> sharesIdx = colIdx
                        "price / share" -> priceIdx = colIdx
                        "currency (price / share)" -> priceCurrIdx = colIdx
                        "exchange rate" -> fxIdx = colIdx
                        "total" -> totalIdx = colIdx
                        "currency conversion fee" -> feeIdx = colIdx
                        else -> {
                            if (headerToken.startsWith("time")) timeIdx = colIdx
                            else if (priceIdx == -1 && headerToken.contains("price") && !headerToken.contains("currency")) priceIdx = colIdx
                            else if (sharesIdx == -1 && headerToken.contains("shares")) sharesIdx = colIdx
                        }
                    }
                }
                continue
            }

            if (tokens.size <= actionIdx) continue

            val action = tokens.getOrNull(actionIdx)?.trim() ?: ""
            if (action.isBlank() || action.equals("action", ignoreCase = true)) continue

            val timeStr = tokens.getOrNull(timeIdx)?.trim() ?: ""
            val isin = tokens.getOrNull(isinIdx)?.trim() ?: ""
            var ticker = tokens.getOrNull(tickerIdx)?.trim() ?: ""
            if (ticker.isBlank()) ticker = isin
            val name = tokens.getOrNull(nameIdx)?.trim()?.removeSurrounding("\"")?.ifEmpty { ticker } ?: ticker
            val rawId = tokens.getOrNull(idIdx)?.trim() ?: ""
            val shares = parseDoubleSafe(tokens.getOrNull(sharesIdx))
            val pricePerShare = parseDoubleSafe(tokens.getOrNull(priceIdx))
            val priceCurrency = tokens.getOrNull(priceCurrIdx)?.trim()?.ifEmpty { "EUR" } ?: "EUR"
            val exchangeRate = parseDoubleSafe(tokens.getOrNull(fxIdx)).let { if (it > 0) it else 1.0 }
            val totalCost = parseDoubleSafe(tokens.getOrNull(totalIdx))
            val fee = parseDoubleSafe(tokens.getOrNull(feeIdx))

            val id = rawId.ifBlank { "$timeStr|$ticker|$action|$shares|$totalCost" }

            // Skip duplicate transactions
            if (!seenTransactionIds.add(id)) {
                continue
            }

            val actionLower = action.lowercase()

            when {
                actionLower.contains("scrip stock") || actionLower.contains("stock distribution") -> {
                    if (ticker.isBlank()) continue
                    val holding = holdingsMap.getOrPut(ticker) {
                        HoldingAccumulator(ticker, isin, name, priceCurrency)
                    }
                    if (priceCurrency.isNotBlank()) holding.priceCurrency = priceCurrency
                    holding.accumulatedShares += shares
                    holding.trades.add(Trade(id, ticker, timeStr, "DIVIDEND_SHARE", shares, 0.0, 0.0, 0.0))
                }
                actionLower.contains("buy") -> {
                    if (ticker.isBlank()) continue
                    val buyCostEur = if (totalCost > 0.0) totalCost else (shares * pricePerShare / exchangeRate)
                    val buyCostNative = shares * pricePerShare

                    val holding = holdingsMap.getOrPut(ticker) {
                        HoldingAccumulator(ticker, isin, name, priceCurrency)
                    }
                    if (priceCurrency.isNotBlank()) holding.priceCurrency = priceCurrency
                    holding.accumulatedShares += shares
                    holding.accumulatedCostEur += buyCostEur
                    holding.accumulatedCostNative += buyCostNative
                    if (pricePerShare > 0.0) {
                        holding.latestPricePerShare = pricePerShare
                        holding.latestFxRate = exchangeRate
                    }
                    totalSpentOnBuysEur += buyCostEur
                    holding.trades.add(Trade(id, ticker, timeStr, "BUY", shares, pricePerShare, fee, buyCostEur))
                }
                actionLower.contains("sell") -> {
                    if (ticker.isBlank()) continue
                    val sellReceivedEur = if (totalCost > 0.0) totalCost else (shares * pricePerShare / exchangeRate)

                    val holding = holdingsMap.getOrPut(ticker) {
                        HoldingAccumulator(ticker, isin, name, priceCurrency)
                    }
                    if (priceCurrency.isNotBlank()) holding.priceCurrency = priceCurrency
                    if (holding.accumulatedShares > 0.0) {
                        val ratio = (shares / holding.accumulatedShares).coerceAtMost(1.0)
                        holding.accumulatedCostEur -= (holding.accumulatedCostEur * ratio)
                        holding.accumulatedCostNative -= (holding.accumulatedCostNative * ratio)
                        holding.accumulatedShares = maxOf(0.0, holding.accumulatedShares - shares)
                    }
                    if (pricePerShare > 0.0) {
                        holding.latestPricePerShare = pricePerShare
                        holding.latestFxRate = exchangeRate
                    }
                    totalReceivedFromSellsEur += sellReceivedEur
                    holding.trades.add(Trade(id, ticker, timeStr, "SELL", shares, pricePerShare, fee, sellReceivedEur))
                }
                actionLower.contains("deposit") -> {
                    totalDepositsEur += totalCost
                }
                actionLower.contains("withdrawal") -> {
                    totalWithdrawalsEur += totalCost
                }
                actionLower.contains("dividend") -> {
                    totalDividendsEur += totalCost
                }
            }
        }

        // 3. Construct Positions for active holdings (> 0.00000001 shares)
        val positions = mutableListOf<Position>()
        for ((_, holding) in holdingsMap) {
            if (holding.accumulatedShares <= 0.00000001) continue

            val avgCostPriceNative = holding.accumulatedCostNative / holding.accumulatedShares
            val avgCostPriceEur = holding.accumulatedCostEur / holding.accumulatedShares

            val markPrice = if (holding.latestPricePerShare > 0.0) holding.latestPricePerShare else avgCostPriceNative
            val marketValueEur = if (holding.priceCurrency.equals("EUR", ignoreCase = true)) {
                holding.accumulatedShares * markPrice
            } else {
                if (holding.latestFxRate > 0.0) {
                    (holding.accumulatedShares * markPrice) / holding.latestFxRate
                } else {
                    holding.accumulatedCostEur
                }
            }

            val totalPnlEur = marketValueEur - holding.accumulatedCostEur
            val pnlPct = if (holding.accumulatedCostEur > 0.0) (totalPnlEur / holding.accumulatedCostEur) * 100.0 else 0.0

            val sector = mapSector(holding.name)
            val country = mapIsinToCountry(holding.isin)
            val category = if (sector == "Broad ETF") AssetCategory.ETF else AssetCategory.STK

            positions.add(
                Position(
                    id = "t212_pos_${holding.ticker.uppercase()}",
                    symbol = holding.ticker,
                    description = holding.name,
                    assetCategory = category,
                    quantity = holding.accumulatedShares,
                    costBasisPrice = if (holding.priceCurrency.equals("EUR", ignoreCase = true)) avgCostPriceEur else avgCostPriceNative,
                    markPrice = markPrice,
                    marketValue = marketValueEur,
                    dailyPnl = 0.0,
                    totalPnl = totalPnlEur,
                    pnlPercentage = pnlPct,
                    sector = sector,
                    country = country,
                    trades = holding.trades
                )
            )
        }

        // Sort positions by market value descending
        positions.sortByDescending { it.marketValue }

        val calculatedCash = if (totalDepositsEur > 0.0) {
            (totalDepositsEur - totalWithdrawalsEur - totalSpentOnBuysEur + totalReceivedFromSellsEur + totalDividendsEur).coerceAtLeast(0.0)
        } else {
            0.0
        }

        val totalMarketValueEur = positions.sumOf { it.marketValue }
        val netLiquidation = totalMarketValueEur + calculatedCash
        val totalUnrealizedPnl = positions.sumOf { it.totalPnl }
        val totalCostBasis = positions.sumOf { it.marketValue - it.totalPnl }
        val totalPnlPct = if (totalCostBasis > 0.0) (totalUnrealizedPnl / totalCostBasis) * 100.0 else 0.0

        val sectorAllocations = calculateSectorAllocations(positions)
        val geographyAllocations = calculateGeographyAllocations(positions)

        return PortfolioData(
            accountSummary = AccountSummary(
                accountId = "T212-Account",
                accountName = "Trading 212 Account",
                currency = "EUR",
                netLiquidation = netLiquidation,
                totalCashValue = calculatedCash,
                settledCash = calculatedCash,
                accruedCash = 0.0,
                buyingPower = calculatedCash,
                dailyPnl = 0.0,
                dailyPnlPercentage = 0.0,
                totalPnl = totalUnrealizedPnl,
                totalPnlPercentage = totalPnlPct
            ),
            balances = Balances(
                totalCash = calculatedCash,
                settledCash = calculatedCash,
                accruedCash = 0.0,
                unrealizedPnl = totalUnrealizedPnl,
                realizedPnl = 0.0,
                netLiquidation = netLiquidation
            ),
            marginSummary = MarginRequirements(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            positions = positions,
            sectorAllocations = sectorAllocations,
            geographyAllocations = geographyAllocations,
            isMockData = false,
            referenceCode = "T212-CSV-IMPORT"
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    private fun parseDoubleSafe(valueStr: String?): Double {
        if (valueStr.isNullOrBlank()) return 0.0
        val cleaned = valueStr.trim().replace(",", "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun mapIsinToCountry(isin: String): String {
        if (isin.length < 2) return "United States"
        return when (isin.take(2).uppercase()) {
            "US" -> "United States"
            "CA" -> "Canada"
            "IE" -> "Ireland"
            "CH" -> "Switzerland"
            "DE" -> "Germany"
            "GB" -> "United Kingdom"
            "FR" -> "France"
            "NL" -> "Netherlands"
            "AT" -> "Austria"
            "IT" -> "Italy"
            "ES" -> "Spain"
            "JP" -> "Japan"
            "CN" -> "China"
            else -> "United States"
        }
    }

    private fun mapSector(name: String): String {
        val lowerName = name.lowercase()
        return when {
            lowerName.contains("etf") || lowerName.contains("ftse") || lowerName.contains("s&p") || lowerName.contains("ishares") || lowerName.contains("vanguard") -> "Broad ETF"
            lowerName.contains("bank") || lowerName.contains("chase") || lowerName.contains("blackrock") || lowerName.contains("citigroup") || lowerName.contains("mastercard") -> "Financial Services"
            lowerName.contains("tech") || lowerName.contains("microsoft") || lowerName.contains("apple") || lowerName.contains("cisco") || lowerName.contains("ibm") || lowerName.contains("roper") || lowerName.contains("automatic data") -> "Technology"
            lowerName.contains("health") || lowerName.contains("johnson") || lowerName.contains("cardinal") || lowerName.contains("bristol") -> "Healthcare"
            lowerName.contains("coca") || lowerName.contains("pepsi") || lowerName.contains("procter") || lowerName.contains("walmart") || lowerName.contains("starbucks") || lowerName.contains("mcdonald") || lowerName.contains("kimberly") || lowerName.contains("sysco") -> "Consumer Staples"
            lowerName.contains("chevron") || lowerName.contains("canadian natural") || lowerName.contains("slb") -> "Energy"
            lowerName.contains("realty") || lowerName.contains("ltc") || lowerName.contains("stag") || lowerName.contains("agree") || lowerName.contains("gladstone") -> "Real Estate"
            lowerName.contains("utility") || lowerName.contains("duke") -> "Utilities"
            lowerName.contains("nucor") || lowerName.contains("sherwin") || lowerName.contains("ecolab") || lowerName.contains("ppg") || lowerName.contains("royal gold") || lowerName.contains("illinois tool") || lowerName.contains("general dynamics") || lowerName.contains("pentair") || lowerName.contains("grainger") || lowerName.contains("lowe") -> "Industrials & Materials"
            else -> "Other"
        }
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
            "Financial Services" -> "#2196F3"
            "Consumer Staples" -> "#FF9800"
            "Healthcare" -> "#E91E63"
            "Energy" -> "#FF5722"
            "Real Estate" -> "#9C27B0"
            "Utilities" -> "#00BCD4"
            "Industrials & Materials" -> "#795548"
            "Broad ETF" -> "#009688"
            else -> "#607D8B"
        }
    }

    private fun createEmptyPortfolioData(): PortfolioData {
        return PortfolioData(
            accountSummary = AccountSummary(
                accountId = "T212-Account",
                accountName = "Trading 212 Account",
                currency = "EUR",
                netLiquidation = 0.0,
                totalCashValue = 0.0,
                settledCash = 0.0,
                accruedCash = 0.0,
                buyingPower = 0.0,
                dailyPnl = 0.0,
                dailyPnlPercentage = 0.0,
                totalPnl = 0.0,
                totalPnlPercentage = 0.0
            ),
            balances = Balances(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            marginSummary = MarginRequirements(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            positions = emptyList(),
            sectorAllocations = emptyList(),
            geographyAllocations = emptyList(),
            isMockData = false,
            referenceCode = "T212-CSV-IMPORT"
        )
    }
}
