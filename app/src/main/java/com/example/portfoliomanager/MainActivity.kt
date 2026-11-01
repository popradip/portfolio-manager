package com.example.portfoliomanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.portfoliomanager.ui.portfolio.PortfolioScreen
import com.example.portfoliomanager.ui.portfolio.PortfolioViewModel
import com.example.portfoliomanager.ui.theme.PortfolioManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PortfolioManagerTheme {
                val context = applicationContext
                val viewModel: PortfolioViewModel = viewModel(
                    factory = com.example.portfoliomanager.ui.portfolio.PortfolioViewModelFactory(context)
                )
                PortfolioScreen(viewModel = viewModel)
            }
        }
    }
}
