package com.example.portfoliomanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
val ActiveLimePill = Color(0xFFD2F53B)
val ActiveLimePillText = Color(0xFF0F172A)

// Dynamic theme colors adapted for Light and Dark modes
val PortfolioBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF0F172A) else Color(0xFFF6F7FB)

val CardBackground: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFFFFFFF)

val DarkPill: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFF0F172A)

val UnselectedPillBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFE2E8F0)

val UnselectedPillText: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF475569)

val TextPrimary: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFF8FAFC) else Color(0xFF0F172A)

val TextSecondary: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B)

val PositiveGreen = Color(0xFF10B981)
val PositiveGreenBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF064E3B) else Color(0xFFECFDF5)

val NegativeRed = Color(0xFFEF4444)
val NegativeRedBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF7F1D1D) else Color(0xFFFEF2F2)

val CardBorderColor: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFE2E8F0)

val InputFieldBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFFFFFFF)
