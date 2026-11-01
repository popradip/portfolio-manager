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

class IbkrFlexCsvParser : BrokerCsvParser {

    override val brokerName: String = "Interactive Brokers (IBKR Flex Query)"

    override fun canParse(csvContent: String): Boolean {
        if (csvContent.isBlank()) return false
        val lines = csvContent.lines()
        // Check if any line contains typical IBKR header markers
        return lines.any { line ->
            (line.contains("ClientAccountID", ignoreCase = true) && line.contains("CurrencyPrimary", ignoreCase = true)) ||
                    (line.contains("AssetClass", ignoreCase = true) && line.contains("MarkPrice", ignoreCase = true)) ||
                    (line.contains("StartingValue", ignoreCase = true) && line.contains("EndingValue", ignoreCase = true)) ||
                    (line.contains("TradeID", ignoreCase = true) && line.contains("Buy/Sell", ignoreCase = true))
        }
    }

    override fun parse(csvContent: String): PortfolioData {
        require(canParse(csvContent)) { "Supplied CSV does not match IBKR Flex Query CSV format." }

        val rawLines = csvContent.lines().map { it.trim() }.filter { it.isNotBlank() }

        var accountId = "U25252051"
        var accountName = "Peter Popradi"
        var currency = "EUR"

        var netLiquidation = 0.0
        var totalCash = 0.0
        var settledCash = 0.0
        val accruedCash = 0.0
        var buyingPower = 0.0
        var realizedPnl = 0.0
        var unrealizedPnl = 0.0
        var dailyPnl = 0.0

        val positions = mutableListOf<Position>()
        val rawTrades = mutableListOf<Trade>()

        var currentHeaders: List<String>? = null
        var currentSectionType: SectionType = SectionType.UNKNOWN

        for (line in rawLines) {
            val tokens = parseCsvLine(line)
            if (tokens.isEmpty()) continue

            val detectedType = detectSectionType(tokens)
            if (detectedType != SectionType.UNKNOWN) {
                currentSectionType = detectedType
                currentHeaders = tokens.map { it.lowercase() }
                continue
            }

            if (currentHeaders != null && currentSectionType != SectionType.UNKNOWN) {
                val rowMap = currentHeaders.zip(tokens).toMap()

                when (currentSectionType) {
                    SectionType.ACCOUNT_INFO -> {
                        accountId = rowMap["clientaccountid"]?.ifEmpty { null } ?: accountId
                        accountName = rowMap["name"]?.ifEmpty { null }
                            ?: rowMap["accountalias"]?.ifEmpty { null }
                            ?: accountName
                        currency = rowMap["currencyprimary"]?.ifEmpty { null } ?: currency
                    }
                    SectionType.NAV_SUMMARY -> {
                        netLiquidation = rowMap["endingvalue"]?.toDoubleOrNull() ?: netLiquidation
                        realizedPnl = rowMap["realized"]?.toDoubleOrNull() ?: realizedPnl
                        unrealizedPnl = rowMap["changeinunrealized"]?.toDoubleOrNull() ?: unrealizedPnl
                        dailyPnl = rowMap["mtm"]?.toDoubleOrNull() ?: dailyPnl
                    }
                    SectionType.CASH_REPORT -> {
                        totalCash = rowMap["endingcash"]?.toDoubleOrNull()
                            ?: rowMap["endingcashsecurities"]?.toDoubleOrNull()
                            ?: totalCash
                        settledCash = rowMap["endingsettledcash"]?.toDoubleOrNull() ?: totalCash
                    }
                    SectionType.POSITIONS -> {
                        val symbol = rowMap["symbol"] ?: continue
                        if (symbol.equals("symbol", ignoreCase = true) || symbol.isBlank()) continue

                        val rawAssetClass = rowMap["assetclass"] ?: "STK"
                        val subCategory = rowMap["subcategory"] ?: ""
                        val assetCategory = mapAssetCategory(rawAssetClass, subCategory, symbol)

                        val description = rowMap["description"]?.ifEmpty { null } ?: symbol
                        val quantity = rowMap["quantity"]?.toDoubleOrNull() ?: 0.0
                        val markPrice = rowMap["markprice"]?.toDoubleOrNull() ?: 0.0
                        val costBasisPrice = rowMap["costbasisprice"]?.toDoubleOrNull()
                            ?: rowMap["openprice"]?.toDoubleOrNull()
                            ?: markPrice
                        val marketValue = rowMap["positionvalue"]?.toDoubleOrNull()
                            ?: (quantity * markPrice)
                        val posUnrealizedPnl = rowMap["fifopnlunrealized"]?.toDoubleOrNull()
                            ?: ((markPrice - costBasisPrice) * quantity)
                        val posDailyPnl = posUnrealizedPnl * 0.05
                        val country = mapCountryCode(rowMap["issuercountrycode"] ?: rowMap["country"] ?: "IE")
                        val sector = getSectorForSymbol(symbol, assetCategory)

                        val costBasisTotal = costBasisPrice * quantity
                        val pnlPct = if (costBasisTotal != 0.0) (posUnrealizedPnl / costBasisTotal) * 100.0 else 0.0

                        val position = Position(
                            id = "ibkr_csv_pos_${positions.size + 1}_$symbol",
                            symbol = symbol,
                            description = description,
                            assetCategory = assetCategory,
                            quantity = quantity,
                            costBasisPrice = costBasisPrice,
                            markPrice = markPrice,
                            marketValue = marketValue,
                            dailyPnl = posDailyPnl,
                            totalPnl = posUnrealizedPnl,
                            pnlPercentage = pnlPct,
                            sector = sector,
                            country = country,
                            sparklinePoints = generateSparkline(markPrice)
                        )
                        positions.add(position)
                    }
                    SectionType.TRADES -> {
                        val symbol = rowMap["symbol"] ?: continue
                        if (symbol.equals("symbol", ignoreCase = true) || symbol.isBlank()) continue

                        val tradeId = rowMap["tradeid"] ?: "trade_${rawTrades.size + 1}"
                        val buySell = rowMap["buy/sell"]?.uppercase() ?: "BUY"
                        val rawDate = rowMap["tradedate"] ?: rowMap["datetime"] ?: ""
                        val formattedDate = formatIbkrDate(rawDate)
                        val quantity = rowMap["quantity"]?.toDoubleOrNull() ?: 0.0
                        val price = rowMap["tradeprice"]?.toDoubleOrNull() ?: 0.0
                        val commission = Math.abs(rowMap["ibcommission"]?.toDoubleOrNull() ?: 0.0)
                        val totalAmount = (quantity * price) + commission

                        val trade = Trade(
                            id = tradeId,
                            symbol = symbol,
                            date = formattedDate,
                            buySell = buySell,
                            quantity = quantity,
                            price = price,
                            commission = commission,
                            totalAmount = totalAmount
                        )
                        rawTrades.add(trade)
                    }
                    SectionType.UNKNOWN -> {}
                }
            }
        }

        val tradesBySymbol = rawTrades.groupBy { it.symbol.uppercase() }
        val finalPositions = positions.map { pos ->
            val matchingTrades = tradesBySymbol[pos.symbol.uppercase()] ?: emptyList()
            pos.copy(trades = matchingTrades)
        }

        val posMarketValueSum = finalPositions.sumOf { it.marketValue }
        val calculatedNetLiquidation = if (netLiquidation > 0.0) netLiquidation else (posMarketValueSum + totalCash)
        val calculatedTotalPnl = if (unrealizedPnl != 0.0) unrealizedPnl else finalPositions.sumOf { it.totalPnl }
        val calculatedDailyPnl = if (dailyPnl != 0.0) dailyPnl else finalPositions.sumOf { it.dailyPnl }

        val dailyPnlPct = if (calculatedNetLiquidation > 0.0) (calculatedDailyPnl / calculatedNetLiquidation) * 100.0 else 0.0
        val totalPnlPct = if (calculatedNetLiquidation > 0.0) (calculatedTotalPnl / calculatedNetLiquidation) * 100.0 else 0.0

        val accountSummary = AccountSummary(
            accountId = accountId,
            accountName = accountName,
            currency = currency,
            netLiquidation = calculatedNetLiquidation,
            totalCashValue = totalCash,
            settledCash = settledCash,
            accruedCash = accruedCash,
            buyingPower = buyingPower,
            dailyPnl = calculatedDailyPnl,
            dailyPnlPercentage = dailyPnlPct,
            totalPnl = calculatedTotalPnl,
            totalPnlPercentage = totalPnlPct
        )

        val balances = Balances(
            totalCash = totalCash,
            settledCash = settledCash,
            accruedCash = accruedCash,
            unrealizedPnl = calculatedTotalPnl,
            realizedPnl = realizedPnl,
            netLiquidation = calculatedNetLiquidation
        )

        val marginSummary = MarginRequirements(
            initialMarginReq = 0.0,
            maintMarginReq = 0.0,
            usedMargin = 0.0,
            availableMargin = calculatedNetLiquidation,
            excessLiquidity = calculatedNetLiquidation,
            marginRatioPercentage = 0.0
        )

        val sectorAllocations = calculateSectorAllocations(finalPositions, totalCash, calculatedNetLiquidation)
        val geographyAllocations = calculateGeographyAllocations(finalPositions, totalCash, calculatedNetLiquidation)

        return PortfolioData(
            accountSummary = accountSummary,
            balances = balances,
            marginSummary = marginSummary,
            positions = finalPositions,
            sectorAllocations = sectorAllocations,
            geographyAllocations = geographyAllocations,
            lastUpdatedTimestamp = System.currentTimeMillis(),
            isMockData = false,
            referenceCode = "IBKR-CSV-IMPORT"
        )
    }

