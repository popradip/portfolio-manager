package com.example.portfoliomanager.data.mock

import com.example.portfoliomanager.data.model.AccountSummary
import com.example.portfoliomanager.data.model.AssetCategory
import com.example.portfoliomanager.data.model.Balances
import com.example.portfoliomanager.data.model.GeographyAllocation
import com.example.portfoliomanager.data.model.MarginRequirements
import com.example.portfoliomanager.data.model.PortfolioData
import com.example.portfoliomanager.data.model.Position
import com.example.portfoliomanager.data.model.SectorAllocation

object MockPortfolioData {

    fun getSamplePortfolio(): PortfolioData {
        val accountSummary = AccountSummary(
            accountId = "U12345678",
            accountName = "Primary Investment Account",
            currency = "USD",
            netLiquidation = 1245890.50,
            totalCashValue = 245890.50,
            settledCash = 220000.00,
            accruedCash = 25890.50,
            buyingPower = 2000000.00,
            dailyPnl = 12500.25,
            dailyPnlPercentage = 1.01,
            totalPnl = 185400.00,
            totalPnlPercentage = 17.48
        )

        val balances = Balances(
            totalCash = 245890.50,
            settledCash = 220000.00,
            accruedCash = 25890.50,
            unrealizedPnl = 145000.00,
            realizedPnl = 40400.00,
            netLiquidation = 1245890.50
        )

        val marginSummary = MarginRequirements(
            initialMarginReq = 165000.00,
            maintMarginReq = 150000.00,
            usedMargin = 150000.00,
            availableMargin = 850000.00,
            excessLiquidity = 850000.00,
            marginRatioPercentage = 15.0
        )

        val positions = listOf(
            Position(
                id = "pos_1",
                symbol = "AAPL",
                description = "Apple Inc.",
                assetCategory = AssetCategory.STK,
                quantity = 1000.0,
                costBasisPrice = 173.00,
                markPrice = 175.50,
                marketValue = 175500.00,
                dailyPnl = 2.50,
                totalPnl = 450.00,
                pnlPercentage = 1.45,
                sector = "Technology",
                country = "United States",
                sparklinePoints = listOf(170.0, 171.2, 172.5, 171.8, 174.0, 173.5, 175.5)
            ),
            Position(
                id = "pos_2",
                symbol = "BTC",
                description = "Bitcoin",
                assetCategory = AssetCategory.CRYPTO,
                quantity = 2.5,
                costBasisPrice = 28000.00,
                markPrice = 35000.00,
                marketValue = 87500.00,
                dailyPnl = 1200.00,
                totalPnl = 17500.00,
                pnlPercentage = 25.00,
                sector = "Digital Assets",
                country = "Global",
                sparklinePoints = listOf(28000.0, 29500.0, 31000.0, 30500.0, 33000.0, 35000.0)
            ),
            Position(
                id = "pos_3",
                symbol = "NVDA",
                description = "NVIDIA Corporation",
                assetCategory = AssetCategory.STK,
                quantity = 150.0,
                costBasisPrice = 110.00,
                markPrice = 135.00,
                marketValue = 202500.00,
                dailyPnl = 4120.00,
                totalPnl = 37500.00,
                pnlPercentage = 22.73,
                sector = "Technology",
                country = "United States",
                sparklinePoints = listOf(110.0, 115.0, 120.0, 122.0, 128.0, 135.0)
            ),
            Position(
                id = "pos_4",
                symbol = "SPY",
                description = "SPDR S&P 500 ETF Trust",
                assetCategory = AssetCategory.ETF,
                quantity = 600.0,
                costBasisPrice = 480.00,
                markPrice = 525.00,
                marketValue = 315000.00,
                dailyPnl = 1850.00,
                totalPnl = 27000.00,
                pnlPercentage = 9.38,
                sector = "Broad Market",
                country = "United States",
                sparklinePoints = listOf(480.0, 485.0, 492.0, 500.0, 510.0, 525.0)
            ),
            Position(
                id = "pos_5",
                symbol = "MSFT",
                description = "Microsoft Corporation",
                assetCategory = AssetCategory.STK,
                quantity = 425.0,
                costBasisPrice = 400.00,
                markPrice = 447.00,
                marketValue = 189975.00,
                dailyPnl = -850.00,
                totalPnl = 19975.00,
                pnlPercentage = 11.75,
                sector = "Technology",
                country = "United States",
                sparklinePoints = listOf(400.0, 410.0, 425.0, 440.0, 450.0, 447.0)
            ),
            Position(
                id = "pos_6",
                symbol = "AAPL 200 C 01/17/25",
                description = "Apple Jan 2025 Call Option",
                assetCategory = AssetCategory.OPT,
                quantity = 10.0,
                costBasisPrice = 12.00,
                markPrice = 18.50,
                marketValue = 18500.00,
                dailyPnl = 950.00,
                totalPnl = 6500.00,
                pnlPercentage = 54.17,
                sector = "Technology",
                country = "United States",
                sparklinePoints = listOf(12.0, 13.5, 14.0, 16.2, 18.5)
            ),
            Position(
                id = "pos_7",
                symbol = "AMZN",
                description = "Amazon.com Inc.",
                assetCategory = AssetCategory.STK,
                quantity = 600.0,
                costBasisPrice = 160.00,
                markPrice = 185.00,
                marketValue = 111000.00,
                dailyPnl = 2100.00,
                totalPnl = 15000.00,
                pnlPercentage = 15.63,
                sector = "Consumer Cyclical",
                country = "United States",
                sparklinePoints = listOf(160.0, 165.0, 172.0, 180.0, 185.0)
            ),
            Position(
                id = "pos_8",
                symbol = "JPM",
                description = "JPMorgan Chase & Co.",
                assetCategory = AssetCategory.STK,
                quantity = 500.0,
                costBasisPrice = 180.00,
                markPrice = 200.00,
                marketValue = 100000.00,
                dailyPnl = 630.25,
                totalPnl = 10000.00,
                pnlPercentage = 11.11,
                sector = "Financials",
                country = "United States",
                sparklinePoints = listOf(180.0, 184.0, 190.0, 195.0, 200.0)
            )
        )

        val sectorAllocations = listOf(
            SectorAllocation("Technology", 586475.0, 47.07, "#2196F3"),
            SectorAllocation("Broad Market", 315000.0, 25.28, "#4CAF50"),
            SectorAllocation("Consumer Cyclical", 111000.0, 8.91, "#FF9800"),
            SectorAllocation("Financials", 100000.0, 8.03, "#9C27B0"),
            SectorAllocation("Digital Assets", 87500.0, 7.02, "#F44336"),
            SectorAllocation("Cash & Other", 45915.5, 3.69, "#607D8B")
        )

        val geographyAllocations = listOf(
            GeographyAllocation("United States", 1112475.0, 89.29),
            GeographyAllocation("Global", 87500.0, 7.02),
            GeographyAllocation("Europe", 30000.0, 2.41),
            GeographyAllocation("Asia-Pacific", 15915.5, 1.28)
        )

        return PortfolioData(
            accountSummary = accountSummary,
            balances = balances,
            marginSummary = marginSummary,
            positions = positions,
            sectorAllocations = sectorAllocations,
            geographyAllocations = geographyAllocations,
            lastUpdatedTimestamp = System.currentTimeMillis(),
            isMockData = true,
            referenceCode = "MOCK_REF_9999"
        )
    }
}
