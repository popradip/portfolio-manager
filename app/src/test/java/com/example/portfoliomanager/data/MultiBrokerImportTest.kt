package com.example.portfoliomanager.data

import com.example.portfoliomanager.data.local.UserPreferencesRepository
import com.example.portfoliomanager.data.repository.PortfolioRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Base64

class MultiBrokerImportTest {

    private val sampleIbkrCsv = """
        "ClientAccountID","AccountAlias","Model","CurrencyPrimary","Name","AccountType","CustomerType","AccountCapabilities","TradingPermissions","AccountRepName","AccountRepPhone","DateOpened","DateFunded","DateClosed","LastTradedDate","Street","Street2","City","State","Country","PostalCode","StreetResidentialAddress","Street2ResidentialAddress","CityResidentialAddress","StateResidentialAddress","CountryResidentialAddress","PostalCodeResidentialAddress","MasterName","IBEntity","PrimaryEmail","BeneficiaryDesignations","SyepEnrollmentStatus","DividendReinvestmentEnabled","DateOfBirth","HasPendingTasks","TaxLotMatchingMethod","RecurringTransactions"
        "U25252051","","","EUR","Peter Popradi","Individual","Individual","Cash","Stocks,Warrants,Forex","","","20260411","20260412","","20261030","Kresankova 3582/7c","","Bratislava","SK-BL","Slovakia","841 05","Kresankova 3582/7c","","Bratislava","SK-BL","Slovakia","841 05","","IB-IE","popradi.p@gmail.com","","","No","19880429","No","FIFO",""
        "ClientAccountID","AccountAlias","Model","CurrencyPrimary","FXRateToBase","AssetClass","SubCategory","Symbol","Description","Conid","SecurityID","SecurityIDType","CUSIP","ISIN","FIGI","ListingExchange","UnderlyingConid","UnderlyingSymbol","UnderlyingSecurityID","UnderlyingListingExchange","Issuer","IssuerCountryCode","Multiplier","Strike","Expiry","Put/Call","PrincipalAdjustFactor","ReportDate","Quantity","MarkPrice","PositionValue","OpenPrice","CostBasisPrice","CostBasisMoney","PercentOfNAV","FifoPnlUnrealized","Side","LevelOfDetail","OpenDateTime","HoldingPeriodDateTime","VestingDate","Code","OriginatingOrderID","OriginatingTransactionID","AccruedInterest","SerialNumber","DeliveryType","CommodityType","Fineness","Weight"
        "U25252051","","","EUR","1","STK","ETF","VWCE","VANG FTSE AW USDA","375858281","IE00BK5BQT80","ISIN","","IE00BK5BQT80","BBG00PQQJ374","IBIS2","","VWCE","","","","IE","1","","","","","20261030","10","100.0","1000.0","90.0","90.0","900.0","100.00","100.0","Long","SUMMARY","","","","","","","","","","","0.0","0.0"
    """.trimIndent()

    private class FakeUserPreferencesRepository : UserPreferencesRepository(null) {
        private val state = MutableStateFlow<String?>(null)
        override val importedCsvFlow: Flow<String?> = state

        override suspend fun saveImportedCsv(csvContent: String) {
            state.value = csvContent
        }

        override suspend fun clearImportedCsv() {
            state.value = null
        }
    }

    @Test
    fun testCombineIbkrXtbAndTrading212Imports() = runBlocking {
        val fakePrefs = FakeUserPreferencesRepository()
        val repository = PortfolioRepositoryImpl(preferencesRepository = fakePrefs)

        // 1. Import IBKR CSV
        val result1 = repository.importPortfolioCsv(sampleIbkrCsv)
        assertTrue(result1.isSuccess)
        val data1 = result1.getOrThrow().second
        assertEquals(1, data1.positions.size)
        assertEquals("VWCE", data1.positions[0].symbol)

        // 2. Import XTB XLSX
        val xtbFile = File("""C:\Users\popra\Downloads\50198829_2006-01-01_2026-11-01\50198829\EUR_50198829_2006-01-01_2026-11-01.xlsx""")
        if (xtbFile.exists()) {
            val xtbB64 = "BASE64_XLSX:" + Base64.getEncoder().encodeToString(xtbFile.readBytes())
            val result2 = repository.importPortfolioCsv(xtbB64)
            assertTrue(result2.isSuccess)

            val (brokerTitle2, combinedData2) = result2.getOrThrow()
            assertTrue("Title should mention IBKR & XTB", brokerTitle2.contains("Interactive Brokers") || brokerTitle2.contains("spojených"))
            assertEquals(31, combinedData2.positions.size)
        }

        // 3. Import Trading 212 CSV
        val t212File = File("""C:\Users\popra\Downloads\from_2025-11-01_to_2026-11-01_MTc5MzUzNTg3NDM2Ng.csv""")
        if (t212File.exists()) {
            val t212Csv = t212File.readText()
            val result3 = repository.importPortfolioCsv(t212Csv)
            assertTrue(result3.isSuccess)

            val (brokerTitle3, combinedData3) = result3.getOrThrow()
            assertTrue("Title should reflect multi-broker import", brokerTitle3.contains("spojených") || brokerTitle3.contains("Trading 212"))

            // Verify IBKR position exists in merged portfolio
            val vwcePos = combinedData3.positions.find { it.symbol == "VWCE" }
            assertNotNull("IBKR VWCE position should still exist", vwcePos)

            // Verify Trading 212 positions exist
            val msftPos = combinedData3.positions.find { it.symbol == "MSF" }
            assertNotNull("Trading 212 MSF position should exist", msftPos)

            // Verify portfolio totals are greater than single broker
            assertTrue("Combined net liquidation should be > 0", combinedData3.accountSummary.netLiquidation > 1000.0)
        }

        // 4. Test reloading portfolio data from repository
        val reloadResult = repository.getPortfolioData(forceRefresh = true)
        assertTrue(reloadResult.isSuccess)
        val reloadedData = reloadResult.getOrThrow()
        assertTrue("Reloaded data should contain merged positions", reloadedData.positions.isNotEmpty())
    }
}
