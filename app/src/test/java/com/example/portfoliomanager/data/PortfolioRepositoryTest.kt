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
}
