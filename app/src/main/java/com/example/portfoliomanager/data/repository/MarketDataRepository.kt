package com.example.portfoliomanager.data.repository

import com.example.portfoliomanager.data.model.GeographyAllocation
import com.example.portfoliomanager.data.model.PortfolioData
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

            // 4. Update each position with live prices and converted market value in target currency (EUR)
            val updatedPositions = updatedPositionCurrencies.map { pos ->
                val marketData = marketDataMap[pos.effectiveTicker]
                val fxRate = fxRatesMap[pos.currency] ?: 1.0

                if (marketData != null) {
                    val marketDataCurrency = marketData.currency.ifBlank { pos.currency }.uppercase()
                    val priceFxToPosCurrency = if (marketDataCurrency == pos.currency) {
                        1.0
                    } else {
                        val rateMarketToTarget = fxRatesMap[marketDataCurrency] ?: 1.0
                        if (fxRate > 0.0) rateMarketToTarget / fxRate else 1.0
                    }

                    val liveMarkPriceNative = marketData.currentPrice * priceFxToPosCurrency
                    val costBasisPriceNative = pos.costBasisPrice

                    val updatedMarketValueInTarget = pos.quantity * liveMarkPriceNative * fxRate
                    val updatedCostBasisTotalInTarget = pos.quantity * costBasisPriceNative * fxRate
                    val updatedTotalPnlInTarget = updatedMarketValueInTarget - updatedCostBasisTotalInTarget
                    val updatedPnlPercentage = if (updatedCostBasisTotalInTarget > 0.0) {
                        (updatedTotalPnlInTarget / updatedCostBasisTotalInTarget) * 100.0
                    } else pos.pnlPercentage

                    val updatedDailyPnlInTarget = marketData.dailyChange * priceFxToPosCurrency * pos.quantity * fxRate

                    pos.copy(
                        markPrice = liveMarkPriceNative,
                        marketValue = updatedMarketValueInTarget,
                        dailyPnl = updatedDailyPnlInTarget,
                        totalPnl = updatedTotalPnlInTarget,
                        pnlPercentage = updatedPnlPercentage,
                        sparklinePoints = if (marketData.sparklinePoints.isNotEmpty()) marketData.sparklinePoints else pos.sparklinePoints,
                        fxRateToPortfolio = fxRate
                    )
                } else {
                    val updatedMarketValueInTarget = pos.marketValue * fxRate
                    val updatedTotalPnlInTarget = pos.totalPnl * fxRate
                    val updatedDailyPnlInTarget = pos.dailyPnl * fxRate

                    pos.copy(
                        marketValue = updatedMarketValueInTarget,
                        totalPnl = updatedTotalPnlInTarget,
                        dailyPnl = updatedDailyPnlInTarget,
                        fxRateToPortfolio = fxRate
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
}
