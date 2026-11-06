package com.example.portfoliomanager.ui.portfolio

import com.example.portfoliomanager.data.model.PortfolioData
import com.example.portfoliomanager.data.model.PortfolioHistoryData
import com.example.portfoliomanager.data.model.PortfolioTimeRange
import com.example.portfoliomanager.data.model.Position
import com.example.portfoliomanager.data.model.SectorAllocation
import com.example.portfoliomanager.data.model.Trade

enum class CategoryFilter(val displayName: String) {
    ALL("Všetky"),
    STOCKS("Akcie"),
    ETFS("ETF"),
    BONDS("Dlhopisy"),
    CASH("Hotovosť"),
    OTHER("Ostatné")
}

enum class PositionSortOption(val displayName: String) {
    SYMBOL("Symbol"),
    MARKET_VALUE("Trhová hodnota"),
    DAILY_PNL("Denný P&L"),
    PNL_PERCENTAGE("P&L %")
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

enum class BreakdownTab(val displayName: String) {
    GEOGRAPHY("Geografia"),
    INDUSTRY("Sektor"),
    CATEGORY("Kategória")
}

enum class MarginTab(val displayName: String) {
    EXPIRATION("Expirácia"),
    OVERNIGHT("Overnight")
}

enum class BottomNavTab(val displayName: String) {
    HOME("Prehľad"),
    ASSETS("Moje aktíva")
}

data class PortfolioUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val portfolioData: PortfolioData? = null,
    val filteredPositions: List<Position> = portfolioData?.positions ?: emptyList(),
    val sectorBreakdown: List<SectorAllocation> = portfolioData?.sectorAllocations ?: emptyList(),
    val searchQuery: String = "",
    val selectedCategoryFilter: CategoryFilter = CategoryFilter.ALL,
    val selectedSortOption: PositionSortOption = PositionSortOption.MARKET_VALUE,
    val sortOrder: SortOrder = SortOrder.DESCENDING,
    val selectedBreakdownTab: BreakdownTab = BreakdownTab.INDUSTRY,
    val selectedMarginTab: MarginTab = MarginTab.OVERNIGHT,
    val selectedBottomNavTab: BottomNavTab = BottomNavTab.HOME,
    val selectedTimeRange: PortfolioTimeRange = PortfolioTimeRange.ONE_YEAR,
    val portfolioHistoryData: PortfolioHistoryData? = null,
    val isBalancesExpanded: Boolean = false,
    val isMarginExpanded: Boolean = false,
    val isBreakdownExpanded: Boolean = false,
    val expandedPositionIds: Set<String> = emptySet(),
    val isSettingsDialogOpen: Boolean = false,
    val isMockMode: Boolean = true,
    val tokenInput: String = "",
    val queryIdInput: String = "",
    val isSavingCredentials: Boolean = false,
    val credentialsSavedMessage: String? = null,
    val isImportingCsv: Boolean = false,
    val importSuccessMessage: String? = null,
    val errorMessage: String? = null,
    val addTradePosition: Position? = null,
    val editTradePosition: Position? = null,
    val editTrade: Trade? = null,
    val deleteTradePosition: Position? = null,
    val deleteTrade: Trade? = null,
    val editTickerPosition: Position? = null,
    val tradesDialogPosition: Position? = null,
    val selectedPositionIdForScroll: String? = null
)
