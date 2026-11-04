package com.example.portfoliomanager.ui

import com.example.portfoliomanager.data.mock.MockPortfolioData
import com.example.portfoliomanager.data.model.AssetCategory
import com.example.portfoliomanager.data.model.PortfolioData
import com.example.portfoliomanager.data.repository.PortfolioRepository
import com.example.portfoliomanager.ui.portfolio.BottomNavTab
import com.example.portfoliomanager.ui.portfolio.CategoryFilter
import com.example.portfoliomanager.ui.portfolio.PortfolioViewModel
import com.example.portfoliomanager.ui.portfolio.PositionSortOption
import com.example.portfoliomanager.ui.portfolio.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakePortfolioRepository
    private lateinit var viewModel: PortfolioViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePortfolioRepository()
        viewModel = PortfolioViewModel(repository = fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization loads portfolio data and populates state`() {
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNotNull(state.portfolioData)
        assertEquals(8, state.filteredPositions.size)
        assertTrue(state.sectorBreakdown.isNotEmpty())
        assertNull(state.errorMessage)
    }

    @Test
    fun `refreshPortfolioData updates state when repository fails`() {
        fakeRepository.shouldFail = true
        viewModel.refreshPortfolioData(forceRefresh = true)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertEquals("Network error connecting to IBKR API", state.errorMessage)
    }

    @Test
    fun `search by query filters positions correctly`() {
        // Search symbol "AAPL"
        viewModel.onSearchQueryChanged("AAPL")
        var state = viewModel.uiState.value
        // Should match AAPL stock and AAPL call option
        assertEquals(2, state.filteredPositions.size)
        assertTrue(state.filteredPositions.all { it.symbol.contains("AAPL", ignoreCase = true) })

        // Search description "Bitcoin"
        viewModel.onSearchQueryChanged("Bitcoin")
        state = viewModel.uiState.value
        assertEquals(1, state.filteredPositions.size)
        assertEquals("BTC", state.filteredPositions.first().symbol)

        // Search non-existing query
        viewModel.onSearchQueryChanged("NON_EXISTENT_TICKER")
        state = viewModel.uiState.value
        assertTrue(state.filteredPositions.isEmpty())

        // Clearing search restores all positions
        viewModel.onSearchQueryChanged("")
        state = viewModel.uiState.value
        assertEquals(8, state.filteredPositions.size)
    }

    @Test
    fun `category filter filters positions correctly`() {
        // Filter by STOCKS
        viewModel.onCategoryFilterSelected(CategoryFilter.STOCKS)
        var state = viewModel.uiState.value
        assertTrue(state.filteredPositions.all { it.assetCategory == AssetCategory.STK })

        // Filter by ETFS
        viewModel.onCategoryFilterSelected(CategoryFilter.ETFS)
        state = viewModel.uiState.value
        assertEquals(1, state.filteredPositions.size)
        assertEquals(AssetCategory.ETF, state.filteredPositions.first().assetCategory)

        // Filter by ALL
        viewModel.onCategoryFilterSelected(CategoryFilter.ALL)
        state = viewModel.uiState.value
        assertEquals(8, state.filteredPositions.size)
    }

    @Test
    fun `sorting positions works as expected`() {
        // Sort by Market Value Descending (default)
        viewModel.onSortOptionSelected(PositionSortOption.MARKET_VALUE)
        viewModel.onSortOrderChanged(SortOrder.DESCENDING)
        var state = viewModel.uiState.value
        val topPosition = state.filteredPositions.first()
        assertEquals("SPY", topPosition.symbol) // SPY market value is 315000.00

        // Sort by Market Value Ascending
        viewModel.onSortOrderChanged(SortOrder.ASCENDING)
        state = viewModel.uiState.value
        assertEquals("pos_6", state.filteredPositions.first().id) // AAPL option is 18500.00

        // Sort by Symbol Ascending
        viewModel.onSortOptionSelected(PositionSortOption.SYMBOL)
        viewModel.onSortOrderChanged(SortOrder.ASCENDING)
        state = viewModel.uiState.value
        assertEquals("AAPL", state.filteredPositions.first().symbol)

        // Toggle sort order
        viewModel.toggleSortOrder()
        state = viewModel.uiState.value
        assertEquals(SortOrder.DESCENDING, state.sortOrder)
    }

    @Test
    fun `expandable UI state toggles correctly`() {
        // Balances expansion (default false)
        assertFalse(viewModel.uiState.value.isBalancesExpanded)
        viewModel.toggleBalancesExpanded()
        assertTrue(viewModel.uiState.value.isBalancesExpanded)
        viewModel.toggleBalancesExpanded()
        assertFalse(viewModel.uiState.value.isBalancesExpanded)

        // Margin expansion
        assertFalse(viewModel.uiState.value.isMarginExpanded)
        viewModel.toggleMarginExpanded()
        assertTrue(viewModel.uiState.value.isMarginExpanded)

        // Breakdown expansion (default false)
        assertFalse(viewModel.uiState.value.isBreakdownExpanded)
        viewModel.toggleBreakdownExpanded()
        assertTrue(viewModel.uiState.value.isBreakdownExpanded)
        viewModel.toggleBreakdownExpanded()
        assertFalse(viewModel.uiState.value.isBreakdownExpanded)

        // Position expansion
        assertTrue(viewModel.uiState.value.expandedPositionIds.isEmpty())
        viewModel.togglePositionExpanded("pos_1")
        assertTrue(viewModel.uiState.value.expandedPositionIds.contains("pos_1"))
        viewModel.togglePositionExpanded("pos_2")
        assertEquals(setOf("pos_1", "pos_2"), viewModel.uiState.value.expandedPositionIds)
        viewModel.togglePositionExpanded("pos_1")
        assertEquals(setOf("pos_2"), viewModel.uiState.value.expandedPositionIds)
    }

    @Test
    fun `navigateToPosition updates tab, clears search filters, expands position and sets scroll target`() {
        // Set search query and category filter
        viewModel.onSearchQueryChanged("SPY")
        viewModel.onCategoryFilterSelected(CategoryFilter.ETFS)

        // Navigate to pos_1
        viewModel.navigateToPosition("pos_1")

        val state = viewModel.uiState.value
        assertEquals(BottomNavTab.ASSETS, state.selectedBottomNavTab)
        assertEquals("", state.searchQuery)
        assertEquals(CategoryFilter.ALL, state.selectedCategoryFilter)
        assertTrue(state.expandedPositionIds.contains("pos_1"))
        assertEquals("pos_1", state.selectedPositionIdForScroll)

        // Handle scroll
        viewModel.onPositionScrollHandled()
        assertNull(viewModel.uiState.value.selectedPositionIdForScroll)
    }

    @Test
    fun `credentials management works correctly`() {
        viewModel.onTokenInputChanged("MY_SECRET_TOKEN")
        viewModel.onQueryIdInputChanged("998877")

        assertEquals("MY_SECRET_TOKEN", viewModel.uiState.value.tokenInput)
        assertEquals("998877", viewModel.uiState.value.queryIdInput)

        viewModel.saveCredentials()
        assertTrue(fakeRepository.saveCredentialsCalled)
        assertEquals("MY_SECRET_TOKEN", fakeRepository.storedToken)
        assertEquals("998877", fakeRepository.storedQueryId)
        assertEquals("Prihlasovacie údaje boli úspešne uložené.", viewModel.uiState.value.credentialsSavedMessage)

        viewModel.clearCredentialsMessage()
        assertNull(viewModel.uiState.value.credentialsSavedMessage)
    }

    @Test
    fun `sector breakdown is calculated dynamically from positions`() {
        val sectors = viewModel.uiState.value.sectorBreakdown
        assertTrue(sectors.isNotEmpty())

        val techSector = sectors.find { it.sector == "Technology" }
        assertNotNull(techSector)
        assertTrue(techSector!!.percentage > 0.0)

        val totalPercentage = sectors.sumOf { it.percentage }
        assertEquals(100.0, totalPercentage, 1.0)
    }

    @Test
    fun `importCsvContent updates uiState with success message`() {
        val testCsv = "header\ndata"
        viewModel.importCsvContent(testCsv)

        assertNotNull(viewModel.uiState.value.importSuccessMessage)
        assertTrue(viewModel.uiState.value.importSuccessMessage!!.contains("Interactive Brokers"))
        assertFalse(viewModel.uiState.value.isMockMode)
    }

    @Test
    fun `manual trade dialog opens and dismisses correctly`() {
        val targetPosition = viewModel.uiState.value.filteredPositions.first()
        assertNull(viewModel.uiState.value.addTradePosition)

        viewModel.onOpenAddTradeDialog(targetPosition)
        assertEquals(targetPosition, viewModel.uiState.value.addTradePosition)

        viewModel.onDismissAddTradeDialog()
        assertNull(viewModel.uiState.value.addTradePosition)
    }

    @Test
    fun `addManualTrade updates position quantity and closes dialog`() {
        val targetPosition = viewModel.uiState.value.filteredPositions.first()
        viewModel.onOpenAddTradeDialog(targetPosition)

        val initialQuantity = targetPosition.quantity
        viewModel.addManualTrade(
            positionId = targetPosition.id,
            quantity = 5.0,
            price = 180.0,
            date = "2025-03-30",
            commission = 1.0
        )

        val state = viewModel.uiState.value
        assertNull(state.addTradePosition)

        val updatedPosition = state.filteredPositions.find { it.id == targetPosition.id }
        assertNotNull(updatedPosition)
        assertEquals(initialQuantity + 5.0, updatedPosition!!.quantity, 0.001)
        assertTrue(updatedPosition.trades.any { it.price == 180.0 && it.quantity == 5.0 })
    }

    @Test
    fun `selecting ALL time range matches total portfolio PnL`() {
        val data = viewModel.uiState.value.portfolioData
        assertNotNull(data)
        val expectedTotalPnl = data!!.accountSummary.totalPnl

        viewModel.onTimeRangeSelected(com.example.portfoliomanager.data.model.PortfolioTimeRange.ALL)

        val historyData = viewModel.uiState.value.portfolioHistoryData
        assertNotNull(historyData)
        assertEquals(com.example.portfoliomanager.data.model.PortfolioTimeRange.ALL, historyData!!.timeRange)
        assertEquals(expectedTotalPnl, historyData.changeAmount, 0.01)
    }

    @Test
    fun `saveTickerOverride updates alternative ticker on position and state`() {
        val targetPosition = viewModel.uiState.value.filteredPositions.first()
        viewModel.onOpenEditTickerDialog(targetPosition)
        assertEquals(targetPosition, viewModel.uiState.value.editTickerPosition)

        viewModel.saveTickerOverride(targetPosition.symbol, "MPW.US")

        val state = viewModel.uiState.value
        assertNull(state.editTickerPosition)

        val updatedPosition = state.filteredPositions.find { it.symbol == targetPosition.symbol }
        assertNotNull(updatedPosition)
        assertEquals("MPW.US", updatedPosition!!.alternativeTicker)
        assertEquals("MPW.US", updatedPosition.effectiveTicker)
    }

    @Test
    fun `editTrade updates trade details and refreshes portfolio state`() {
        val targetPosition = viewModel.uiState.value.filteredPositions.first()
        viewModel.addManualTrade(targetPosition.id, 10.0, 150.0, "2024-01-01")
        val posWithTrade = viewModel.uiState.value.filteredPositions.find { it.id == targetPosition.id }!!
        val addedTrade = posWithTrade.trades.first()

        viewModel.onOpenEditTradeDialog(posWithTrade, addedTrade)
        assertEquals(posWithTrade, viewModel.uiState.value.editTradePosition)
        assertEquals(addedTrade, viewModel.uiState.value.editTrade)

        viewModel.editTrade(addedTrade.id, 20.0, 160.0, "2024-01-02", 2.0, "BUY")

        val state = viewModel.uiState.value
        assertNull(state.editTradePosition)
        assertNull(state.editTrade)

        val updatedPos = state.filteredPositions.find { it.id == targetPosition.id }!!
        val editedTrade = updatedPos.trades.find { it.id == addedTrade.id }
        assertNotNull(editedTrade)
        assertEquals(20.0, editedTrade!!.quantity, 0.01)
        assertEquals(160.0, editedTrade.price, 0.01)
        assertEquals("2024-01-02", editedTrade.date)
    }

    @Test
    fun `deleteTrade removes trade and refreshes portfolio state`() {
        val targetPosition = viewModel.uiState.value.filteredPositions.first()
        viewModel.addManualTrade(targetPosition.id, 10.0, 150.0, "2024-01-01")
        val posWithTrade = viewModel.uiState.value.filteredPositions.find { it.id == targetPosition.id }!!
        val addedTrade = posWithTrade.trades.first()

        viewModel.onOpenDeleteTradeDialog(posWithTrade, addedTrade)
        assertEquals(posWithTrade, viewModel.uiState.value.deleteTradePosition)
        assertEquals(addedTrade, viewModel.uiState.value.deleteTrade)

        viewModel.deleteTrade(addedTrade.id)

        val state = viewModel.uiState.value
        assertNull(state.deleteTradePosition)
        assertNull(state.deleteTrade)

        val updatedPos = state.filteredPositions.find { it.id == targetPosition.id }!!
        assertTrue(updatedPos.trades.none { it.id == addedTrade.id })
    }
}

private class FakePortfolioRepository(
    var mockData: PortfolioData = MockPortfolioData.getSamplePortfolio(),
    var shouldFail: Boolean = false,
    var storedToken: String = "",
    var storedQueryId: String = ""
) : PortfolioRepository {

    var saveCredentialsCalled = false

    override suspend fun getPortfolioData(
        token: String?,
        queryId: String?,
        forceRefresh: Boolean
    ): Result<PortfolioData> {
        return if (shouldFail) {
            Result.failure(RuntimeException("Network error connecting to IBKR API"))
        } else {
            Result.success(mockData)
        }
    }

    override fun getPortfolioDataFlow(
        token: String?,
        queryId: String?
    ): Flow<Result<PortfolioData>> = flow {
        emit(getPortfolioData(token, queryId, forceRefresh = true))
    }

    override suspend fun saveIbkrCredentials(token: String, queryId: String) {
        storedToken = token
        storedQueryId = queryId
        saveCredentialsCalled = true
    }

    override fun getIbkrCredentials(): Flow<Pair<String, String>> {
        return flowOf(Pair(storedToken, storedQueryId))
    }

    override suspend fun importPortfolioCsv(csvContent: String): Result<Pair<String, PortfolioData>> {
        return Result.success(Pair("Interactive Brokers", mockData.copy(isMockData = false)))
    }

    override suspend fun clearImportedCsvData() {}

    override suspend fun resetManualEditsAndDeletedTrades(): Result<PortfolioData> {
        return Result.success(mockData)
    }

    override fun getSavedImportedCsv(): Flow<String?> = flowOf(null)

    override fun attachPreferencesRepository(preferencesRepository: com.example.portfoliomanager.data.local.UserPreferencesRepository) {}

    override suspend fun addManualTrade(
        positionId: String,
        trade: com.example.portfoliomanager.data.model.Trade
    ): Result<PortfolioData> {
        val updatedPositions = mockData.positions.map { pos ->
            if (pos.id == positionId) {
                val newQty = pos.quantity + trade.quantity
                val newTrades = pos.trades + trade
                pos.copy(quantity = newQty, trades = newTrades)
            } else pos
        }
        mockData = mockData.copy(positions = updatedPositions)
        return Result.success(mockData)
    }

    override suspend fun editTrade(
        trade: com.example.portfoliomanager.data.model.Trade
    ): Result<PortfolioData> {
        val updatedPositions = mockData.positions.map { pos ->
            if (pos.trades.any { it.id == trade.id }) {
                val newTrades = pos.trades.map { if (it.id == trade.id) trade else it }
                pos.copy(trades = newTrades)
            } else pos
        }
        mockData = mockData.copy(positions = updatedPositions)
        return Result.success(mockData)
    }

    override suspend fun deleteTrade(tradeId: String): Result<PortfolioData> {
        val updatedPositions = mockData.positions.map { pos ->
            if (pos.trades.any { it.id == tradeId }) {
                val newTrades = pos.trades.filterNot { it.id == tradeId }
                pos.copy(trades = newTrades)
            } else pos
        }
        mockData = mockData.copy(positions = updatedPositions)
        return Result.success(mockData)
    }

    override suspend fun saveTickerOverride(
        originalSymbol: String,
        alternativeTicker: String
    ): Result<PortfolioData> {
        val upperOriginal = originalSymbol.uppercase()
        val upperAlt = alternativeTicker.uppercase()
        val updatedPositions = mockData.positions.map { pos ->
            if (pos.symbol.uppercase() == upperOriginal) {
                pos.copy(alternativeTicker = upperAlt.ifBlank { null })
            } else pos
        }
        mockData = mockData.copy(positions = updatedPositions)
        return Result.success(mockData)
    }
}
