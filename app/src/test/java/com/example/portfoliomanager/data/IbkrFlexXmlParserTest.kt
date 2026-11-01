package com.example.portfoliomanager.data

import com.example.portfoliomanager.data.model.AssetCategory
import com.example.portfoliomanager.data.remote.IbkrApiException
import com.example.portfoliomanager.data.remote.IbkrFlexXmlParser
import com.example.portfoliomanager.data.remote.formatIbkrErrorMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IbkrFlexXmlParserTest {

    private lateinit var parser: IbkrFlexXmlParser

    @Before
    fun setUp() {
        parser = IbkrFlexXmlParser()
    }

    @Test
    fun parseSendRequestResponse_success() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse status="Success">
                <ReferenceCode>987654321</ReferenceCode>
                <Url>https://ndcdyn.interactivebrokers.com/Universal/servlet/FlexStatementService.GetStatement</Url>
            </FlexStatementResponse>
        """.trimIndent()

        val result = parser.parseSendRequestResponse(xml)
        assertEquals("Success", result.status)
        assertEquals("987654321", result.referenceCode)
        assertEquals("https://ndcdyn.interactivebrokers.com/Universal/servlet/FlexStatementService.GetStatement", result.url)
    }

    @Test
    fun parseSendRequestResponse_successWithAttributes() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse status="Success" errorCode="0" referenceCode="1234567890" url="https://www.interactivebrokers.com/Universal/servlet/FlexStatementService.GetStatement"/>
        """.trimIndent()

        val result = parser.parseSendRequestResponse(xml)
        assertEquals("Success", result.status)
        assertEquals("1234567890", result.referenceCode)
        assertEquals("https://www.interactivebrokers.com/Universal/servlet/FlexStatementService.GetStatement", result.url)
    }

    @Test
    fun parseSendRequestResponse_warnGenerating() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse status="Warn">
                <ErrorCode>1018</ErrorCode>
                <ErrorMessage>Statement is being generated. Please try again shortly.</ErrorMessage>
            </FlexStatementResponse>
        """.trimIndent()

        val result = parser.parseSendRequestResponse(xml)
        assertEquals("Warn", result.status)
        assertEquals("1018", result.errorCode)
        assertEquals("Statement is being generated. Please try again shortly.", result.errorMessage)
    }

    @Test
    fun parseSendRequestResponse_warnGeneratingWithReferenceCode() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse status="Warn">
                <ReferenceCode>55667788</ReferenceCode>
                <Url>https://ndcdyn.interactivebrokers.com/Universal/servlet/FlexStatementService.GetStatement</Url>
                <ErrorCode>1018</ErrorCode>
                <ErrorMessage>Statement is being generated. Please try again shortly.</ErrorMessage>
            </FlexStatementResponse>
        """.trimIndent()

        val result = parser.parseSendRequestResponse(xml)
        assertEquals("Warn", result.status)
        assertEquals("55667788", result.referenceCode)
        assertEquals("https://ndcdyn.interactivebrokers.com/Universal/servlet/FlexStatementService.GetStatement", result.url)
        assertEquals("1018", result.errorCode)
        assertEquals("Statement is being generated. Please try again shortly.", result.errorMessage)
    }

    @Test
    fun parseSendRequestResponse_warnGeneratingWithAttributesAndReferenceCode() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse status="Warn" errorCode="1018" referenceCode="99887766" url="https://www.interactivebrokers.com/Universal/servlet/FlexStatementService.GetStatement" errorMessage="Statement is generating"/>
        """.trimIndent()

        val result = parser.parseSendRequestResponse(xml)
        assertEquals("Warn", result.status)
        assertEquals("99887766", result.referenceCode)
        assertEquals("https://www.interactivebrokers.com/Universal/servlet/FlexStatementService.GetStatement", result.url)
        assertEquals("1018", result.errorCode)
        assertEquals("Statement is generating", result.errorMessage)
    }

    @Test(expected = IbkrApiException::class)
    fun parseGetStatementResponse_xmlErrorResponse_throwsIbkrApiException() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse status="Warn">
                <ErrorCode>1018</ErrorCode>
                <ErrorMessage>Statement is being generated. Try again in a few seconds.</ErrorMessage>
            </FlexStatementResponse>
        """.trimIndent()

        parser.parseGetStatementResponse(xml)
    }

    @Test
    fun parseGetStatementResponse_attributesWarn1018_throwsIbkrApiExceptionWithCode1018() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse status="Warn" errorCode="1018" errorMessage="Statement is being generated. Try again shortly."/>
        """.trimIndent()

        try {
            parser.parseGetStatementResponse(xml)
            org.junit.Assert.fail("Expected IbkrApiException")
        } catch (e: IbkrApiException) {
            assertEquals("1018", e.code)
            assertTrue(e.message?.contains("Statement is generating") == true)
        }
    }

    @Test
    fun formatIbkrErrorMessage_errorCode1025() {
        val message = formatIbkrErrorMessage("1025", "Too many failed attempts. Please review your configuration.")
        assertTrue(message.contains("1025"))
        assertTrue(message.contains("Too many failed attempts"))
        assertTrue(message.contains("10-15 minutes"))
        assertTrue(message.contains("Token vs Query ID"))
        assertTrue(message.contains("Flex Query Settings"))
    }

    @Test
    fun parseGetStatementResponse_errorCode1025_throwsIbkrApiExceptionWithFriendlyMessage() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse status="Fail">
                <ErrorCode>1025</ErrorCode>
                <ErrorMessage>Too many failed attempts. Please review your configuration.</ErrorMessage>
            </FlexStatementResponse>
        """.trimIndent()

        try {
            parser.parseGetStatementResponse(xml)
            org.junit.Assert.fail("Expected IbkrApiException")
        } catch (e: IbkrApiException) {
            assertEquals("1025", e.code)
            assertTrue(e.message?.contains("IBKR Error 1025") == true)
            assertTrue(e.message?.contains("10-15 minutes") == true)
            assertTrue(e.message?.contains("Token vs Query ID") == true)
            assertTrue(e.message?.contains("Flex Query Settings") == true)
        }
    }

    @Test
    fun parseGetStatementResponse_validXml() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexQueryResponse queryId="12345" type="AF">
                <FlexStatements count="1">
                    <FlexStatement accountId="U8888888" currency="USD">
                        <AccountInformation accountId="U8888888" acctAlias="Test Account" currency="USD" name="Jane Doe"/>
                        <EquitySummaryByReportDateInBase>
                            <EquitySummaryInBase accountId="U8888888" cash="150000.0" settledCash="140000.0" accruedCash="10000.0" netLiquidation="1000000.0" buyingPower="1500000.0" initialMarginReq="200000.0" maintMarginReq="180000.0" unrealizedPNL="250000.0" realizedPNL="5000.0"/>
                        </EquitySummaryByReportDateInBase>
                        <OpenPositions>
                            <OpenPosition accountId="U8888888" symbol="AAPL" description="Apple Inc." assetCategory="STK" position="500" costBasisPrice="150.0" markPrice="180.0" positionValue="90000.0" fifoPnlUnrealized="15000.0" sector="Technology" issuerCountryCode="US"/>
                            <OpenPosition accountId="U8888888" symbol="BTC" description="Bitcoin" assetCategory="CRYPTO" position="1.0" costBasisPrice="30000.0" markPrice="60000.0" positionValue="60000.0" fifoPnlUnrealized="30000.0" sector="Digital Assets" issuerCountryCode="US"/>
                        </OpenPositions>
                    </FlexStatement>
                </FlexStatements>
            </FlexQueryResponse>
        """.trimIndent()

        val portfolioData = parser.parseGetStatementResponse(xml)

        assertNotNull(portfolioData)
        assertEquals("U8888888", portfolioData.accountSummary.accountId)
        assertEquals("Test Account", portfolioData.accountSummary.accountName)
        assertEquals(1000000.0, portfolioData.accountSummary.netLiquidation, 0.01)
        assertEquals(150000.0, portfolioData.accountSummary.totalCashValue, 0.01)
        assertEquals(180000.0, portfolioData.marginSummary.maintMarginReq, 0.01)
        assertEquals(820000.0, portfolioData.marginSummary.availableMargin, 0.01)

        assertEquals(2, portfolioData.positions.size)

        val aapl = portfolioData.positions.find { it.symbol == "AAPL" }
        assertNotNull(aapl)
        assertEquals(AssetCategory.STK, aapl?.assetCategory)
        assertEquals(90000.0, aapl?.marketValue ?: 0.0, 0.01)
        assertEquals("Technology", aapl?.sector)

        val btc = portfolioData.positions.find { it.symbol == "BTC" }
        assertNotNull(btc)
        assertEquals(AssetCategory.CRYPTO, btc?.assetCategory)
        assertEquals(60000.0, btc?.marketValue ?: 0.0, 0.01)

        assertTrue(portfolioData.sectorAllocations.isNotEmpty())
        assertTrue(portfolioData.geographyAllocations.isNotEmpty())
    }
}
