package com.example.portfoliomanager.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.portfoliomanager.data.model.AssetCategory
import com.example.portfoliomanager.data.model.PortfolioData
import com.example.portfoliomanager.data.model.Position
import com.example.portfoliomanager.data.model.SectorAllocation
import com.example.portfoliomanager.data.repository.PortfolioRepository
import com.example.portfoliomanager.data.repository.PortfolioRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

class PortfolioViewModel(
    private val repository: PortfolioRepository = PortfolioRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getIbkrCredentials().collect { (token, queryId) ->
                val hasCredentials = token.isNotBlank() && queryId.isNotBlank()
                _uiState.update { currentState ->
                    currentState.copy(
                        tokenInput = token,
                        queryIdInput = queryId,
                        isMockMode = if (hasCredentials && currentState.portfolioData == null) false else currentState.isMockMode
                    )
                }
            }
        }
        refreshPortfolioData(forceRefresh = false)
    }

    /**
     * Refreshes portfolio data from repository.
     */
    fun refreshPortfolioData(
        token: String? = null,
        queryId: String? = null,
        forceRefresh: Boolean = true
    ) {
        val currentState = _uiState.value
        val effectiveToken = if (currentState.isMockMode) "" else (token ?: currentState.tokenInput)
        val effectiveQueryId = if (currentState.isMockMode) "" else (queryId ?: currentState.queryIdInput)

        _uiState.update { state ->
            if (state.portfolioData == null) {
                state.copy(isLoading = true, errorMessage = null)
            } else {
                state.copy(isRefreshing = true, errorMessage = null)
            }
        }

        viewModelScope.launch {
            val result = repository.getPortfolioData(
                token = effectiveToken,
                queryId = effectiveQueryId,
                forceRefresh = forceRefresh
            )

            result.fold(
                onSuccess = { portfolioData ->
                    updatePortfolioDataState(portfolioData)
                },
                onFailure = { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = throwable.message ?: "Nepodarilo sa synchronizovať trhové dáta z Yahoo Finance."
                        )
                    }
                }
            )
        }
    }

    private fun updatePortfolioDataState(data: PortfolioData) {
        val state = _uiState.value
        val (filtered, sectors) = processPositionsAndSectors(
            positions = data.positions,
            searchQuery = state.searchQuery,
            categoryFilter = state.selectedCategoryFilter,
            sortOption = state.selectedSortOption,
            sortOrder = state.sortOrder,
            fallbackSectors = data.sectorAllocations
        )

        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                portfolioData = data,
                filteredPositions = filtered,
                sectorBreakdown = sectors,
                errorMessage = null,
                isMockMode = data.isMockData
            )
        }
    }

    /**
     * Updates search query and recalculates filtered positions & sector breakdown.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val data = state.portfolioData
            val positions = data?.positions ?: emptyList()
            val (filtered, sectors) = processPositionsAndSectors(
                positions = positions,
                searchQuery = query,
                categoryFilter = state.selectedCategoryFilter,
                sortOption = state.selectedSortOption,
                sortOrder = state.sortOrder,
                fallbackSectors = data?.sectorAllocations ?: emptyList()
            )

            state.copy(
                searchQuery = query,
                filteredPositions = filtered,
                sectorBreakdown = sectors
            )
        }
    }

    /**
     * Updates selected category filter and recalculates filtered positions & sector breakdown.
     */
    fun onCategoryFilterSelected(category: CategoryFilter) {
        _uiState.update { state ->
            val data = state.portfolioData
            val positions = data?.positions ?: emptyList()
            val (filtered, sectors) = processPositionsAndSectors(
                positions = positions,
                searchQuery = state.searchQuery,
                categoryFilter = category,
                sortOption = state.selectedSortOption,
                sortOrder = state.sortOrder,
                fallbackSectors = data?.sectorAllocations ?: emptyList()
            )

            state.copy(
                selectedCategoryFilter = category,
                filteredPositions = filtered,
                sectorBreakdown = sectors
            )
        }
    }

    /**
     * Updates position sorting option and recalculates filtered positions.
     */
    fun onSortOptionSelected(sortOption: PositionSortOption) {
        _uiState.update { state ->
            val data = state.portfolioData
            val positions = data?.positions ?: emptyList()
            val (filtered, sectors) = processPositionsAndSectors(
                positions = positions,
                searchQuery = state.searchQuery,
                categoryFilter = state.selectedCategoryFilter,
                sortOption = sortOption,
                sortOrder = state.sortOrder,
                fallbackSectors = data?.sectorAllocations ?: emptyList()
            )

            state.copy(
                selectedSortOption = sortOption,
                filteredPositions = filtered,
                sectorBreakdown = sectors
            )
        }
    }

    /**
     * Sets specific sort order (Ascending / Descending).
     */
    fun onSortOrderChanged(order: SortOrder) {
        _uiState.update { state ->
            val data = state.portfolioData
            val positions = data?.positions ?: emptyList()
            val (filtered, sectors) = processPositionsAndSectors(
                positions = positions,
                searchQuery = state.searchQuery,
                categoryFilter = state.selectedCategoryFilter,
                sortOption = state.selectedSortOption,
                sortOrder = order,
                fallbackSectors = data?.sectorAllocations ?: emptyList()
            )

            state.copy(
                sortOrder = order,
                filteredPositions = filtered,
                sectorBreakdown = sectors
            )
        }
    }

    /**
     * Toggles sort order between Ascending and Descending.
     */
    fun toggleSortOrder() {
        val newOrder = if (_uiState.value.sortOrder == SortOrder.ASCENDING) {
            SortOrder.DESCENDING
        } else {
            SortOrder.ASCENDING
        }
        onSortOrderChanged(newOrder)
    }

    /**
     * Toggles expansion state for Balances card.
     */
    fun toggleBalancesExpanded() {
        _uiState.update { it.copy(isBalancesExpanded = !it.isBalancesExpanded) }
    }

    /**
     * Toggles expansion state for Margin Requirements card.
     */
    fun toggleMarginExpanded() {
        _uiState.update { it.copy(isMarginExpanded = !it.isMarginExpanded) }
    }

    /**
     * Toggles expansion state for an individual asset/position detail card.
     */
    fun togglePositionExpanded(positionId: String) {
        _uiState.update { currentState ->
            val currentExpanded = currentState.expandedPositionIds
            val updatedExpanded = if (currentExpanded.contains(positionId)) {
                currentExpanded - positionId
            } else {
                currentExpanded + positionId
            }
            currentState.copy(expandedPositionIds = updatedExpanded)
        }
    }

    /**
     * Updates token input state.
     */
    fun onTokenInputChanged(token: String) {
        _uiState.update { it.copy(tokenInput = token) }
    }

    /**
     * Updates query ID input state.
     */
    fun onQueryIdInputChanged(queryId: String) {
        _uiState.update { it.copy(queryIdInput = queryId) }
    }

    /**
     * Saves user's IBKR API credentials and triggers a data refresh.
     */
    fun saveCredentials() {
        val state = _uiState.value
        val token = state.tokenInput.trim()
        val queryId = state.queryIdInput.trim()

        _uiState.update {
            it.copy(
                isSavingCredentials = true,
                errorMessage = null,
                isMockMode = false
            )
        }

        viewModelScope.launch {
            repository.saveIbkrCredentials(token, queryId)
            _uiState.update {
                it.copy(
                    isSavingCredentials = false,
                    credentialsSavedMessage = "Prihlasovacie údaje boli úspešne uložené.",
                    isMockMode = false
                )
            }
            refreshPortfolioData(
                token = token,
                queryId = queryId,
                forceRefresh = true
            )
        }
    }

    /**
     * Imports CSV content string, parses positions, updates state and persists imported data.
     */
    fun importCsvContent(csvContent: String) {
        if (csvContent.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vybraný CSV súbor je prázdny.") }
            return
        }

        _uiState.update { it.copy(isImportingCsv = true, errorMessage = null, importSuccessMessage = null) }

        viewModelScope.launch {
            val result = repository.importPortfolioCsv(csvContent)
            result.fold(
                onSuccess = { (brokerName, portfolioData) ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isImportingCsv = false,
                            importSuccessMessage = "Úspešne naimportované dáta z $brokerName (${portfolioData.positions.size} pozícií).",
                            isMockMode = false,
                            isSettingsDialogOpen = false
                        )
                    }
                    updatePortfolioDataState(portfolioData)
                },
                onFailure = { throwable ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isImportingCsv = false,
                            errorMessage = throwable.message ?: "Chyba pri importovaní CSV súboru."
                        )
                    }
                }
            )
        }
    }

    /**
     * Imports CSV or XLSX statement file from URI using ContentResolver.
     */
    fun importCsvFromUri(context: android.content.Context, uri: android.net.Uri) {
        try {
            val prefs = com.example.portfoliomanager.data.local.UserPreferencesRepository.getInstance(context)
            repository.attachPreferencesRepository(prefs)

            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.use { it.readBytes() } ?: byteArrayOf()

            val content = if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()) {
                val b64 = java.util.Base64.getEncoder().encodeToString(bytes)
                "BASE64_XLSX:$b64"
            } else {
                String(bytes, Charsets.UTF_8)
            }

            importCsvContent(content)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    errorMessage = "Súbor sa nepodarilo načítať: ${e.message}"
                )
            }
        }
    }

    /**
     * Clears CSV import success message.
     */
    fun clearImportSuccessMessage() {
        _uiState.update { it.copy(importSuccessMessage = null) }
    }

    /**
     * Clears imported CSV data and resets portfolio data.
     */
    fun clearImportedCsvAndReset() {
        viewModelScope.launch {
            repository.clearImportedCsvData()
            refreshPortfolioData(forceRefresh = true)
        }
    }

    /**
     * Opens dialog to manually add a new purchase/trade for a position.
     */
    fun onOpenAddTradeDialog(position: Position) {
        _uiState.update { it.copy(addTradePosition = position) }
    }

    /**
     * Dismisses the manual trade dialog.
     */
    fun onDismissAddTradeDialog() {
        _uiState.update { it.copy(addTradePosition = null) }
    }

    /**
     * Adds a manual purchase trade to a position, recalculates portfolio metrics and persists it.
     */
    fun addManualTrade(
        positionId: String,
        quantity: Double,
        price: Double,
        date: String,
        commission: Double = 0.0
    ) {
        val position = _uiState.value.portfolioData?.positions?.find { it.id == positionId }
            ?: _uiState.value.addTradePosition
            ?: return

        val tradeId = "manual_${System.currentTimeMillis()}"
        val totalAmount = (quantity * price) + commission
        val trade = com.example.portfoliomanager.data.model.Trade(
            id = tradeId,
            symbol = position.symbol,
            date = date.ifBlank {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                java.time.LocalDate.now().format(formatter)
            },
            buySell = "BUY",
            quantity = quantity,
            price = price,
            commission = commission,
            totalAmount = totalAmount
        )

        viewModelScope.launch {
            val result = repository.addManualTrade(positionId, trade)
            result.fold(
                onSuccess = { updatedPortfolioData ->
                    _uiState.update { state ->
                        state.copy(addTradePosition = null)
                    }
                    updatePortfolioDataState(updatedPortfolioData)
                },
                onFailure = { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            errorMessage = throwable.message ?: "Chyba pri pridávaní nákupu."
                        )
                    }
                }
            )
        }
    }

    /**
     * Clears credentials saved status message.
     */
    fun clearCredentialsMessage() {
        _uiState.update { it.copy(credentialsSavedMessage = null) }
    }

    /**
     * Clears error message.
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Updates selected breakdown tab (Geography, Industry, Category).
     */
    fun onBreakdownTabSelected(tab: BreakdownTab) {
        _uiState.update { it.copy(selectedBreakdownTab = tab) }
    }

    /**
     * Updates selected margin requirements tab (Expiration, Overnight).
     */
    fun onMarginTabSelected(tab: MarginTab) {
        _uiState.update { it.copy(selectedMarginTab = tab) }
    }

    /**
     * Updates active bottom navigation tab.
     */
    fun onBottomNavTabSelected(tab: BottomNavTab) {
        _uiState.update { it.copy(selectedBottomNavTab = tab) }
    }

    /**
     * Shows or hides IBKR Settings / Live Sync Modal Dialog.
     */
    fun toggleSettingsDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isSettingsDialogOpen = isOpen) }
    }

    /**
     * Toggles between Live IBKR and Mock data mode.
     */
    fun toggleMockMode(isMock: Boolean) {
        _uiState.update { it.copy(isMockMode = isMock, errorMessage = null) }
        if (isMock) {
            refreshPortfolioData(token = "", queryId = "", forceRefresh = true)
        } else {
            val state = _uiState.value
            refreshPortfolioData(token = state.tokenInput, queryId = state.queryIdInput, forceRefresh = true)
        }
    }

    // --- Helper calculations ---

    private fun processPositionsAndSectors(
        positions: List<Position>,
        searchQuery: String,
        categoryFilter: CategoryFilter,
        sortOption: PositionSortOption,
        sortOrder: SortOrder,
        fallbackSectors: List<SectorAllocation>
    ): Pair<List<Position>, List<SectorAllocation>> {
        val filtered = positions
            .filter { position ->
                matchesSearchQuery(position, searchQuery) && matchesCategory(position, categoryFilter)
            }
            .sortPositions(sortOption, sortOrder)

        val activeFiltering = searchQuery.isNotBlank() || categoryFilter != CategoryFilter.ALL
        val sectorsSource = if (activeFiltering) filtered else positions

        val sectors = if (sectorsSource.isNotEmpty()) {
            calculateSectorBreakdown(sectorsSource)
        } else if (!activeFiltering && fallbackSectors.isNotEmpty()) {
            fallbackSectors
        } else {
            emptyList()
        }

        return Pair(filtered, sectors)
    }

    private fun matchesSearchQuery(position: Position, query: String): Boolean {
        if (query.isBlank()) return true
        val trimmed = query.trim().lowercase(Locale.getDefault())
        return position.symbol.lowercase(Locale.getDefault()).contains(trimmed) ||
                position.description.lowercase(Locale.getDefault()).contains(trimmed) ||
                position.sector.lowercase(Locale.getDefault()).contains(trimmed)
    }

    private fun matchesCategory(position: Position, filter: CategoryFilter): Boolean {
        return when (filter) {
            CategoryFilter.ALL -> true
            CategoryFilter.STOCKS -> position.assetCategory == AssetCategory.STK
            CategoryFilter.ETFS -> position.assetCategory == AssetCategory.ETF
            CategoryFilter.BONDS -> position.assetCategory == AssetCategory.BOND
            CategoryFilter.CASH -> position.assetCategory == AssetCategory.CASH
            CategoryFilter.OTHER -> position.assetCategory == AssetCategory.OTHER
        }
    }

    private fun List<Position>.sortPositions(
        sortOption: PositionSortOption,
        sortOrder: SortOrder
    ): List<Position> {
        val comparator = when (sortOption) {
            PositionSortOption.SYMBOL -> compareBy<Position> { it.symbol.lowercase(Locale.getDefault()) }
            PositionSortOption.MARKET_VALUE -> compareBy { it.marketValue }
            PositionSortOption.DAILY_PNL -> compareBy { it.dailyPnl }
            PositionSortOption.PNL_PERCENTAGE -> compareBy { it.pnlPercentage }
        }
        return if (sortOrder == SortOrder.ASCENDING) {
            sortedWith(comparator)
        } else {
            sortedWith(comparator.reversed())
        }
    }

    private fun calculateSectorBreakdown(positions: List<Position>): List<SectorAllocation> {
        if (positions.isEmpty()) return emptyList()

        val totalMarketValue = positions.sumOf { it.marketValue.coerceAtLeast(0.0) }
        if (totalMarketValue <= 0) return emptyList()

        val groupedBySector = positions.groupBy { it.sector.ifBlank { "Other" } }

        val defaultPalette = listOf(
            "#2196F3", "#4CAF50", "#FF9800", "#9C27B0", "#F44336",
            "#00BCD4", "#E91E63", "#3F51B5", "#8BC34A", "#FFC107", "#607D8B"
        )

        return groupedBySector.entries.mapIndexed { index, entry ->
            val sector = entry.key
            val sectorPositions = entry.value
            val sectorValue = sectorPositions.sumOf { it.marketValue.coerceAtLeast(0.0) }
            val rawPercentage = (sectorValue / totalMarketValue) * 100.0
            val roundedPercentage = (rawPercentage * 100.0).roundToInt() / 100.0

            val colorHex = when (sector) {
                "Technology" -> "#2196F3"
                "Broad Market" -> "#4CAF50"
                "Consumer Cyclical" -> "#FF9800"
                "Financials" -> "#9C27B0"
                "Digital Assets" -> "#F44336"
                "Cash & Other" -> "#607D8B"
                else -> defaultPalette[index % defaultPalette.size]
            }

            SectorAllocation(
                sector = sector,
                value = sectorValue,
                percentage = roundedPercentage,
                colorHex = colorHex
            )
        }.sortedByDescending { it.value }
    }
}

class PortfolioViewModelFactory(
    private val context: android.content.Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val prefs = com.example.portfoliomanager.data.local.UserPreferencesRepository.getInstance(context)
        val repository = PortfolioRepositoryImpl(preferencesRepository = prefs)
        return PortfolioViewModel(repository) as T
    }
}
