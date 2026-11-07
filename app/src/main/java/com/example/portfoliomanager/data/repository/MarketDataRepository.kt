package com.example.portfoliomanager.data.repository

import com.example.portfoliomanager.data.model.GeographyAllocation
import com.example.portfoliomanager.data.model.PortfolioData
import com.example.portfoliomanager.data.model.PortfolioHistoryData
import com.example.portfoliomanager.data.model.PortfolioHistoryPoint
import com.example.portfoliomanager.data.model.PortfolioTimeRange
import com.example.portfoliomanager.data.model.Position
import com.example.portfoliomanager.data.model.SectorAllocation
import com.example.portfoliomanager.data.remote.AppLogger
import com.example.portfoliomanager.data.remote.YahooFinanceApiClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

interface MarketDataRepository {
    /**
     * Enriches the provided [PortfolioData] with live Yahoo Finance market prices, FX exchange rates, sparklines, and updated P&L metrics.
     */
    suspend fun enrichPortfolioWithLivePrices(portfolioData: PortfolioData): PortfolioData

    /**
     * Calculates real historical portfolio values over time for the specified [timeRange].
     */
    suspend fun fetchHistoricalPortfolioData(portfolioData: PortfolioData, timeRange: PortfolioTimeRange): PortfolioHistoryData
}

