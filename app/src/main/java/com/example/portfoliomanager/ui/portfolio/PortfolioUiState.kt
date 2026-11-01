package com.example.portfoliomanager.ui.portfolio

import com.example.portfoliomanager.data.model.PortfolioData
import com.example.portfoliomanager.data.model.Position
import com.example.portfoliomanager.data.model.SectorAllocation

enum class CategoryFilter(val displayName: String) {
    ALL("All"),
    STOCKS("Stocks"),
    ETFS("ETFs"),
    BONDS("Bonds"),
    CASH("Cash"),
    OTHER("Other")
}

enum class PositionSortOption(val displayName: String) {
    SYMBOL("Symbol"),
    MARKET_VALUE("Market Value"),
    DAILY_PNL("Daily P&L"),
    PNL_PERCENTAGE("P&L %")
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

enum class BreakdownTab(val displayName: String) {
    GEOGRAPHY("Geography"),
    INDUSTRY("Industry"),
    CATEGORY("Category")
}

enum class MarginTab(val displayName: String) {
    EXPIRATION("Expiration"),
    OVERNIGHT("Overnight")
}

enum class BottomNavTab(val displayName: String) {
    HOME("Home"),
    ASSETS("Your Assets")
}

data class PortfolioUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val portfolioData: PortfolioData? = null,
    val filteredPositions: List<Position> = emptyList(),
    val sectorBreakdown: List<SectorAllocation> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryFilter: CategoryFilter = CategoryFilter.ALL,
    val selectedSortOption: PositionSortOption = PositionSortOption.MARKET_VALUE,
    val sortOrder: SortOrder = SortOrder.DESCENDING,
    val selectedBreakdownTab: BreakdownTab = BreakdownTab.INDUSTRY,
    val selectedMarginTab: MarginTab = MarginTab.OVERNIGHT,
    val selectedBottomNavTab: BottomNavTab = BottomNavTab.HOME,
    val isBalancesExpanded: Boolean = false,
    val isMarginExpanded: Boolean = false,
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
    val addTradePosition: Position? = null
)
