package com.example.portfoliomanager.data

import com.example.portfoliomanager.data.remote.IbkrFlexApiClient
import com.example.portfoliomanager.data.repository.PortfolioRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortfolioRepositoryTest {

    @Test
    fun getPortfolioData_emptyToken_returnsMockData() = runBlocking {
        val repository = PortfolioRepositoryImpl()
        val result = repository.getPortfolioData(token = "", queryId = "")

        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertNotNull(data)
        assertTrue(data!!.isMockData)
        assertEquals(1245890.50, data.accountSummary.netLiquidation, 0.01)
        assertTrue(data.positions.isNotEmpty())
    }

    @Test
    fun getPortfolioData_invalidCredentials_returnsFailure() = runBlocking {
        // Test client pointing to invalid url / empty response
        val mockClient = object : IbkrFlexApiClient("https://invalid.interactivebrokers.com/") {}
        val repository = PortfolioRepositoryImpl(apiClient = mockClient)

        val result = repository.getPortfolioData(token = "INVALID_TOKEN", queryId = "INVALID_QID", forceRefresh = true)

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getPortfolioData_mockDataContainsPositionsAndAllocations() = runBlocking {
        val repository = PortfolioRepositoryImpl()
        val result = repository.getPortfolioData(forceRefresh = true)

        val data = result.getOrThrow()
        assertTrue(data.positions.size >= 5)
        assertTrue(data.sectorAllocations.any { it.sector == "Technology" })
        assertTrue(data.geographyAllocations.any { it.country == "United States" })
    }

    @Test
    fun addManualTrade_smallAddition_doesNotDuplicateAllTransactions() = runBlocking {
        val repository = PortfolioRepositoryImpl()
        val initialData = repository.getPortfolioData(forceRefresh = true).getOrThrow()
        val firstPos = initialData.positions.first()
        val initialQty = firstPos.quantity

        val smallTrade = com.example.portfoliomanager.data.model.Trade(
            id = "manual_small_1",
            symbol = firstPos.symbol,
            date = "2024-05-20",
            buySell = "BUY",
            quantity = 0.29,
            price = 10.0,
            commission = 0.0,
            totalAmount = 2.90
        )

        val result = repository.addManualTrade(firstPos.id, smallTrade)
        assertTrue(result.isSuccess)
        val updatedData = result.getOrThrow()
        val updatedPos = updatedData.positions.find { it.symbol.equals(firstPos.symbol, ignoreCase = true) }
        assertNotNull(updatedPos)
        assertEquals(initialQty + 0.29, updatedPos!!.quantity, 0.01)
    }

    @Test
    fun addManualTrade_matchingBaseTrade_deduplicatesAndDoesNotDoubleQuantity() = runBlocking {
        val repository = PortfolioRepositoryImpl()
        val initialData = repository.getPortfolioData(forceRefresh = true).getOrThrow()
        val firstPos = initialData.positions.first()

        val baseTrade = firstPos.trades.firstOrNull() ?: com.example.portfoliomanager.data.model.Trade(
            id = "base_1",
            symbol = firstPos.symbol,
            date = "2024-03-10 12:00:00",
            buySell = "BUY",
            quantity = firstPos.quantity,
            price = firstPos.costBasisPrice,
            commission = 0.0,
            totalAmount = firstPos.quantity * firstPos.costBasisPrice
        )

        val duplicateManualTrade = com.example.portfoliomanager.data.model.Trade(
            id = "manual_dup_1",
            symbol = firstPos.symbol.substringBefore('.'),
            date = baseTrade.date.take(10),
            buySell = "BUY",
            quantity = baseTrade.quantity,
            price = baseTrade.price,
            commission = 0.0,
            totalAmount = baseTrade.quantity * baseTrade.price
        )

        val result = repository.addManualTrade(firstPos.id, duplicateManualTrade)
        assertTrue(result.isSuccess)
        val updatedData = result.getOrThrow()
        val updatedPos = updatedData.positions.find { it.symbol.equals(firstPos.symbol, ignoreCase = true) }
        assertNotNull(updatedPos)
        assertEquals(firstPos.quantity, updatedPos!!.quantity, 0.01)
    }
}
