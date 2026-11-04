package com.example.portfoliomanager.ui.portfolio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.portfoliomanager.data.mock.MockPortfolioData
import com.example.portfoliomanager.data.model.PortfolioTimeRange
import com.example.portfoliomanager.ui.theme.DarkPill
import com.example.portfoliomanager.ui.theme.PortfolioBg
import com.example.portfoliomanager.ui.theme.PortfolioManagerTheme

@Composable
fun PortfolioScreen(
    modifier: Modifier = Modifier,
    viewModel: PortfolioViewModel = viewModel(
        factory = PortfolioViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importCsvFromUri(context, it) }
    }

    PortfolioScreenContent(
        uiState = uiState,
        onToggleBalancesExpanded = viewModel::toggleBalancesExpanded,
        onToggleBreakdownExpanded = viewModel::toggleBreakdownExpanded,
        onBreakdownTabSelected = viewModel::onBreakdownTabSelected,
        onTogglePositionExpanded = viewModel::togglePositionExpanded,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onCategoryFilterSelected = viewModel::onCategoryFilterSelected,
        onToggleSettingsDialog = viewModel::toggleSettingsDialog,
        onClearErrorMessage = viewModel::clearErrorMessage,
        onImportCsvClick = { csvPickerLauncher.launch("*/*") },
        onClearImportedData = viewModel::clearImportedCsvAndReset,
        onRefreshData = { viewModel.refreshPortfolioData(forceRefresh = true) },
        onBottomNavTabSelected = viewModel::onBottomNavTabSelected,
        onTimeRangeSelected = viewModel::onTimeRangeSelected,
        onOpenAddTradeDialog = viewModel::onOpenAddTradeDialog,
        onDismissAddTradeDialog = viewModel::onDismissAddTradeDialog,
        onAddManualTrade = viewModel::addManualTrade,
        onOpenEditTradeDialog = viewModel::onOpenEditTradeDialog,
        onDismissEditTradeDialog = viewModel::onDismissEditTradeDialog,
        onEditTrade = viewModel::editTrade,
        onOpenDeleteTradeDialog = viewModel::onOpenDeleteTradeDialog,
        onDismissDeleteTradeDialog = viewModel::onDismissDeleteTradeDialog,
        onDeleteTrade = viewModel::deleteTrade,
        onOpenEditTickerDialog = viewModel::onOpenEditTickerDialog,
        onDismissEditTickerDialog = viewModel::onDismissEditTickerDialog,
        onSaveTickerOverride = viewModel::saveTickerOverride,
        onPositionClick = viewModel::navigateToPosition,
        onPositionScrollHandled = viewModel::onPositionScrollHandled,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreenContent(
    uiState: PortfolioUiState,
    onToggleBalancesExpanded: () -> Unit = {},
    onToggleBreakdownExpanded: () -> Unit = {},
    onBreakdownTabSelected: (BreakdownTab) -> Unit = {},
    onTogglePositionExpanded: (String) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onCategoryFilterSelected: (CategoryFilter) -> Unit = {},
    onToggleSettingsDialog: (Boolean) -> Unit = {},
    onClearErrorMessage: () -> Unit = {},
    onImportCsvClick: () -> Unit = {},
    onClearImportedData: () -> Unit = {},
    onRefreshData: () -> Unit = {},
    onBottomNavTabSelected: (BottomNavTab) -> Unit = {},
    onTimeRangeSelected: (PortfolioTimeRange) -> Unit = {},
    onOpenAddTradeDialog: (com.example.portfoliomanager.data.model.Position) -> Unit = {},
    onDismissAddTradeDialog: () -> Unit = {},
    onAddManualTrade: (positionId: String, quantity: Double, price: Double, date: String, commission: Double) -> Unit = { _, _, _, _, _ -> },
    onOpenEditTradeDialog: (com.example.portfoliomanager.data.model.Position, com.example.portfoliomanager.data.model.Trade) -> Unit = { _, _ -> },
    onDismissEditTradeDialog: () -> Unit = {},
    onEditTrade: (tradeId: String, quantity: Double, price: Double, date: String, commission: Double, buySell: String) -> Unit = { _, _, _, _, _, _ -> },
    onOpenDeleteTradeDialog: (com.example.portfoliomanager.data.model.Position, com.example.portfoliomanager.data.model.Trade) -> Unit = { _, _ -> },
    onDismissDeleteTradeDialog: () -> Unit = {},
    onDeleteTrade: (tradeId: String) -> Unit = {},
    onOpenEditTickerDialog: (com.example.portfoliomanager.data.model.Position) -> Unit = {},
    onDismissEditTickerDialog: () -> Unit = {},
    onSaveTickerOverride: (originalSymbol: String, alternativeTicker: String) -> Unit = { _, _ -> },
    onPositionClick: (String) -> Unit = {},
    onPositionScrollHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val portfolioData = uiState.portfolioData ?: MockPortfolioData.getSamplePortfolio()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.selectedPositionIdForScroll, uiState.selectedBottomNavTab) {
        val targetId = uiState.selectedPositionIdForScroll
        if (targetId != null && uiState.selectedBottomNavTab == BottomNavTab.ASSETS) {
            val displayPositions = uiState.filteredPositions
            val targetIndex = displayPositions.indexOfFirst {
                it.id == targetId || it.symbol.equals(targetId, ignoreCase = true)
            }
            if (targetIndex != -1) {
                listState.animateScrollToItem(targetIndex + 1)
            }
            onPositionScrollHandled()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            PortfolioBottomNavBar(
                selectedTab = uiState.selectedBottomNavTab,
                onTabSelected = onBottomNavTabSelected
            )
        },
        containerColor = PortfolioBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(color = DarkPill)
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefreshData,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Error Feedback Banner
                        if (uiState.errorMessage != null) {
                            item {
                                ErrorBanner(
                                    errorMessage = uiState.errorMessage,
                                    onDismiss = onClearErrorMessage,
                                    onRetry = onRefreshData
                                )
                            }
                        }

                        when (uiState.selectedBottomNavTab) {
                            BottomNavTab.HOME -> {
                                // Account Summary & Net Liquidity Card
                                item {
                                    AccountSummaryCard(
                                        accountSummary = portfolioData.accountSummary,
                                        onImportClick = { onToggleSettingsDialog(true) }
                                    )
                                }

                                // Interactive Portfolio Evolution Chart Card
                                item {
                                    PortfolioEvolutionChartCard(
                                        historyData = uiState.portfolioHistoryData,
                                        selectedTimeRange = uiState.selectedTimeRange,
                                        onTimeRangeSelected = onTimeRangeSelected,
                                        dailyPnl = portfolioData.accountSummary.dailyPnl,
                                        dailyPnlPercentage = portfolioData.accountSummary.dailyPnlPercentage,
                                        currency = portfolioData.accountSummary.currency,
                                        positions = portfolioData.positions,
                                        onPositionClick = onPositionClick
                                    )
                                }

                                // Expandable Portfolio Statistics Card
                                item {
                                    ExpandableBalancesCard(
                                        portfolioData = portfolioData,
                                        isExpanded = uiState.isBalancesExpanded,
                                        onToggleExpand = onToggleBalancesExpanded
                                    )
                                }

                                // Sector / Geography / Industry Analytics Breakdown Card
                                item {
                                    BreakdownAnalyticsCard(
                                        selectedBreakdownTab = uiState.selectedBreakdownTab,
                                        sectorAllocations = uiState.sectorBreakdown,
                                        geographyAllocations = portfolioData.geographyAllocations,
                                        positions = portfolioData.positions,
                                        isExpanded = uiState.isBreakdownExpanded,
                                        onToggleExpand = onToggleBreakdownExpanded,
                                        onBreakdownTabSelected = onBreakdownTabSelected
                                    )
                                }
                            }

                            BottomNavTab.ASSETS -> {
                                val displayPositions = uiState.filteredPositions

                                item(key = "assets_header") {
                                    YourAssetsHeader(
                                        positionsCount = displayPositions.size,
                                        searchQuery = uiState.searchQuery,
                                        selectedCategoryFilter = uiState.selectedCategoryFilter,
                                        onSearchQueryChanged = onSearchQueryChanged,
                                        onCategoryFilterSelected = onCategoryFilterSelected
                                    )
                                }

                                if (displayPositions.isEmpty()) {
                                    item(key = "assets_empty") {
                                        YourAssetsEmptyState()
                                    }
                                } else {
                                    items(
                                        items = displayPositions,
                                        key = { position -> position.id }
                                    ) { position ->
                                        val isExpanded = uiState.expandedPositionIds.contains(position.id)
                                        PositionCard(
                                            position = position,
                                            isExpanded = isExpanded,
                                            onToggleExpand = { onTogglePositionExpanded(position.id) },
                                            onAddTradeClick = { onOpenAddTradeDialog(position) },
                                            onEditTickerClick = { onOpenEditTickerDialog(position) },
                                            onEditTradeClick = { trade -> onOpenEditTradeDialog(position, trade) },
                                            onDeleteTradeClick = { trade -> onOpenDeleteTradeDialog(position, trade) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CSV Import Modal Dialog
            IbkrSettingsDialog(
                isOpen = uiState.isSettingsDialogOpen,
                isImportingCsv = uiState.isImportingCsv,
                importSuccessMessage = uiState.importSuccessMessage,
                errorMessage = uiState.errorMessage,
                onImportCsvClick = onImportCsvClick,
                onClearImportedData = onClearImportedData,
                onDismiss = { onToggleSettingsDialog(false) }
            )

            // Add Manual Purchase Dialog
            AddTradeDialog(
                position = uiState.addTradePosition,
                onDismiss = onDismissAddTradeDialog,
                onSaveTrade = { quantity, price, date, commission ->
                    uiState.addTradePosition?.let { pos ->
                        onAddManualTrade(pos.id, quantity, price, date, commission)
                    }
                }
            )

            // Edit Trade Dialog
            EditTradeDialog(
                position = uiState.editTradePosition,
                trade = uiState.editTrade,
                onDismiss = onDismissEditTradeDialog,
                onSaveTrade = { quantity, price, date, commission, buySell ->
                    uiState.editTrade?.let { trade ->
                        onEditTrade(trade.id, quantity, price, date, commission, buySell)
                    }
                }
            )

            // Delete Trade Confirmation Dialog
            DeleteTradeDialog(
                position = uiState.deleteTradePosition,
                trade = uiState.deleteTrade,
                onDismiss = onDismissDeleteTradeDialog,
                onConfirmDelete = {
                    uiState.deleteTrade?.let { trade ->
                        onDeleteTrade(trade.id)
                    }
                }
            )

            // Edit Alternative Ticker Dialog
            EditTickerDialog(
                position = uiState.editTickerPosition,
                onDismiss = onDismissEditTickerDialog,
                onSave = onSaveTickerOverride
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PortfolioScreenPreview() {
    PortfolioManagerTheme {
        PortfolioScreenContent(
            uiState = PortfolioUiState(
                portfolioData = MockPortfolioData.getSamplePortfolio()
            )
        )
    }
}
