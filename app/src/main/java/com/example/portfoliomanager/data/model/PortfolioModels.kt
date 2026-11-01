package com.example.portfoliomanager.data.model

/**
 * Core domain models for IBKR Portfolio Manager.
 */

data class AccountSummary(
    val accountId: String,
    val accountName: String,
    val currency: String,
    val netLiquidation: Double,
    val totalCashValue: Double,
    val settledCash: Double,
    val accruedCash: Double,
    val buyingPower: Double,
    val dailyPnl: Double,
    val dailyPnlPercentage: Double,
    val totalPnl: Double,
    val totalPnlPercentage: Double
)

data class Balances(
    val totalCash: Double,
    val settledCash: Double,
    val accruedCash: Double,
    val unrealizedPnl: Double,
    val realizedPnl: Double,
    val netLiquidation: Double
)

data class MarginRequirements(
    val initialMarginReq: Double,
    val maintMarginReq: Double,
    val usedMargin: Double,
    val availableMargin: Double,
    val excessLiquidity: Double,
    val marginRatioPercentage: Double
)

enum class AssetCategory {
    STK,    // Stocks
    OPT,    // Options
    CRYPTO, // Crypto
    ETF,    // Exchange-Traded Funds
    BOND,   // Bonds
    CASH,   // Cash & Forex
    OTHER
}

data class Trade(
    val id: String,
    val symbol: String,
    val date: String,
    val buySell: String,
    val quantity: Double,
    val price: Double,
    val commission: Double,
    val totalAmount: Double,
    val currency: String = "USD"
)

data class Position(
    val id: String,
    val symbol: String,
    val description: String,
    val assetCategory: AssetCategory,
    val quantity: Double,
    val costBasisPrice: Double,
    val markPrice: Double,
    val marketValue: Double,
    val dailyPnl: Double,
    val totalPnl: Double,
    val pnlPercentage: Double,
    val sector: String,
    val country: String,
    val sparklinePoints: List<Double> = emptyList(),
    val trades: List<Trade> = emptyList(),
    val currency: String = "USD",
    val fxRateToPortfolio: Double = 1.0
)

data class SectorAllocation(
    val sector: String,
    val value: Double,
    val percentage: Double,
    val colorHex: String
)

data class GeographyAllocation(
    val country: String,
    val value: Double,
    val percentage: Double
)

enum class PortfolioTimeRange(val label: String) {
    ONE_MONTH("1M"),
    THREE_MONTHS("3M"),
    SIX_MONTHS("6M"),
    ONE_YEAR("1R"),
    YTD("YTD"),
    ALL("ALL")
}

data class PortfolioHistoryPoint(
    val timestamp: Long,
    val dateLabel: String,
    val value: Double
)

data class PortfolioHistoryData(
    val timeRange: PortfolioTimeRange,
    val points: List<PortfolioHistoryPoint>,
    val startValue: Double,
    val endValue: Double,
    val changeAmount: Double,
    val changePercentage: Double
)

data class PortfolioData(
    val accountSummary: AccountSummary,
    val balances: Balances,
    val marginSummary: MarginRequirements,
    val positions: List<Position>,
    val sectorAllocations: List<SectorAllocation>,
    val geographyAllocations: List<GeographyAllocation>,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val isMockData: Boolean = false,
    val referenceCode: String? = null
)
