package com.example.portfoliomanager.data

import com.example.portfoliomanager.data.model.AccountSummary
import com.example.portfoliomanager.data.model.AssetCategory
import com.example.portfoliomanager.data.model.Balances
import com.example.portfoliomanager.data.model.MarginRequirements
import com.example.portfoliomanager.data.model.PortfolioData
import com.example.portfoliomanager.data.model.Position
import com.example.portfoliomanager.data.remote.YahooFinanceApiClient
import com.example.portfoliomanager.data.remote.YahooMarketData
import com.example.portfoliomanager.data.repository.MarketDataRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeYahooFinanceApiClient : YahooFinanceApiClient() {
    override suspend fun fetchMarketData(symbol: String): YahooMarketData? {
        return when (symbol.uppercase()) {
            "VWCE" -> YahooMarketData(
                symbol = "VWCE",
                yahooSymbol = "VWCE.DE",
                currentPrice = 175.0,
                previousClose = 170.0,
                dailyChange = 5.0,
                dailyChangePercentage = 2.94,
                sparklinePoints = listOf(168.0, 170.0, 172.0, 175.0),
                currency = "EUR"
            )
            "AAPL" -> YahooMarketData(
                symbol = "AAPL",
                yahooSymbol = "AAPL",
                currentPrice = 200.0,
                previousClose = 190.0,
                dailyChange = 10.0,
                dailyChangePercentage = 5.26,
                sparklinePoints = listOf(185.0, 190.0, 200.0),
                currency = "USD"
            )
            "AMZN.DE" -> YahooMarketData(
                symbol = "AMZN.DE",
                yahooSymbol = "AMZN",
                currentPrice = 200.0,
                previousClose = 190.0,
                dailyChange = 10.0,
                dailyChangePercentage = 5.26,
                sparklinePoints = listOf(185.0, 190.0, 200.0),
                currency = "USD"
            )
            else -> null
        }
    }

    override suspend fun fetchExchangeRate(fromCurrency: String, toCurrency: String): Double {
        return if (fromCurrency == "USD" && toCurrency == "EUR") 0.90 else 1.0
    }
}

class MarketDataRepositoryTest {

    @Test
    fun enrichPortfolioWithLivePrices_updatesPositionPricesAndCalculatesPnl() = runTest {
        val fakeClient = FakeYahooFinanceApiClient()
        val repository = MarketDataRepositoryImpl(yahooApiClient = fakeClient)

        val testPortfolio = PortfolioData(
            accountSummary = AccountSummary("U123", "Test Account", "EUR", 1000.0, 100.0, 100.0, 0.0, 1000.0, 0.0, 0.0, 0.0, 0.0),
            balances = Balances(100.0, 100.0, 0.0, 0.0, 0.0, 1000.0),
            marginSummary = MarginRequirements(0.0, 0.0, 0.0, 1000.0, 1000.0, 0.0),
            positions = listOf(
                Position("1", "VWCE", "Vanguard ETF", AssetCategory.ETF, 5.0, 160.0, 165.0, 825.0, 0.0, 25.0, 3.125, "Broad Market", "Ireland"),
                Position("2", "AAPL", "Apple Inc.", AssetCategory.STK, 2.0, 180.0, 190.0, 380.0, 0.0, 20.0, 5.55, "Technology", "United States"),
                Position("3", "AMZN.DE", "Amazon.com", AssetCategory.STK, 10.0, 170.0, 170.0, 1700.0, 0.0, 0.0, 0.0, "Consumer Cyclical", "Germany", currency = "EUR")
            ),
            sectorAllocations = emptyList(),
            geographyAllocations = emptyList()
        )

        val enriched = repository.enrichPortfolioWithLivePrices(testPortfolio)

        assertNotNull(enriched)
        val vwce = enriched.positions.find { it.symbol == "VWCE" }
        assertNotNull(vwce)
        assertEquals(175.0, vwce!!.markPrice, 0.001)
        assertEquals(5.0 * 175.0, vwce.marketValue, 0.001)
        assertTrue(vwce.sparklinePoints.isNotEmpty())
        assertEquals(4, vwce.sparklinePoints.size)

        val aapl = enriched.positions.find { it.symbol == "AAPL" }
        assertNotNull(aapl)
        assertEquals(200.0, aapl!!.markPrice, 0.001)

        val amzn = enriched.positions.find { it.symbol == "AMZN.DE" }
        assertNotNull(amzn)
        // AMZN.DE currency must be preserved as EUR despite Yahoo returning USD
        assertEquals("EUR", amzn!!.currency)
        // Mark price converted from USD ($200) to EUR using 0.90 rate = €180
        assertEquals(180.0, amzn.markPrice, 0.001)
        assertEquals(10.0 * 180.0, amzn.marketValue, 0.001)
    }
}
