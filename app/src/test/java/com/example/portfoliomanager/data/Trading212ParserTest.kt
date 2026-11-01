package com.example.portfoliomanager.data

import com.example.portfoliomanager.data.parser.BrokerCsvParserRegistry
import com.example.portfoliomanager.data.parser.Trading212CsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class Trading212ParserTest {

    private lateinit var parser: Trading212CsvParser
    private lateinit var registry: BrokerCsvParserRegistry

    @Before
    fun setUp() {
        parser = Trading212CsvParser()
        registry = BrokerCsvParserRegistry()
    }

    @Test
    fun testParseRealTrading212CsvFile() {
        val sampleFile = File("""C:\Users\popra\Downloads\from_2025-11-01_to_2026-11-01_MTc5MzUzNTg3NDM2Ng.csv""")
        if (!sampleFile.exists()) {
            println("Sample file does not exist at path: ${sampleFile.absolutePath}")
            return
        }

        val csvContent = sampleFile.readText()

        assertTrue("Trading212CsvParser should detect Trading 212 CSV file", parser.canParse(csvContent))

        val parseResult = registry.parse(csvContent)
        assertEquals("Trading 212", parseResult.brokerName)

        val portfolioData = parseResult.portfolioData
        assertNotNull(portfolioData)

        println("Account ID: ${portfolioData.accountSummary.accountId}")
        println("Positions count: ${portfolioData.positions.size}")
        println("Net liquidation: ${portfolioData.accountSummary.netLiquidation}")

        // Account details
        assertEquals("T212-Account", portfolioData.accountSummary.accountId)
        assertEquals("EUR", portfolioData.accountSummary.currency)

        // Verify position count extracted from the CSV
        assertTrue("Positions should be non-empty", portfolioData.positions.isNotEmpty())

        // Check specific positions from the Trading 212 sample file
        val msftPos = portfolioData.positions.find { it.symbol == "MSF" }
        assertNotNull("Microsoft position (MSF) should exist", msftPos)
        assertEquals("Microsoft", msftPos!!.description)
        assertTrue("Microsoft shares should be > 0", msftPos.quantity > 0.4)

        val appPos = portfolioData.positions.find { it.symbol == "APC" }
        assertNotNull("Apple position (APC) should exist", appPos)

        val mainPos = portfolioData.positions.find { it.symbol == "13M" }
        assertNotNull("Main Street Capital position (13M) should exist", mainPos)

        // Verify net liquidation is positive
        assertTrue("Net liquidation should be > 0", portfolioData.accountSummary.netLiquidation > 0.0)
    }
}
