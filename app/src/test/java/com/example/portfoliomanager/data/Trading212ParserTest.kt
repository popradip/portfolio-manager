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

    @Test
    fun testIncrementalImportAndDeduplication() {
        val csvHeader = "Action,Time (UTC),ISIN,Ticker,Name,ID,No. of shares,Price / share,Currency (Price / share),Exchange rate,Total,Currency (Total)\n"
        val csvBatch1 = csvHeader +
                "Market buy,2024-12-17 14:30:01+00:00,US5949181045,MSFT,\"Microsoft\",EOF10001,1.0,400.0,USD,1.0,400.0,\"EUR\"\n" +
                "Market buy,2024-12-17 14:30:01+00:00,US0378331005,AAPL,\"Apple\",EOF10002,2.0,200.0,USD,1.0,400.0,\"EUR\"\n"

        // Batch 2 contains one duplicate (EOF10001) and one new transaction (EOF10003 for MSFT)
        val csvBatch2 = csvHeader +
                "Market buy,2024-12-17 14:30:01+00:00,US5949181045,MSFT,\"Microsoft\",EOF10001,1.0,400.0,USD,1.0,400.0,\"EUR\"\n" +
                "Market buy,2025-01-15 10:00:00+00:00,US5949181045,MSFT,\"Microsoft\",EOF10003,0.5,420.0,USD,1.0,210.0,\"EUR\"\n"

        val combinedCsv = csvBatch1 + "\n" + csvBatch2
        val portfolioData = parser.parse(combinedCsv)

        val msftPos = portfolioData.positions.find { it.symbol == "MSFT" }
        assertNotNull("MSFT position should exist", msftPos)
        // 1.0 from batch 1 + 0.5 from batch 2 = 1.5 shares total (EOF10001 from batch 2 was skipped as duplicate)
        assertEquals(1.5, msftPos!!.quantity, 0.0001)
        assertEquals(2, msftPos.trades.size) // EOF10001 and EOF10003

        val aaplPos = portfolioData.positions.find { it.symbol == "AAPL" }
        assertNotNull("AAPL position should exist", aaplPos)
        assertEquals(2.0, aaplPos!!.quantity, 0.0001)
    }
}