class MarketDataRepositoryImpl(
    private val yahooApiClient: YahooFinanceApiClient = YahooFinanceApiClient()
) : MarketDataRepository {

    override suspend fun enrichPortfolioWithLivePrices(portfolioData: PortfolioData): PortfolioData = coroutineScope {
        if (portfolioData.positions.isEmpty()) return@coroutineScope portfolioData

        try {
            val targetCurrency = portfolioData.accountSummary.currency.ifBlank { "EUR" }.uppercase()

            // 1. Fetch live prices for all distinct effective tickers in parallel
            val distinctTickers = portfolioData.positions.map { it.effectiveTicker }.distinct()
            AppLogger.d("MarketDataRepository", "Enriching ${portfolioData.positions.size} positions (${distinctTickers.joinToString()}) with Yahoo Finance live prices...")

            val marketDataDeferreds = distinctTickers.map { ticker ->
                async { ticker to yahooApiClient.fetchMarketData(ticker) }
            }

            val marketDataMap = marketDataDeferreds.awaitAll()
                .mapNotNull { (ticker, data) -> if (data != null) ticker to data else null }
                .toMap()

            AppLogger.d("MarketDataRepository", "Received live market data for ${marketDataMap.size} of ${distinctTickers.size} tickers.")

            // 2. Resolve currencies for positions (preserve inferred or explicitly set currencies)
            val updatedPositionCurrencies = portfolioData.positions.map { pos ->
                val marketData = marketDataMap[pos.effectiveTicker]
                val inferredCurrency = inferCurrencyFromSymbol(pos.symbol) ?: inferCurrencyFromSymbol(pos.effectiveTicker)
                val detectedCurrency: String = if (inferredCurrency != null) {
                    inferredCurrency
                } else if (!pos.currency.isBlank() && !pos.currency.equals("USD", ignoreCase = true)) {
                    pos.currency
                } else if (marketData != null && marketData.currency.isNotBlank()) {
                    marketData.currency
                } else {
                    pos.currency.ifBlank { "USD" }
                }
                pos.copy(currency = detectedCurrency.uppercase())
            }

            // 3. Identify all distinct position & market data currencies and fetch FX exchange rates to target currency (e.g. EUR)
            val positionCurrencies = updatedPositionCurrencies.map { it.currency }
            val marketDataCurrencies = updatedPositionCurrencies.mapNotNull { pos -> marketDataMap[pos.effectiveTicker]?.currency }
            val distinctCurrencies = (positionCurrencies + marketDataCurrencies + targetCurrency).distinct()

            val fxRatesDeferreds = distinctCurrencies.map { curr ->
                async {
                    val rate = if (curr == targetCurrency) 1.0 else {
                        yahooApiClient.fetchExchangeRate(curr, targetCurrency) ?: getFallbackFxRate(curr, targetCurrency)
                    }
                    curr to rate
                }
            }

            val fxRatesMap = fxRatesDeferreds.awaitAll().toMap()
            AppLogger.d("MarketDataRepository", "FX Rates to $targetCurrency: $fxRatesMap")

            val historicalFxDeferreds = distinctCurrencies.filter { it != targetCurrency }.map { curr ->
                async {
                    curr to yahooApiClient.fetchHistoricalExchangeRates(curr, targetCurrency)
                }
            }
            val historicalFxMap = historicalFxDeferreds.awaitAll().toMap()

            // 4. Update each position with live prices and converted market value in target currency (EUR)
            val updatedPositions = updatedPositionCurrencies.map { pos ->
                val marketData = marketDataMap[pos.effectiveTicker]
                val currentFxRate = fxRatesMap[pos.currency] ?: 1.0
                val posHistFxRates = historicalFxMap[pos.currency] ?: emptyMap()

                if (marketData != null) {
                    val marketDataCurrency = marketData.currency.ifBlank { pos.currency }.uppercase()
                    val priceFxToPosCurrency = if (marketDataCurrency == pos.currency) {
                        1.0
                    } else {
                        val rateMarketToTarget = fxRatesMap[marketDataCurrency] ?: 1.0
                        if (currentFxRate > 0.0) rateMarketToTarget / currentFxRate else 1.0
                    }

                    val liveMarkPriceNative = marketData.currentPrice * priceFxToPosCurrency
                    val updatedMarketValueInTarget = pos.quantity * liveMarkPriceNative * currentFxRate

                    // Calculate historical cost basis in target portfolio currency using historical FX rates at purchase dates
                    val buyTrades = pos.trades.filter { it.buySell.uppercase() == "BUY" && it.quantity > 0 }
                    val updatedCostBasisTotalInTarget = if (buyTrades.isNotEmpty() && pos.currency != targetCurrency && posHistFxRates.isNotEmpty()) {
                        val totalBuyQty = buyTrades.sumOf { it.quantity }
                        val totalBuyCostInTarget = buyTrades.fold(0.0) { acc, trade ->
                            val tradeFx = getHistoricalFxRate(posHistFxRates, trade.date, currentFxRate)
                            acc + (trade.quantity * trade.price * priceFxToPosCurrency * tradeFx)
                        }
                        if (totalBuyQty > 0.0) {
                            (totalBuyCostInTarget / totalBuyQty) * pos.quantity
                        } else {
                            pos.quantity * pos.costBasisPrice * currentFxRate
                        }
                    } else {
                        pos.quantity * pos.costBasisPrice * currentFxRate
                    }

                    val updatedTotalPnlInTarget = updatedMarketValueInTarget - updatedCostBasisTotalInTarget
                    val updatedPnlPercentage = if (updatedCostBasisTotalInTarget > 0.0) {
                        (updatedTotalPnlInTarget / updatedCostBasisTotalInTarget) * 100.0
                    } else pos.pnlPercentage

                    val nowCal = java.util.Calendar.getInstance()
                    val dayOfWeek = nowCal.get(java.util.Calendar.DAY_OF_WEEK)
                    val isWeekend = (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY)

                    val updatedDailyPnlInTarget = if (isWeekend) 0.0 else (marketData.dailyChange * priceFxToPosCurrency * pos.quantity * currentFxRate)

                    pos.copy(
                        markPrice = liveMarkPriceNative,
                        marketValue = updatedMarketValueInTarget,
                        dailyPnl = updatedDailyPnlInTarget,
                        totalPnl = updatedTotalPnlInTarget,
                        pnlPercentage = updatedPnlPercentage,
                        sparklinePoints = if (marketData.sparklinePoints.isNotEmpty()) marketData.sparklinePoints else pos.sparklinePoints,
                        fxRateToPortfolio = currentFxRate
                    )
                } else {
                    val updatedMarketValueInTarget = pos.marketValue * currentFxRate
                    val updatedTotalPnlInTarget = pos.totalPnl * currentFxRate

                    val nowCal = java.util.Calendar.getInstance()
                    val dayOfWeek = nowCal.get(java.util.Calendar.DAY_OF_WEEK)
                    val isWeekend = (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY)

                    val updatedDailyPnlInTarget = if (isWeekend) 0.0 else (pos.dailyPnl * currentFxRate)

                    pos.copy(
                        marketValue = updatedMarketValueInTarget,
                        totalPnl = updatedTotalPnlInTarget,
                        dailyPnl = updatedDailyPnlInTarget,
                        fxRateToPortfolio = currentFxRate
                    )
                }
            }

            // 5. Recalculate portfolio-wide summary stats based on converted values
            val posMarketValueSum = updatedPositions.sumOf { it.marketValue }
            val totalCash = portfolioData.balances.totalCash
            val updatedNetLiquidation = posMarketValueSum + totalCash

            val updatedUnrealizedPnlSum = updatedPositions.sumOf { it.totalPnl }
            val updatedDailyPnlSum = updatedPositions.sumOf { it.dailyPnl }
            val realizedPnl = portfolioData.balances.realizedPnl
            val combinedTotalPnl = updatedUnrealizedPnlSum + realizedPnl

            val openPositions = updatedPositions.filter { it.quantity > 0.0 }
            val portfolioCostBasis = (updatedNetLiquidation - combinedTotalPnl).takeIf { it > 0.0 }
                ?: openPositions.sumOf { it.quantity * it.costBasisPrice * it.fxRateToPortfolio }.takeIf { it > 0.0 }
                ?: 1.0

            val updatedDailyPnlPct = if (updatedNetLiquidation > 0.0) (updatedDailyPnlSum / updatedNetLiquidation) * 100.0 else 0.0
            val updatedTotalPnlPct = (combinedTotalPnl / portfolioCostBasis) * 100.0

            val updatedAccountSummary = portfolioData.accountSummary.copy(
                currency = targetCurrency,
                netLiquidation = updatedNetLiquidation,
                dailyPnl = updatedDailyPnlSum,
                dailyPnlPercentage = updatedDailyPnlPct,
                totalPnl = combinedTotalPnl,
                totalPnlPercentage = updatedTotalPnlPct
            )

            val updatedBalances = portfolioData.balances.copy(
                unrealizedPnl = updatedUnrealizedPnlSum,
                realizedPnl = realizedPnl,
                netLiquidation = updatedNetLiquidation
            )

            val updatedMargin = portfolioData.marginSummary.copy(
                availableMargin = updatedNetLiquidation,
                excessLiquidity = updatedNetLiquidation
            )

            val updatedSectors = recalculateSectors(updatedPositions, totalCash, updatedNetLiquidation)
            val updatedGeography = recalculateGeography(updatedPositions, totalCash, updatedNetLiquidation)

            portfolioData.copy(
                accountSummary = updatedAccountSummary,
                balances = updatedBalances,
                marginSummary = updatedMargin,
                positions = updatedPositions,
                sectorAllocations = updatedSectors,
                geographyAllocations = updatedGeography,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            AppLogger.e("MarketDataRepository", "Failed to enrich portfolio with Yahoo Finance live prices: ${e.message}", e)
            portfolioData
        }
    }

    private fun getFallbackFxRate(fromCurrency: String, targetCurrency: String): Double {
        if (fromCurrency.equals(targetCurrency, ignoreCase = true)) return 1.0
        return when (fromCurrency.uppercase()) {
            "USD" -> 0.92
            "GBP" -> 1.17
            "CZK" -> 0.040
            "PLN" -> 0.23
            "CHF" -> 1.05
            "CAD" -> 0.68
            "AUD" -> 0.60
            "HUF" -> 0.0025
            "JPY" -> 0.0062
            else -> 1.0
        }
    }

    private fun getHistoricalFxRate(
        ratesMap: Map<String, Double>,
        dateStr: String,
        fallbackRate: Double
    ): Double {
        val cleanDate = dateStr.trim().take(10)
        if (cleanDate.length < 10) return fallbackRate

        ratesMap[cleanDate]?.let { return it }

        val lastDate = if (ratesMap is java.util.NavigableMap) ratesMap.lastKey() else ratesMap.keys.maxOrNull()
        if (lastDate != null && cleanDate >= lastDate && fallbackRate > 0.0) {
            return fallbackRate
        }

        if (ratesMap is java.util.NavigableMap) {
            val floorEntry = ratesMap.floorEntry(cleanDate)
            if (floorEntry != null) return floorEntry.value
        } else {
            val previousDates = ratesMap.keys.filter { it <= cleanDate }
            if (previousDates.isNotEmpty()) {
                val maxDate = previousDates.maxOrNull()
                if (maxDate != null) {
                    ratesMap[maxDate]?.let { return it }
                }
            }
        }
        return fallbackRate
    }

    private fun inferCurrencyFromSymbol(symbol: String): String? {
        val upper = symbol.trim().uppercase()
        return when {
            upper.endsWith(".DE") || upper.endsWith(".PA") || upper.endsWith(".MI") ||
            upper.endsWith(".AS") || upper.endsWith(".FR") || upper.endsWith(".NL") ||
            upper.endsWith(".FI") || upper.endsWith(".HE") -> "EUR"
            upper.endsWith(".UK") || upper.endsWith(".L") -> "GBP"
            upper.endsWith(".PL") || upper.endsWith(".WA") -> "PLN"
            upper.endsWith(".PR") -> "CZK"
            upper.endsWith(".US") -> "USD"
            else -> null
        }
    }

    private fun recalculateSectors(
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

    private fun recalculateGeography(
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

    override suspend fun fetchHistoricalPortfolioData(
        portfolioData: PortfolioData,
        timeRange: PortfolioTimeRange
    ): PortfolioHistoryData = coroutineScope {
        val targetCurrency = portfolioData.accountSummary.currency.ifBlank { "EUR" }.uppercase()
        val positions = portfolioData.positions
        val totalCash = portfolioData.accountSummary.totalCashValue.coerceAtLeast(0.0)

        if (positions.isEmpty()) {
            val currentValue = portfolioData.accountSummary.netLiquidation.takeIf { it > 0 } ?: 0.0
            return@coroutineScope PortfolioHistoryData(
                timeRange = timeRange,
                points = emptyList(),
                startValue = currentValue,
                endValue = currentValue,
                changeAmount = 0.0,
                changePercentage = 0.0
            )
        }

        // 1. Fetch historical prices for all distinct tickers in parallel
        val distinctTickers = positions.map { it.effectiveTicker }.distinct()
        val historicalPriceDeferreds = distinctTickers.map { ticker ->
            async { ticker to yahooApiClient.fetchHistoricalPrices(ticker, "5y") }
        }
        val historicalPricesMap = historicalPriceDeferreds.awaitAll().toMap()

        // 2. Fetch historical FX rates for all distinct position currencies in parallel
        val distinctCurrencies = (positions.map { it.currency } + targetCurrency).distinct()
        val historicalFxDeferreds = distinctCurrencies.filter { it != targetCurrency }.map { curr ->
            async { curr to yahooApiClient.fetchHistoricalExchangeRates(curr, targetCurrency) }
        }
        val historicalFxMap = historicalFxDeferreds.awaitAll().toMap()

        // 3. Determine time points (number of points and day steps)
        val numPoints = when (timeRange) {
            PortfolioTimeRange.ONE_MONTH -> 30
            PortfolioTimeRange.THREE_MONTHS -> 30
            PortfolioTimeRange.SIX_MONTHS -> 30
            PortfolioTimeRange.ONE_YEAR -> 52
            PortfolioTimeRange.YTD -> 40
            PortfolioTimeRange.ALL -> 60
        }

        val dayStep = when (timeRange) {
            PortfolioTimeRange.ONE_MONTH -> 1
            PortfolioTimeRange.THREE_MONTHS -> 3
            PortfolioTimeRange.SIX_MONTHS -> 6
            PortfolioTimeRange.ONE_YEAR -> 7
            PortfolioTimeRange.YTD -> 7
            PortfolioTimeRange.ALL -> 14
        }

        val points = mutableListOf<PortfolioHistoryPoint>()
        val calendar = java.util.Calendar.getInstance()
        val skLocale = java.util.Locale.forLanguageTag("sk")
        val dateFormat = when (timeRange) {
            PortfolioTimeRange.ONE_MONTH, PortfolioTimeRange.THREE_MONTHS -> java.text.SimpleDateFormat("d. MMM", skLocale)
            else -> java.text.SimpleDateFormat("MMM yyyy", skLocale)
        }
        val isoDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        isoDateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

        // Start calendar at (now - numPoints * dayStep)
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -((numPoints - 1) * dayStep))

        for (i in 0 until numPoints) {
            val dateStr = isoDateFormat.format(calendar.time)
            var dayPositionsMarketValue = 0.0

            for (pos in positions) {
                // Determine active quantity at dateStr
                val buyTrades = pos.trades.filter { it.buySell.equals("BUY", ignoreCase = true) }
                val sellTrades = pos.trades.filter { it.buySell.equals("SELL", ignoreCase = true) }

                val qtyAtDate = if (buyTrades.isNotEmpty() || sellTrades.isNotEmpty()) {
                    val buysUpToDate = buyTrades.filter { it.date.trim().take(10) <= dateStr }.sumOf { it.quantity }
                    val sellsUpToDate = sellTrades.filter { it.date.trim().take(10) <= dateStr }.sumOf { it.quantity }
                    val netQty = buysUpToDate - sellsUpToDate
                    if (netQty > 0) netQty else if (buysUpToDate == 0.0 && dateStr >= (pos.trades.firstOrNull()?.date?.trim()?.take(10) ?: "")) pos.quantity else 0.0
                } else {
                    pos.quantity
                }

                if (qtyAtDate > 0.0) {
                    val priceMap = historicalPricesMap[pos.effectiveTicker] ?: emptyMap()
                    val priceAtDate = getHistoricalPrice(priceMap, dateStr, if (pos.markPrice > 0) pos.markPrice else pos.costBasisPrice)

                    val fxMap = historicalFxMap[pos.currency] ?: emptyMap()
                    val fxAtDate = if (pos.currency == targetCurrency) 1.0 else getHistoricalFxRate(fxMap, dateStr, if (pos.fxRateToPortfolio > 0) pos.fxRateToPortfolio else 1.0)

                    dayPositionsMarketValue += qtyAtDate * priceAtDate * fxAtDate
                }
            }

            val dayTotalPortfolioValue = dayPositionsMarketValue + totalCash
            val dateLabelStr = dateFormat.format(calendar.time)
            points.add(
                PortfolioHistoryPoint(
                    timestamp = calendar.timeInMillis,
                    dateLabel = dateLabelStr,
                    value = dayTotalPortfolioValue
                )
            )

            calendar.add(java.util.Calendar.DAY_OF_YEAR, dayStep)
        }

        // Scale history points so that the last point matches actual netLiquidation smoothly without discontinuity
        val currentNetLiq = portfolioData.accountSummary.netLiquidation
        val lastCalcValue = points.lastOrNull()?.value ?: 0.0
        val finalPoints = if (currentNetLiq > 0.0 && lastCalcValue > 0.0) {
            val scaleFactor = currentNetLiq / lastCalcValue
            points.map { it.copy(value = it.value * scaleFactor) }
        } else {
            points
        }

        val startValue = finalPoints.firstOrNull()?.value ?: 0.0
        val endValue = finalPoints.lastOrNull()?.value ?: 0.0
        val changeAmount = endValue - startValue
        val changePercentage = if (startValue > 0) (changeAmount / startValue) * 100.0 else 0.0

        PortfolioHistoryData(
            timeRange = timeRange,
            points = finalPoints,
            startValue = startValue,
            endValue = endValue,
            changeAmount = changeAmount,
            changePercentage = changePercentage
        )
    }

    private fun getHistoricalPrice(
        pricesMap: Map<String, Double>,
        dateStr: String,
        fallbackPrice: Double
    ): Double {
        val cleanDate = dateStr.trim().take(10)
        if (cleanDate.length < 10) return fallbackPrice

        pricesMap[cleanDate]?.let { return it }

        val lastDate = if (pricesMap is java.util.NavigableMap) pricesMap.lastKey() else pricesMap.keys.maxOrNull()
        if (lastDate != null && cleanDate >= lastDate && fallbackPrice > 0.0) {
            return fallbackPrice
        }

        if (pricesMap is java.util.NavigableMap) {
            val floorEntry = pricesMap.floorEntry(cleanDate)
            if (floorEntry != null) return floorEntry.value
        } else {
            val previousDates = pricesMap.keys.filter { it <= cleanDate }
            if (previousDates.isNotEmpty()) {
                val maxDate = previousDates.maxOrNull()
                if (maxDate != null) {
                    pricesMap[maxDate]?.let { return it }
                }
            }
        }
        return fallbackPrice
    }
}
