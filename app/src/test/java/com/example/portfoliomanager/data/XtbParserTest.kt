package com.example.portfoliomanager.data

import com.example.portfoliomanager.data.parser.BrokerCsvParserRegistry
import com.example.portfoliomanager.data.parser.XtbCsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Base64

class XtbParserTest {

    private lateinit var parser: XtbCsvParser
    private lateinit var registry: BrokerCsvParserRegistry

    @Before
    fun setUp() {
        parser = XtbCsvParser()
        registry = BrokerCsvParserRegistry()
    }

    @Test
    fun testParseRealXtbXlsxFile() {
        val sampleFile = File("""C:\Users\popra\Downloads\50198829_2006-01-01_2026-11-01\50198829\EUR_50198829_2006-01-01_2026-11-01.xlsx""")
        if (!sampleFile.exists()) {
            println("Sample file does not exist at path: ${sampleFile.absolutePath}")
            return
        }

        val bytes = sampleFile.readBytes()
        val b64Str = Base64.getEncoder().encodeToString(bytes)
        val b64 = "BASE64_XLSX:" + b64Str

        assertTrue("XtbCsvParser should detect XTB XLSX file", parser.canParse(b64))

        val parseResult = registry.parse(b64)
        assertEquals("XTB", parseResult.brokerName)

        val portfolioData = parseResult.portfolioData
        assertNotNull(portfolioData)

        println("Account ID: ${portfolioData.accountSummary.accountId}")
        println("Cash value: ${portfolioData.accountSummary.totalCashValue}")
        println("Positions count: ${portfolioData.positions.size}")
        println("Net liquidation: ${portfolioData.accountSummary.netLiquidation}")

        // Check account details
        assertEquals("XTB-50198829", portfolioData.accountSummary.accountId)
        assertEquals("EUR", portfolioData.accountSummary.currency)

        // Verify total position count
        assertEquals(27, portfolioData.positions.size)

        // Verify cash calculation from Cash Operations
        assertEquals(171.18, portfolioData.accountSummary.totalCashValue, 0.01)

        // Verify specific positions from XTB report
        val mcdPos = portfolioData.positions.find { it.symbol == "MCD.US" }
        assertNotNull(mcdPos)
        assertEquals("McDonald's", mcdPos!!.description)
        assertEquals(17.0, mcdPos.quantity, 0.001)
        assertEquals(3911.02, mcdPos.marketValue, 0.01)

        val amznPos = portfolioData.positions.find { it.symbol == "AMZN.DE" }
        assertNotNull(amznPos)
        assertEquals("Amazon", amznPos!!.description)
        assertEquals(17.0, amznPos.quantity, 0.001)
        assertEquals(3655.0, amznPos.marketValue, 0.01)

        val sxr8Pos = portfolioData.positions.find { it.symbol == "SXR8.DE" }
        assertNotNull(sxr8Pos)
        assertEquals("Core S&P 500", sxr8Pos!!.description)
        assertEquals(12.1905, sxr8Pos.quantity, 0.0001)
        assertEquals(8934.39, sxr8Pos.marketValue, 0.01)

        // Verify trades/purchase history for positions
        assertTrue("McDonald's should have parsed trades", mcdPos.trades.isNotEmpty())
        assertEquals(17, mcdPos.trades.size)
        val firstMcdTrade = mcdPos.trades.first()
        assertEquals("BUY", firstMcdTrade.buySell)
        assertEquals(1.0, firstMcdTrade.quantity, 0.0001)
        assertEquals(270.0, firstMcdTrade.price, 0.01)

        val totalTradesCount = portfolioData.positions.sumOf { it.trades.size }
        assertTrue("Portfolio should contain imported trades across positions", totalTradesCount > 0)
        println("Total parsed trades across all positions: $totalTradesCount")
    }
}
