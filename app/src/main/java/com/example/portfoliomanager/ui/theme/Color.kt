package com.example.portfoliomanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Default Material Palette
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Fixed Base Colors
val DarkPillBase = Color(0xFF0F172A)
val ActiveLimePill: Color
    @Composable get() = MaterialTheme.colorScheme.primary
val ActiveLimePillText: Color
    @Composable get() = MaterialTheme.colorScheme.onPrimary

// Dynamic theme colors adapted from MaterialTheme colorScheme for Material You / System theme integration
val PortfolioBg: Color
    @Composable get() = MaterialTheme.colorScheme.background

val CardBackground: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val DarkPill: Color
    @Composable get() = MaterialTheme.colorScheme.primary

val UnselectedPillBg: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val UnselectedPillText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val TextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

val TextSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val PositiveGreen = Color(0xFF10B981)
val PositiveGreenBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF064E3B) else Color(0xFFECFDF5)

val NegativeRed = Color(0xFFEF4444)
val NegativeRedBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF7F1D1D) else Color(0xFFFEF2F2)

val CardBorderColor: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant

val InputFieldBg: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant
