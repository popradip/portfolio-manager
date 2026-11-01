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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.portfoliomanager.data.mock.MockPortfolioData
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
        onTogglePositionExpanded = viewModel::togglePositionExpanded,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onCategoryFilterSelected = viewModel::onCategoryFilterSelected,
        onToggleSettingsDialog = viewModel::toggleSettingsDialog,
        onClearErrorMessage = viewModel::clearErrorMessage,
        onImportCsvClick = { csvPickerLauncher.launch("*/*") },
        onClearImportedData = viewModel::clearImportedCsvAndReset,
        onRefreshData = { viewModel.refreshPortfolioData(forceRefresh = true) },
        onBottomNavTabSelected = viewModel::onBottomNavTabSelected,
        onOpenAddTradeDialog = viewModel::onOpenAddTradeDialog,
        onDismissAddTradeDialog = viewModel::onDismissAddTradeDialog,
        onAddManualTrade = viewModel::addManualTrade,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreenContent(
    uiState: PortfolioUiState,
    onToggleBalancesExpanded: () -> Unit = {},
    onTogglePositionExpanded: (String) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onCategoryFilterSelected: (CategoryFilter) -> Unit = {},
    onToggleSettingsDialog: (Boolean) -> Unit = {},
    onClearErrorMessage: () -> Unit = {},
    onImportCsvClick: () -> Unit = {},
    onClearImportedData: () -> Unit = {},
    onRefreshData: () -> Unit = {},
    onBottomNavTabSelected: (BottomNavTab) -> Unit = {},
    onOpenAddTradeDialog: (com.example.portfoliomanager.data.model.Position) -> Unit = {},
    onDismissAddTradeDialog: () -> Unit = {},
    onAddManualTrade: (positionId: String, quantity: Double, price: Double, date: String, commission: Double) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val portfolioData = uiState.portfolioData ?: MockPortfolioData.getSamplePortfolio()

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
                        modifier = Modifier.fillMaxSize()
                    ) {
                    // Top Bar Header
                    item {
                        TopHeaderAndTabs(
                            onSettingsClick = { onToggleSettingsDialog(true) }
                        )
                    }

                    // Live Syncing Progress Indicator Banner
                    if (uiState.isRefreshing) {
                        item {
                            SyncingBanner()
                        }
                    }

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
                                    positionsCount = portfolioData.positions.size
                                )
                            }

                            // Expandable Balances Card
                            item {
                                ExpandableBalancesCard(
                                    balances = portfolioData.balances,
                                    isExpanded = uiState.isBalancesExpanded,
                                    onToggleExpand = onToggleBalancesExpanded
                                )
                            }

                            // Sector / Geography / Industry Analytics Breakdown Card
                            item {
                                BreakdownAnalyticsCard(
                                    selectedBreakdownTab = uiState.selectedBreakdownTab,
                                    sectorAllocations = uiState.sectorBreakdown.ifEmpty { portfolioData.sectorAllocations },
                                    geographyAllocations = portfolioData.geographyAllocations,
                                    positions = portfolioData.positions
                                )
                            }
                        }

                        BottomNavTab.ASSETS -> {
                            // Holdings / "Your Assets" Section
                            item {
                                YourAssetsSection(
                                    positions = uiState.filteredPositions.ifEmpty { portfolioData.positions },
                                    searchQuery = uiState.searchQuery,
                                    selectedCategoryFilter = uiState.selectedCategoryFilter,
                                    expandedPositionIds = uiState.expandedPositionIds,
                                    onSearchQueryChanged = onSearchQueryChanged,
                                    onCategoryFilterSelected = onCategoryFilterSelected,
                                    onTogglePositionExpanded = onTogglePositionExpanded,
                                    onAddTradeClick = onOpenAddTradeDialog
                                )
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
