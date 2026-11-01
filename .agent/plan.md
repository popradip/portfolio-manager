# Project Plan

Create an Android application "Portfolio Analyzer / Portfolio Manager" that integrates with Interactive Brokers (IBKR) account or Flex Web Service / API to display portfolio data (Balances, Margin Requirements, Assets breakdown, Positions, Cost Basis, Market Cap, Daily P&L, Total/Lost P&L, Geography/Industry/Marketing breakdowns) matching the provided modern clean UI design with rounded cards, soft shadows, bottom navigation bar (Portfolio Analyzer, etc.), pill tabs, expandable cards for Balances and Margin Requirements, search/filter for assets, and asset detail expansion.

## Project Brief

# Portfolio Analyzer / Portfolio Manager - Project Brief

## Features
- **Account Summary & Balances Dashboard**: View Net Liquidation Value, Cash Balances, Margin Requirements (Initial & Maintenance), and Daily/Total P&L via modern expandable cards.
- **Positions & Asset Holdings List**: Searchable and filterable asset list with expandable details for cost basis, market value, unrealized/realized P&L, and market cap.
- **Portfolio Breakdown & Sector Analytics**: Interactive pill tabs showing asset breakdowns across geography, industry, and sector allocations.
- **IBKR Flex Web Service Integration**: Integration layer to securely fetch and parse account data reports from Interactive Brokers.

## High-Level Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Navigation & Adaptive Strategy**: Jetpack Navigation 3 (state-driven) and Compose Material Adaptive library (`androidx.compose.material3.adaptive`)
- **Asynchronous & State Management**: Kotlin Coroutines, Flow, ViewModel
- **Networking**: Retrofit & OkHttp (for IBKR Flex Web Service API integration)

## UI Design Image
![UI Design](C:/Users/popra/AndroidStudioProjects/Portfoliomanager/input_images/portfolio_analyzer_ui_mockup.jpg)
Image path = C:/Users/popra/AndroidStudioProjects/Portfoliomanager/input_images/portfolio_analyzer_ui_mockup.jpg

## Implementation Steps
**Total Duration:** 14m 45s

### Task_1_DataAndNetworking: Implement IBKR Flex Web Service data models, Retrofit service API, XML parser, and repository for fetching account summary, positions, and sector allocations with sample fallback data.
- **Status:** COMPLETED
- **Updates:** Completed IBKR data models, Flex Web Service Retrofit API, XML parser, UserPreferencesRepository, PortfolioRepository, and MockPortfolioData. All unit tests and debug assemble passed.
- **Acceptance Criteria:**
  - IBKR Flex API query token / API_KEY integration configured and accessible
  - Data models for Account Summary, Positions, and Sector Allocations implemented
  - Repository returns parsed IBKR data or fallback mock data
  - build pass
- **Duration:** 4m 10s

### Task_2_ViewModelAndState: Implement PortfolioViewModel and UI state management for syncing IBKR data, searching/filtering positions, and calculating sector breakdown.
- **Status:** COMPLETED
- **Updates:** Created PortfolioUiState and PortfolioViewModel with StateFlow, position search/filter/sorting logic, expandable card states, credentials management, and 8 comprehensive unit tests. Build and unit tests passed.
- **Acceptance Criteria:**
  - PortfolioViewModel manages UI state with StateFlow
  - Search and filter logic for positions implemented
  - build pass
- **Duration:** 1m 57s

### Task_3_UIImplementation: Build Jetpack Compose UI matching design mockup including Account Summary cards, Positions list with search/filters, and Sector Analytics breakdown pill tabs.
- **Status:** COMPLETED
- **Updates:** Resolved UI refinement issues: category filter pills formatted with singleLine LazyRow layout preventing text wrapping, header styled with dark gradient matching mockup, and bottom navigation bar centered (+) FAB added. Build passed.
- **Acceptance Criteria:**
  - Account Summary & Balances Dashboard cards built
  - Searchable and filterable Asset Holdings list built
  - Sector/Geography/Industry breakdown tabs built
  - The implemented UI must match the design provided in C:/Users/popra/AndroidStudioProjects/Portfoliomanager/input_images/portfolio_analyzer_ui_mockup.jpg.
  - build pass
- **Duration:** 4m 29s

### Task_4_RunAndVerify: Integrate UI with ViewModel, run build, verify application stability, alignment with requirements, and visual fidelity.
- **Status:** COMPLETED
- **Updates:** Final verification passed. Critic agent confirmed application stability (0 crashes), horizontal rendering of "Bonds" filter pill on single line, dark gradient top header banner ("Portfolio Analyzer"), and centered (+) FAB in bottom navigation bar matching design mockup portfolio_analyzer_ui_mockup.jpg. All 15 unit tests and debug build passed.
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues
  - The implemented UI must match the design provided in C:/Users/popra/AndroidStudioProjects/Portfoliomanager/input_images/portfolio_analyzer_ui_mockup.jpg.
- **Duration:** 4m 9s