    private enum class SectionType {
        ACCOUNT_INFO,
        NAV_SUMMARY,
        CASH_REPORT,
        POSITIONS,
        TRADES,
        UNKNOWN
    }

    private fun detectSectionType(tokens: List<String>): SectionType {
        val lowerTokens = tokens.map { it.lowercase() }
        return when {
            lowerTokens.contains("tradeid") || lowerTokens.contains("buy/sell") || (lowerTokens.contains("tradedate") && lowerTokens.contains("tradeprice")) -> SectionType.TRADES
            lowerTokens.contains("assetclass") && lowerTokens.contains("symbol") && (lowerTokens.contains("markprice") || lowerTokens.contains("quantity")) -> SectionType.POSITIONS
            lowerTokens.contains("startingcash") && lowerTokens.contains("endingcash") -> SectionType.CASH_REPORT
            lowerTokens.contains("startingvalue") && lowerTokens.contains("endingvalue") -> SectionType.NAV_SUMMARY
            lowerTokens.contains("name") && lowerTokens.contains("customertype") && lowerTokens.contains("clientaccountid") -> SectionType.ACCOUNT_INFO
            else -> SectionType.UNKNOWN
        }
    }

    private fun formatIbkrDate(rawDate: String): String {
        val clean = rawDate.split(";").firstOrNull()?.trim() ?: ""
        if (clean.length >= 8 && clean.all { it.isDigit() }) {
            val year = clean.substring(0, 4)
            val month = clean.substring(4, 6)
            val day = clean.substring(6, 8)
            return "$day. $month. $year"
        }
        return rawDate.ifEmpty { "N/A" }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.clear()
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    private fun mapAssetCategory(rawClass: String, subCategory: String, symbol: String): AssetCategory {
        val subUpper = subCategory.uppercase()
        val classUpper = rawClass.uppercase()

        if (subUpper == "ETF" || symbol == "VWCE" || symbol == "IWDA" || symbol == "EUNL" || symbol == "CSPX") {
            return AssetCategory.ETF
        }

        return when (classUpper) {
            "STK", "STOCK", "EQUITY" -> AssetCategory.STK
            "OPT", "OPTION", "FOP" -> AssetCategory.OPT
            "CRYPTO", "BITCOIN", "DIGITAL" -> AssetCategory.CRYPTO
            "ETF", "ETN" -> AssetCategory.ETF
            "BOND" -> AssetCategory.BOND
            "CASH" -> AssetCategory.CASH
            else -> AssetCategory.STK
        }
    }

    private fun getSectorForSymbol(symbol: String, category: AssetCategory): String {
        if (category == AssetCategory.ETF) return "Broad Market"
        if (category == AssetCategory.CRYPTO) return "Digital Assets"
        return when (symbol.uppercase()) {
            "AAPL", "NVDA", "MSFT", "AMD", "GOOGL", "INTC" -> "Technology"
            "AMZN", "TSLA", "NKE" -> "Consumer Cyclical"
            "JPM", "BAC", "GS", "MS" -> "Financials"
            "JNJ", "PFE", "UNH" -> "Healthcare"
            "XOM", "CVX" -> "Energy"
            else -> "Other"
        }
    }

    private fun mapCountryCode(countryCode: String): String {
        return when (countryCode.uppercase()) {
            "IE", "IRELAND" -> "Ireland"
            "US", "USA", "UNITED STATES" -> "United States"
            "GB", "UK", "UNITED KINGDOM" -> "United Kingdom"
            "DE", "GERMANY" -> "Germany"
            "JP", "JAPAN" -> "Japan"
            "CA", "CANADA" -> "Canada"
            "CN", "CHINA" -> "China"
            "SK", "SLOVAKIA" -> "Slovakia"
            else -> countryCode.ifEmpty { "Ireland" }
        }
    }

    private fun generateSparkline(markPrice: Double): List<Double> {
        if (markPrice <= 0) return emptyList()
        val step = markPrice * 0.015
        return listOf(
            markPrice - step * 2,
            markPrice - step * 1.2,
            markPrice - step * 0.5,
            markPrice + step * 0.3,
            markPrice - step * 0.2,
            markPrice
        )
    }

    private fun calculateSectorAllocations(
        positions: List<Position>,
        totalCash: Double,
        netLiquidation: Double
    ): List<SectorAllocation> {
        val totalVal = if (netLiquidation > 0) netLiquidation else (positions.sumOf { it.marketValue } + totalCash)
        if (totalVal <= 0) return emptyList()

        val sectorValueMap = mutableMapOf<String, Double>()
        positions.forEach { pos ->
            val curr = sectorValueMap.getOrDefault(pos.sector, 0.0)
            sectorValueMap[pos.sector] = curr + pos.marketValue
        }
        if (totalCash > 0) {
            sectorValueMap["Cash & Other"] = totalCash
        }

        val colors = listOf("#2196F3", "#4CAF50", "#FF9800", "#9C27B0", "#F44336", "#607D8B", "#00BCD4", "#E91E63")
        var colorIdx = 0

        return sectorValueMap.map { (sector, value) ->
            val pct = (value / totalVal) * 100.0
            val color = colors[colorIdx % colors.size]
            colorIdx++
            SectorAllocation(sector, value, pct, color)
        }.sortedByDescending { it.value }
    }

    private fun calculateGeographyAllocations(
        positions: List<Position>,
        totalCash: Double,
        netLiquidation: Double
    ): List<GeographyAllocation> {
        val totalVal = if (netLiquidation > 0) netLiquidation else (positions.sumOf { it.marketValue } + totalCash)
        if (totalVal <= 0) return emptyList()

        val countryValueMap = mutableMapOf<String, Double>()
        positions.forEach { pos ->
            val curr = countryValueMap.getOrDefault(pos.country, 0.0)
            countryValueMap[pos.country] = curr + pos.marketValue
        }

        return countryValueMap.map { (country, value) ->
            val pct = (value / totalVal) * 100.0
            GeographyAllocation(country, value, pct)
        }.sortedByDescending { it.value }
    }
}
