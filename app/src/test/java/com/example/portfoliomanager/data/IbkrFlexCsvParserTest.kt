import com.example.portfoliomanager.data.parser.BrokerCsvParserRegistry
import com.example.portfoliomanager.data.parser.IbkrFlexCsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IbkrFlexCsvParserTest {

    private lateinit var parser: IbkrFlexCsvParser
    private lateinit var registry: BrokerCsvParserRegistry

    private val sampleIbkrCsvWithTrades = """
        "ClientAccountID","AccountAlias","Model","CurrencyPrimary","Name","AccountType","CustomerType","AccountCapabilities","TradingPermissions","AccountRepName","AccountRepPhone","DateOpened","DateFunded","DateClosed","LastTradedDate","Street","Street2","City","State","Country","PostalCode","StreetResidentialAddress","Street2ResidentialAddress","CityResidentialAddress","StateResidentialAddress","CountryResidentialAddress","PostalCodeResidentialAddress","MasterName","IBEntity","PrimaryEmail","BeneficiaryDesignations","SyepEnrollmentStatus","DividendReinvestmentEnabled","DateOfBirth","HasPendingTasks","TaxLotMatchingMethod","RecurringTransactions"
        "U25252051","","","EUR","Peter Popradi","Individual","Individual","Cash","Stocks,Warrants,Forex","","","20260411","20260412","","20261030","Kresankova 3582/7c","","Bratislava","SK-BL","Slovakia","841 05","Kresankova 3582/7c","","Bratislava","SK-BL","Slovakia","841 05","","IB-IE","popradi.p@gmail.com","","","No","19880429","No","FIFO",""
        "ClientAccountID","AccountAlias","Model","CurrencyPrimary","FromDate","ToDate","StartingValue","Mtm","MtmAtPaxos","Realized","ChangeInUnrealized","CostAdjustments","TransferredPnlAdjustments","DepositsWithdrawals","CarbonCredits","Donations","InternalCashTransfers","PaxosTransfers","ExcessFundSweep","AssetTransfers","DebitCardActivity","BillPay","GrantActivity","Dividends","WithholdingTax","871mWithholding","WithholdingTaxCollected","ChangeInDividendAccruals","ChangeInLiteSurchargeAccruals","ChangeInCGTWithholdingAccruals","Interest","ChangeInInterestAccruals","ChangeInIncentiveCouponAccruals","BrokerFees","changeInBrokerFeeAccruals","AdvisorFees","ClientFees","OtherFees","OtherIncome","FeesReceivables","Commissions","CommissionsAtPaxos","ReferralFee","CommissionCreditsRedemption","CommissionReceivables","ForexCommissions","TransactionTax","TaxReceivables","SalesTax","BillableSalesTax","SoftDollars","NetFxTrading","FxTranslation","LinkingAdjustments","Other","EndingValue","TWR","CorporateActionProceeds"
        "U25252051","","","EUR","20260101","20261030","0","2.6","0","0","0","0","0","3000","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","-3.8122596","0","0","0","0","0","0","0","0","0","0","0","0","0","0","2998.7877404","-0.040408653","0"
        "ClientAccountID","AccountAlias","Model","CurrencyPrimary","LevelOfDetail","FromDate","ToDate","StartingCash","StartingCashSecurities","StartingCashCommodities","ClientFees","ClientFeesSecurities","ClientFeesCommodities","Commissions","CommissionsSecurities","CommissionsCommodities","ReferralFee","ReferralFeeSecurities","ReferralFeeCommodities","CommissionCreditsRedemption","CommissionCreditsRedemptionSecurities","CommissionCreditsRedemptionCommodities","BillableCommissions","BillableCommissionsSecurities","BillableCommissionsCommodities","Deposit/Withdrawals","Deposit/WithdrawalsSecurities","Deposit/WithdrawalsCommodities","Deposits","DepositsSecurities","DepositsCommodities","Withdrawals","WithdrawalsSecurities","WithdrawalsCommodities","CarbonCredits","CarbonCreditsSecurities","CarbonCreditsCommodities","Donations","DonationsSecurities","DonationsCommodities","AccountTransfers","AccountTransfersSecurities","AccountTransfersCommodities","LinkingAdjustments","LinkingAdjustmentsSecurities","LinkingAdjustmentsCommodities","InternalTransfers","InternalTransfersSecurities","InternalTransfersCommodities","PaxosTransfers","PaxosTransfersSecurities","PaxosTransfersCommodities","ExcessFundSweep","ExcessFundSweepSec","ExcessFundSweepCom","DebitCardActivity","DebitCardActivitySecurities","DebitCardActivityCommodities","BillPay","BillPaySecurities","BillPayCommodities","Dividends","DividendsSecurities","DividendsCommodities","InsuredDepositInterest","InsuredDepositInterestSecurities","InsuredDepositInterestCommodities","BrokerInterest","BrokerInterestSecurities","BrokerInterestCommodities","BrokerFees","BrokerFeesSecurities","BrokerFeesCommodities","BondInterest","BondInterestSecurities","BondInterestCommodities","CashSettlingMtm","CashSettlingMtmSecurities","CashSettlingMtmCommodities","RealizedVm","RealizedVmSecurities","RealizedVmCommodities","RealizedForexVm","RealizedForexVmSecurities","RealizedForexVmCommodities","CFDCharges","CFDChargesSecurities","CFDChargesCommodities","NetTradesSales","NetTradesSalesSecurities","NetTradesSalesCommodities","NetTradesPurchases","NetTradesPurchasesSecurities","NetTradesPurchasesCommodities","AdvisorFees","AdvisorFeesSecurities","AdvisorFeesCommodities","FeesReceivables","FeesReceivablesSecurities","FeesReceivablesCommodities","PaymentInLieu","PaymentInLieuSecurities","PaymentInLieuCommodities","TransactionTax","TransactionTaxSecurities","TransactionTaxCommodities","TaxReceivables","TaxReceivablesSecurities","TaxReceivablesCommodities","WithholdingTax","WithholdingTaxSecurities","WithholdingTaxCommodities","871mWithholding","871mWithholdingSecurities","871mWithholdingCommodities","WithholdingTaxCollected","WithholdingTaxCollectedSecurities","WithholdingTaxCollectedCommodities","SalesTax","SalesTaxSecurities","SalesTaxCommodities","BillableSalesTax","BillableSalesTaxSecurities","BillableSalesTaxCommodities","ipoSubscription","ipoSubscriptionSecurities","ipoSubscriptionCommodities","FXTranslationGain/Loss","FXTranslationGain/LossSecurities","FXTranslationGain/LossCommodities","OtherFees","OtherFeesSecurities","OtherFeesCommodities","OtherIncome","OtherIncomeSecurities","OtherIncomeCommodities","Other","OtherSecurities","OtherCommodities","EndingCash","EndingCashSecurities","EndingCashCommodities","EndingSettledCash","EndingSettledCashSecurities","EndingSettledCashCommodities","StartingCashCollateralSLB","StartingCashCollateralSLBSecurities","StartingCashCollateralSLBCommodities","NetSecuritiesLentActivitySLB","NetSecuritiesLentActivitySLBSecurities","NetSecuritiesLentActivitySLBCommodities","EndingCashCollateralSLB","EndingCashCollateralSLBSecurities","EndingCashCollateralSLBCommodities","NetCashBalanceSLB","NetCashBalanceSLBSecurities","NetCashBalanceSLBCommodities","NetSettledCashBalanceSLB","NetSettledCashBalanceSLBSecurities","NetSettledCashBalanceSLBCommodities"
        "U25252051","","","BASE_SUMMARY","BaseCurrency","20260101","20261030","0","0","0","0","0","0","-3.8122596","-3.8122596","0","0","0","0","0","0","0","0","0","0","3000","3000","0","3190","3190","0","-190","-190","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","-841.9","-841.9","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","2154.2877404","2154.2877404","0","2490.2877404","2490.2877404","0","0","0","0","0","0","0","0","0","0","2154.2877404","2154.2877404","0","2490.2877404","2490.2877404","0"
        "ClientAccountID","AccountAlias","Model","CurrencyPrimary","FXRateToBase","AssetClass","SubCategory","Symbol","Description","Conid","SecurityID","SecurityIDType","CUSIP","ISIN","FIGI","ListingExchange","UnderlyingConid","UnderlyingSymbol","UnderlyingSecurityID","UnderlyingListingExchange","Issuer","IssuerCountryCode","Multiplier","Strike","Expiry","Put/Call","PrincipalAdjustFactor","ReportDate","Quantity","MarkPrice","PositionValue","OpenPrice","CostBasisPrice","CostBasisMoney","PercentOfNAV","FifoPnlUnrealized","Side","LevelOfDetail","OpenDateTime","HoldingPeriodDateTime","VestingDate","Code","OriginatingOrderID","OriginatingTransactionID","AccruedInterest","SerialNumber","DeliveryType","CommodityType","Fineness","Weight"
        "U25252051","","","EUR","1","STK","ETF","VWCE","VANG FTSE AW USDA","375858281","IE00BK5BQT80","ISIN","","IE00BK5BQT80","BBG00PQQJ374","IBIS2","","VWCE","","","","IE","1","","","","","20261030","5","168.9","844.5","169.1424518","169.1424518","845.712259","100.00","-1.212259","Long","SUMMARY","","","","","","","","","","","0.0","0.0"
        "ClientAccountID","AccountAlias","Model","CurrencyPrimary","FXRateToBase","AssetClass","SubCategory","Symbol","Description","Conid","SecurityID","SecurityIDType","CUSIP","ISIN","FIGI","ListingExchange","UnderlyingConid","UnderlyingSymbol","UnderlyingSecurityID","UnderlyingListingExchange","Issuer","IssuerCountryCode","TradeID","Multiplier","RelatedTradeID","Strike","ReportDate","Expiry","DateTime","Put/Call","TradeDate","PrincipalAdjustFactor","SettleDateTarget","TransactionType","Exchange","Quantity","TradePrice","TradeMoney","Proceeds","Taxes","IBCommission","IBCommissionCurrency","NetCash","ClosePrice","Open/CloseIndicator","Notes/Codes","CostBasis","FifoPnlRealized","MtmPnl","OrigTradePrice","OrigTradeDate","OrigTradeID","OrigOrderID","OrigTransactionID","Buy/Sell","ClearingFirmID","IBOrderID","TransactionID","IBExecID","RelatedTransactionID","RTN","BrokerageOrderID","OrderReference","VolatilityOrderLink","ExchOrderID","ExtExecID","OrderTime","OpenDateTime","HoldingPeriodDateTime","WhenRealized","WhenReopened","LevelOfDetail","ChangeInPrice","ChangeInQuantity","OrderType","TraderID","IsAPIOrder","AccruedInterest","InitialInvestment","PositionActionID","RequestID","SerialNumber","DeliveryType","CommodityType","Fineness","Weight"
        "U25252051","","","EUR","1","STK","ETF","VWCE","VANG FTSE AW USDA","375858281","IE00BK5BQT80","ISIN","","IE00BK5BQT80","BBG00PQQJ374","IBIS2","","VWCE","","","","IE","1627167079","1","","","20261022","","20261022;111158","","20261022","","20261026","ExchTrade","IBIS2","1","169.1","169.1","-169.1","0","-1.2774404","EUR","-170.3774404","169.28","O","","170.3774404","0","0.18","0","","","0","0","BUY","","1318855268","6907420616","000295b5.6ad9e09b.01.01","","","02420ddb.00031be2.6ad99204.0001","","","N/A","4260825:28344:20261022","20261022;051937","","","","","EXECUTION","0","0","LMT","C1","N","0","","","","","","","0.0","0.0"
        "U25252051","","","EUR","1","STK","ETF","VWCE","VANG FTSE AW USDA","375858281","IE00BK5BQT80","ISIN","","IE00BK5BQT80","BBG00PQQJ374","IBIS2","","VWCE","","","","IE","1628586807","1","","","20261023","","20261023;093848","","20261023","","20261027","ExchTrade","IBIS2","2","168.4","336.8","-336.8","0","-1.2848192","EUR","-338.0848192","169.3","O","","338.0848192","0","1.8","0","","","0","0","BUY","","1320012422","6915170117","000295b5.6adb1bd1.01.01","","","02420ddb.00031be2.6adae441.0001","","","N/A","4260825:23975:20261023","20261023;091737","","","","","EXECUTION","0","0","LMT","C1","N","0","","","","","","","0.0","0.0"
        "U25252051","","","EUR","1","STK","ETF","VWCE","VANG FTSE AW USDA","375858281","IE00BK5BQT80","ISIN","","IE00BK5BQT80","BBG00PQQJ374","IBIS2","","VWCE","","","","IE","1637856802","1","","","20261030","","20261030;150835","","20261030","","20261103","ExchTrade","GETTEX2","2","168","336","-336","0","-1.25","EUR","-337.25","168.9","O","","337.25","0","1.8","0","","","0","0","BUY","","1327423524","6958522332","0001ca29.6ae45664.01.01","","","02420ddb.00031be2.6ae43356.0001","","","N/A","632360810","20261030;150835","","","","","EXECUTION","0","0","LMT","C1","N","0","","","","","","","0.0","0.0"
    """.trimIndent()

    @Before
    fun setUp() {
        parser = IbkrFlexCsvParser()
        registry = BrokerCsvParserRegistry()
    }

    @Test
    fun canParse_returnsTrueForIbkrCsv() {
        assertTrue(parser.canParse(sampleIbkrCsvWithTrades))
    }

    @Test
    fun registry_autoDetectsIbkrAndParsesTrades() {
        val parseResult = registry.parse(sampleIbkrCsvWithTrades)
        assertEquals("Interactive Brokers (IBKR Flex Query)", parseResult.brokerName)

        val data = parseResult.portfolioData
        assertEquals("U25252051", data.accountSummary.accountId)
        assertEquals("Peter Popradi", data.accountSummary.accountName)
        assertEquals("EUR", data.accountSummary.currency)

        assertEquals(1, data.positions.size)
        val pos = data.positions[0]
        assertEquals("VWCE", pos.symbol)
        assertEquals(5.0, pos.quantity, 0.001)

        // Verify trades were parsed and attached!
        assertEquals(3, pos.trades.size)

        val trade1 = pos.trades[0]
        assertEquals("22. 10. 2026", trade1.date)
        assertEquals(1.0, trade1.quantity, 0.001)
        assertEquals(169.1, trade1.price, 0.001)
        assertEquals(1.2774404, trade1.commission, 0.0001)

        val trade2 = pos.trades[1]
        assertEquals("23. 10. 2026", trade2.date)
        assertEquals(2.0, trade2.quantity, 0.001)
        assertEquals(168.4, trade2.price, 0.001)

        val trade3 = pos.trades[2]
        assertEquals("30. 10. 2026", trade3.date)
        assertEquals(2.0, trade3.quantity, 0.001)
        assertEquals(168.0, trade3.price, 0.001)
    }
}
