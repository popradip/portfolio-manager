package com.example.portfoliomanager.ui.portfolio

import com.example.portfoliomanager.data.model.Trade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.portfoliomanager.data.model.AccountSummary
import com.example.portfoliomanager.data.model.Balances
import com.example.portfoliomanager.data.model.GeographyAllocation
import com.example.portfoliomanager.data.model.MarginRequirements
import com.example.portfoliomanager.data.model.PortfolioHistoryData
import com.example.portfoliomanager.data.model.PortfolioHistoryPoint
import com.example.portfoliomanager.data.model.PortfolioTimeRange
import com.example.portfoliomanager.data.model.Position
import com.example.portfoliomanager.data.model.SectorAllocation
import com.example.portfoliomanager.ui.theme.ActiveLimePill
import com.example.portfoliomanager.ui.theme.ActiveLimePillText
import com.example.portfoliomanager.ui.theme.CardBackground
import com.example.portfoliomanager.ui.theme.CardBorderColor
import com.example.portfoliomanager.ui.theme.DarkPill
import com.example.portfoliomanager.ui.theme.InputFieldBg
import com.example.portfoliomanager.ui.theme.NegativeRed
import com.example.portfoliomanager.ui.theme.NegativeRedBg
import com.example.portfoliomanager.ui.theme.PositiveGreen
import com.example.portfoliomanager.ui.theme.PositiveGreenBg
import com.example.portfoliomanager.ui.theme.TextPrimary
import com.example.portfoliomanager.ui.theme.TextSecondary
import com.example.portfoliomanager.ui.theme.UnselectedPillBg
import com.example.portfoliomanager.ui.theme.UnselectedPillText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// --- Format Utilities ---

fun String.toCurrencySymbol(): String {
    return when (this.uppercase().trim()) {
        "EUR", "€" -> "€"
        "USD", "$" -> "$"
        "GBP", "£" -> "£"
        "CZK" -> "Kč "
        "PLN" -> "zł "
        "CHF" -> "CHF "
        "CAD" -> "CA$"
        "AUD" -> "A$"
        "JPY" -> "¥"
        "HUF" -> "Ft "
        else -> if (this.length == 1) this else "$this "
    }
}

fun Double.toCurrencyString(currencyCodeOrSymbol: String = "EUR"): String {
    val raw = currencyCodeOrSymbol.trim()
    val symbol = raw.toCurrencySymbol()
    val isSuffix = raw.uppercase() in listOf("CZK", "PLN", "HUF")
    return if (isSuffix) {
        String.format(Locale.US, "%,.2f %s", this, symbol.trim())
    } else {
        String.format(Locale.US, "%s%,.2f", symbol, this)
    }
}

fun Double.toPercentageString(includeSign: Boolean = true): String {
    val sign = if (includeSign && this > 0) "+" else ""
    return String.format(Locale.US, "%s%.2f%%", sign, this)
}

fun Double.toSignedCurrencyString(currencyCodeOrSymbol: String = "EUR"): String {
    val raw = currencyCodeOrSymbol.trim()
    val symbol = raw.toCurrencySymbol()
    val sign = if (this > 0) "+" else if (this < 0) "-" else ""
    val absVal = kotlin.math.abs(this)
    val isSuffix = raw.uppercase() in listOf("CZK", "PLN", "HUF")
    return if (isSuffix) {
        String.format(Locale.US, "%s%,.2f %s", sign, absVal, symbol.trim())
    } else {
        String.format(Locale.US, "%s%s%,.2f", sign, symbol, absVal)
    }
}

// --- Top Header & Breakdown Pills ---

@Composable
fun TopHeaderAndTabs(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B),
                            Color(0xFF0284C7),
                            Color(0xFF06B6D4)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Portfolio Manager",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = Color.White
                    )
                )

                // Import CSV Glassmorphic Button
                Surface(
                    onClick = onSettingsClick,
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileUpload,
                            contentDescription = "Import výpisu",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Import výpisu",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

// --- Sparkline Graph ---

@Composable
fun SparklineCanvas(
    points: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val minVal = points.minOrNull() ?: 0.0
        val maxVal = points.maxOrNull() ?: 1.0
        val range = (maxVal - minVal).coerceAtLeast(0.001)

        val path = Path()
        val stepX = width / (points.size - 1)

        points.forEachIndexed { index, point ->
            val x = index * stepX
            val normalizedY = ((point - minVal) / range).toFloat()
            val y = height - (normalizedY * height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                val prevX = (index - 1) * stepX
                val prevPoint = points[index - 1]
                val prevNormalizedY = ((prevPoint - minVal) / range).toFloat()
                val prevY = height - (prevNormalizedY * height)

                val controlX1 = prevX + (stepX / 2f)
                val controlY1 = prevY
                val controlX2 = prevX + (stepX / 2f)
                val controlY2 = y

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// --- Account Summary Card ---

@Composable
fun AccountSummaryCard(
    accountSummary: AccountSummary?,
    onImportClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTimeGmtStr = rememberTimeStr()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "$currentTimeGmtStr GMT+3",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    onClick = onImportClick,
                    shape = RoundedCornerShape(12.dp),
                    color = DarkPill,
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileUpload,
                            contentDescription = "Import výpisu",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Import výpisu",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Čistá hodnota portfólia",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            )

            val netLiquidation = accountSummary?.netLiquidation ?: 0.0
            val dailyPnl = accountSummary?.dailyPnl ?: 0.0
            val dailyPnlPct = accountSummary?.dailyPnlPercentage ?: 0.0
            val currency = accountSummary?.currency ?: "EUR"
            val isPositive = dailyPnl >= 0

            val sparklinePoints = remember(netLiquidation, dailyPnl) {
                val startVal = netLiquidation - dailyPnl
                if (kotlin.math.abs(dailyPnl) < 0.01) {
                    listOf(netLiquidation, netLiquidation)
                } else {
                    listOf(
                        startVal,
                        startVal + dailyPnl * 0.15,
                        startVal + dailyPnl * 0.35,
                        startVal + dailyPnl * 0.25,
                        startVal + dailyPnl * 0.65,
                        startVal + dailyPnl * 0.85,
                        netLiquidation
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = netLiquidation.toCurrencyString(currency),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            color = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPositive) PositiveGreenBg else NegativeRedBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.AutoMirrored.Rounded.TrendingUp else Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                tint = if (isPositive) PositiveGreen else NegativeRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${dailyPnl.toSignedCurrencyString(currency)} (${dailyPnlPct.toPercentageString()})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) PositiveGreen else NegativeRed
                                )
                            )
                        }
                    }
                }

                if (sparklinePoints.size >= 2) {
                    SparklineCanvas(
                        points = sparklinePoints,
                        lineColor = if (isPositive) PositiveGreen else NegativeRed,
                        modifier = Modifier
                            .width(90.dp)
                            .height(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberTimeStr(): String {
    val sdf = remember { SimpleDateFormat("hh:mm:ss a", Locale.US) }
    return sdf.format(Date())
}

enum class PortfolioChartType {
    LINE,
    BAR
}

// --- Portfolio Evolution Chart Card ---

@Composable
fun PortfolioEvolutionChartCard(
    historyData: PortfolioHistoryData?,
    selectedTimeRange: PortfolioTimeRange,
    onTimeRangeSelected: (PortfolioTimeRange) -> Unit,
    dailyPnl: Double = 0.0,
    dailyPnlPercentage: Double = 0.0,
    currency: String = "EUR",
    positions: List<Position> = emptyList(),
    onPositionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activePoint by remember(historyData) { mutableStateOf<PortfolioHistoryPoint?>(null) }
    var chartType by remember { mutableStateOf(PortfolioChartType.BAR) }
    val points = historyData?.points ?: emptyList()

    val activeIdx = if (activePoint != null && points.isNotEmpty()) points.indexOf(activePoint) else -1

    val dayChangeAmount = when {
        activePoint == null -> dailyPnl
        activeIdx > 0 -> activePoint!!.value - points[activeIdx - 1].value
        else -> 0.0
    }

    val dayChangePct = when {
        activePoint == null -> dailyPnlPercentage
        activeIdx > 0 && points[activeIdx - 1].value > 0 -> (dayChangeAmount / points[activeIdx - 1].value) * 100.0
        else -> 0.0
    }

    val isDayPositive = dayChangeAmount >= 0

    val rangeChangeAmount = historyData?.changeAmount ?: 0.0
    val rangeChangePercentage = historyData?.changePercentage ?: 0.0
    val isRangePositive = rangeChangeAmount >= 0

    val displayValue = activePoint?.value ?: historyData?.endValue ?: 0.0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Title & Active Point Date Label / Chart Type Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = DarkPill,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Vývoj hodnoty portfólia",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                // Chart Type Toggle Chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.clickable {
                        chartType = if (chartType == PortfolioChartType.BAR) PortfolioChartType.LINE else PortfolioChartType.BAR
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (chartType == PortfolioChartType.BAR) Icons.Rounded.Analytics else Icons.AutoMirrored.Rounded.TrendingUp,
                            contentDescription = "Typ grafu",
                            tint = DarkPill,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (chartType == PortfolioChartType.BAR) "Stĺpce" else "Čiara",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = DarkPill
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Value & Time Range Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayValue.toCurrencyString(currency),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = TextPrimary
                    )
                )

                // Time Range Selector Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    PortfolioTimeRange.entries.forEach { range ->
                        val isSelected = range == selectedTimeRange
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) DarkPill else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { onTimeRangeSelected(range) }
                        ) {
                            Text(
                                text = range.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else TextSecondary
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Changes Row (Daily P&L + Selected Range P&L)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Daily Change Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDayPositive) PositiveGreenBg else NegativeRedBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isDayPositive) Icons.AutoMirrored.Rounded.TrendingUp else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = if (isDayPositive) PositiveGreen else NegativeRed,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Dnes: ${dayChangeAmount.toSignedCurrencyString(currency)} (${dayChangePct.toPercentageString()})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isDayPositive) PositiveGreen else NegativeRed
                            )
                        )
                    }
                }

                // Range Change Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isRangePositive) PositiveGreenBg else NegativeRedBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${selectedTimeRange.label}: ${rangeChangeAmount.toSignedCurrencyString(currency)} (${rangeChangePercentage.toPercentageString()})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isRangePositive) PositiveGreen else NegativeRed
                            )
                        )
                    }
                }
            }

            if (activePoint != null) {
                Spacer(modifier = Modifier.height(4.dp))

                // Active Point Date Label below "Dnes" / Changes row
                Text(
                    text = activePoint!!.dateLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Canvas
            val chartColor = if (isRangePositive) PositiveGreen else NegativeRed
            val indicatorColor = DarkPill.copy(alpha = 0.4f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (points.size >= 2) {
                    val minVal = points.minOf { it.value }
                    val maxVal = points.maxOf { it.value }
                    val range = (maxVal - minVal).coerceAtLeast(0.01)

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(points, chartType) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val idx = if (chartType == PortfolioChartType.BAR) {
                                            val slotWidth = size.width / points.size
                                            (offset.x / slotWidth).toInt().coerceIn(0, points.size - 1)
                                        } else {
                                            val xRatio = (offset.x / size.width).coerceIn(0f, 1f)
                                            (xRatio * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                                        }
                                        activePoint = points[idx]
                                    },
                                    onDrag = { change, _ ->
                                        val idx = if (chartType == PortfolioChartType.BAR) {
                                            val slotWidth = size.width / points.size
                                            (change.position.x / slotWidth).toInt().coerceIn(0, points.size - 1)
                                        } else {
                                            val xRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                                            (xRatio * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                                        }
                                        activePoint = points[idx]
                                    },
                                    onDragEnd = { activePoint = null },
                                    onDragCancel = { activePoint = null }
                                )
                            }
                            .pointerInput(points, chartType) {
                                detectTapGestures { offset ->
                                    val idx = if (chartType == PortfolioChartType.BAR) {
                                        val slotWidth = size.width / points.size
                                        (offset.x / slotWidth).toInt().coerceIn(0, points.size - 1)
                                    } else {
                                        val xRatio = (offset.x / size.width).coerceIn(0f, 1f)
                                        (xRatio * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                                    }
                                    activePoint = points[idx]
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height

                        if (chartType == PortfolioChartType.BAR) {
                            val totalPoints = points.size
                            val slotWidth = w / totalPoints
                            val rawBarWidth = slotWidth * 0.68f
                            val barWidth = rawBarWidth.coerceIn(3.dp.toPx(), 22.dp.toPx())

                            points.forEachIndexed { idx, pt ->
                                val normY = ((pt.value - minVal) / range).toFloat()
                                val yTop = h - (normY * (h - 25.dp.toPx())) - 10.dp.toPx()
                                val yBottom = h - 4.dp.toPx()

                                val centerX = (idx + 0.5f) * slotWidth
                                val barLeft = centerX - barWidth / 2f

                                val isBarPositive = if (idx > 0) pt.value >= points[idx - 1].value else isRangePositive
                                val baseColor = if (isBarPositive) PositiveGreen else NegativeRed

                                val isSelected = (activeIdx == idx)
                                val barColor = when {
                                    activePoint == null -> baseColor
                                    isSelected -> baseColor
                                    else -> baseColor.copy(alpha = 0.30f)
                                }

                                val cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                val barHeight = (yBottom - yTop).coerceAtLeast(4.dp.toPx())

                                drawRoundRect(
                                    color = barColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(barLeft, yTop),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                    cornerRadius = cornerRadius
                                )

                                if (isSelected) {
                                    // Vertical dashed line
                                    drawLine(
                                        color = indicatorColor,
                                        start = androidx.compose.ui.geometry.Offset(centerX, 0f),
                                        end = androidx.compose.ui.geometry.Offset(centerX, h),
                                        strokeWidth = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )

                                    // Top dot highlight
                                    drawCircle(
                                        color = Color.White,
                                        radius = 6.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(centerX, yTop)
                                    )
                                    drawCircle(
                                        color = baseColor,
                                        radius = 4.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(centerX, yTop)
                                    )
                                }
                            }
                        } else {
                            val stepX = w / (points.size - 1)

                            val path = Path()
                            val fillPath = Path()

                            points.forEachIndexed { idx, pt ->
                                val x = idx * stepX
                                val normY = ((pt.value - minVal) / range).toFloat()
                                val y = h - (normY * (h - 20.dp.toPx())) - 10.dp.toPx()

                                if (idx == 0) {
                                    path.moveTo(x, y)
                                    fillPath.moveTo(x, h)
                                    fillPath.lineTo(x, y)
                                } else {
                                    val prevX = (idx - 1) * stepX
                                    val prevPt = points[idx - 1]
                                    val prevNormY = ((prevPt.value - minVal) / range).toFloat()
                                    val prevY = h - (prevNormY * (h - 20.dp.toPx())) - 10.dp.toPx()

                                    val ctrlX1 = prevX + (stepX / 2f)
                                    val ctrlY1 = prevY
                                    val ctrlX2 = prevX + (stepX / 2f)
                                    val ctrlY2 = y

                                    path.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, x, y)
                                    fillPath.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, x, y)
                                }

                                if (idx == points.size - 1) {
                                    fillPath.lineTo(x, h)
                                    fillPath.close()
                                }
                            }

                            // Draw gradient fill under curve
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        chartColor.copy(alpha = 0.25f),
                                        chartColor.copy(alpha = 0.01f)
                                    )
                                )
                            )

                            // Draw smooth line
                            drawPath(
                                path = path,
                                color = chartColor,
                                style = Stroke(width = 2.5.dp.toPx())
                            )

                            // If point is touched/hovered, draw vertical indicator and dot
                            activePoint?.let { active ->
                                val activeIdx = points.indexOf(active)
                                if (activeIdx >= 0) {
                                    val activeX = activeIdx * stepX
                                    val normY = ((active.value - minVal) / range).toFloat()
                                    val activeY = h - (normY * (h - 20.dp.toPx())) - 10.dp.toPx()

                                    // Vertical dashed line
                                    drawLine(
                                        color = indicatorColor,
                                        start = androidx.compose.ui.geometry.Offset(activeX, 0f),
                                        end = androidx.compose.ui.geometry.Offset(activeX, h),
                                        strokeWidth = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )

                                    // Outer white ring
                                    drawCircle(
                                        color = Color.White,
                                        radius = 7.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(activeX, activeY)
                                    )
                                    // Inner dot
                                    drawCircle(
                                        color = chartColor,
                                        radius = 4.5.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(activeX, activeY)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Date Range Labels below chart
            if (points.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = points.first().dateLabel,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = points.last().dateLabel,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
            }

            if (positions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CardBorderColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                TopDailyPnlMoversSection(
                    positions = positions,
                    currency = currency,
                    onPositionClick = onPositionClick
                )
            }
        }
    }
}

// --- Top Daily P&L Movers Section ---

fun Double.toCompactSignedCurrencyString(currencyCodeOrSymbol: String = "EUR"): String {
    val raw = currencyCodeOrSymbol.trim()
    val symbol = raw.toCurrencySymbol()
    val sign = if (this > 0) "+" else if (this < 0) "-" else ""
    val absVal = kotlin.math.abs(this)
    val isSuffix = raw.uppercase() in listOf("CZK", "PLN", "HUF")

    val formattedVal = when {
        absVal >= 1_000_000 -> String.format(Locale.US, "%.1fM", absVal / 1_000_000.0)
        absVal >= 100_000 -> String.format(Locale.US, "%.0fk", absVal / 1_000.0)
        absVal >= 1_000 -> String.format(Locale.US, "%.1fk", absVal / 1_000.0)
        absVal >= 100 -> String.format(Locale.US, "%.0f", absVal)
        else -> String.format(Locale.US, "%.2f", absVal)
    }

    return if (isSuffix) {
        "$sign$formattedVal ${symbol.trim()}"
    } else {
        "$sign$symbol$formattedVal"
    }
}

private fun Position.calculateDailyPnlPct(): Double {
    val prevValue = marketValue - dailyPnl
    return if (prevValue > 0) {
        (dailyPnl / prevValue) * 100.0
    } else if (marketValue > 0) {
        (dailyPnl / marketValue) * 100.0
    } else {
        0.0
    }
}

@Composable
fun TopDailyPnlMoversSection(
    positions: List<Position>,
    currency: String = "EUR",
    onPositionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isPercentMode by rememberSaveable { mutableStateOf(true) }

    val mergedPositions = remember(positions) {
        positions.groupBy { it.effectiveTicker.uppercase() }.map { (effectiveSymbol, posList) ->
            if (posList.size == 1) {
                val single = posList.first()
                if (single.alternativeTicker.isNullOrBlank()) {
                    single
                } else {
                    single.copy(symbol = single.effectiveTicker)
                }
            } else {
                val first = posList.first()
                val totalDailyPnl = posList.sumOf { it.dailyPnl }
                val totalMarketValue = posList.sumOf { it.marketValue }
                val totalQty = posList.sumOf { it.quantity }
                val totalCostBasis = posList.sumOf { it.costBasisPrice * it.quantity }
                val totalPnl = posList.sumOf { it.totalPnl }
                val costPrice = if (totalQty > 0.0) totalCostBasis / totalQty else first.costBasisPrice
                val markPrice = if (totalQty > 0.0) totalMarketValue / totalQty else first.markPrice
                val pnlPct = if (totalCostBasis > 0.0) (totalPnl / totalCostBasis) * 100.0 else 0.0

                first.copy(
                    symbol = effectiveSymbol,
                    quantity = totalQty,
                    costBasisPrice = costPrice,
                    markPrice = markPrice,
                    marketValue = totalMarketValue,
                    dailyPnl = totalDailyPnl,
                    totalPnl = totalPnl,
                    pnlPercentage = pnlPct,
                    trades = posList.flatMap { it.trades }
                )
            }
        }
    }

    val positivePositions = remember(mergedPositions, isPercentMode) {
        mergedPositions
            .filter { it.dailyPnl > 0.0 }
            .sortedByDescending { if (isPercentMode) it.calculateDailyPnlPct() else it.dailyPnl }
            .take(5)
    }

    val negativePositions = remember(mergedPositions, isPercentMode) {
        mergedPositions
            .filter { it.dailyPnl < 0.0 }
            .sortedBy { if (isPercentMode) it.calculateDailyPnlPct() else it.dailyPnl }
            .take(5)
    }

    if (positivePositions.isEmpty() && negativePositions.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isPercentMode) "Top denné pohyby podľa %" else "Top denné pohyby podľa P&L",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Surface(
                onClick = { isPercentMode = !isPercentMode },
                shape = RoundedCornerShape(6.dp),
                color = DarkPill
            ) {
                Text(
                    text = if (isPercentMode) "%" else "€",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }

        // Top Gainers
        if (positivePositions.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PositiveGreen)
                )
                Text(
                    text = "Najväčšie denné zisky",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                )
            }

            val maxPositiveVal = positivePositions.maxOfOrNull {
                if (isPercentMode) it.calculateDailyPnlPct() else it.dailyPnl
            } ?: 1.0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                positivePositions.forEach { position ->
                    val pnlPct = position.calculateDailyPnlPct()
                    val posVal = if (isPercentMode) pnlPct else position.dailyPnl
                    val heightFraction = (posVal / maxPositiveVal).toFloat().coerceIn(0.18f, 1f)
                    DailyPnlBarColumn(
                        symbol = position.symbol,
                        amountText = position.dailyPnl.toCompactSignedCurrencyString(currency),
                        percentageText = pnlPct.toPercentageString(),
                        heightFraction = heightFraction,
                        isPositive = true,
                        onClick = { onPositionClick(position.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(5 - positivePositions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Top Losers
        if (negativePositions.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NegativeRed)
                )
                Text(
                    text = "Najväčšie denné straty",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                )
            }

            val maxNegativeAbsVal = negativePositions.maxOfOrNull {
                kotlin.math.abs(if (isPercentMode) it.calculateDailyPnlPct() else it.dailyPnl)
            } ?: 1.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                negativePositions.forEach { position ->
                    val pnlPct = position.calculateDailyPnlPct()
                    val posVal = kotlin.math.abs(if (isPercentMode) pnlPct else position.dailyPnl)
                    val heightFraction = (posVal / maxNegativeAbsVal).toFloat().coerceIn(0.18f, 1f)
                    DailyPnlBarColumn(
                        symbol = position.symbol,
                        amountText = position.dailyPnl.toCompactSignedCurrencyString(currency),
                        percentageText = pnlPct.toPercentageString(),
                        heightFraction = heightFraction,
                        isPositive = false,
                        onClick = { onPositionClick(position.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(5 - negativePositions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DailyPnlBarColumn(
    symbol: String,
    amountText: String,
    percentageText: String,
    heightFraction: Float,
    isPositive: Boolean,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val barColor = if (isPositive) PositiveGreen else NegativeRed
    val barHeight = 44.dp * heightFraction

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        // Amount text above bar
        Text(
            text = amountText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = barColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Percentage text above bar
        Text(
            text = percentageText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = barColor.copy(alpha = 0.85f)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Bar container
        Box(
            modifier = Modifier
                .height(48.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                    .background(barColor)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Symbol ticker text below bar
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- Expandable Portfolio Statistics Card ---

@Composable
fun ExpandableBalancesCard(
    portfolioData: com.example.portfoliomanager.data.model.PortfolioData?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val positions = portfolioData?.positions ?: emptyList()
    val balances = portfolioData?.balances
    val accountSummary = portfolioData?.accountSummary

    val totalPnl = accountSummary?.totalPnl ?: positions.sumOf { it.totalPnl }
    val totalPnlPct = accountSummary?.totalPnlPercentage ?: 0.0
    val unrealizedPnl = balances?.unrealizedPnl ?: positions.sumOf { it.totalPnl }
    val realizedPnl = balances?.realizedPnl ?: 0.0
    val currency = accountSummary?.currency ?: "EUR"

    val totalPositionsCount = positions.size
    val profitableCount = positions.count { it.totalPnl >= 0 }
    val lossCount = positions.count { it.totalPnl < 0 }

    val bestPerformer = positions.maxByOrNull { it.pnlPercentage }
    val worstPerformer = positions.minByOrNull { it.pnlPercentage }
    val topHolding = positions.maxByOrNull { it.marketValue }
    val totalMarketValue = positions.sumOf { it.marketValue.coerceAtLeast(0.0) }.coerceAtLeast(1.0)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Štatistiky portfólia",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    if (!isExpanded && (realizedPnl != 0.0 || totalPnl != 0.0)) {
                        Text(
                            text = "Realizovaný P&L: ${realizedPnl.toSignedCurrencyString(currency)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (realizedPnl >= 0) PositiveGreen else NegativeRed
                            )
                        )
                    }
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Rozbaliť štatistiky",
                        tint = TextSecondary
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = CardBorderColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    BalanceMetricRow(
                        label = "Celkový zisk / strata",
                        value = "${totalPnl.toSignedCurrencyString(currency)} (${totalPnlPct.toPercentageString()})",
                        valueColor = if (totalPnl >= 0) PositiveGreen else NegativeRed,
                        isBold = true
                    )

                    BalanceMetricRow(
                        label = "Nerealizovaný P&L",
                        value = unrealizedPnl.toSignedCurrencyString(currency),
                        valueColor = if (unrealizedPnl >= 0) PositiveGreen else NegativeRed
                    )

                    BalanceMetricRow(
                        label = "Realizovaný P&L",
                        value = realizedPnl.toSignedCurrencyString(currency),
                        valueColor = if (realizedPnl >= 0) PositiveGreen else NegativeRed
                    )

                    BalanceMetricRow(
                        label = "Počet pozícií",
                        value = "$totalPositionsCount ($profitableCount ziskových, $lossCount stratových)"
                    )

                    if (topHolding != null) {
                        val topHoldingPct = (topHolding.marketValue / totalMarketValue) * 100.0
                        BalanceMetricRow(
                            label = "Najväčšia pozícia",
                            value = "${topHolding.symbol} (${String.format(Locale.US, "%.1f", topHoldingPct)}% portfólia)"
                        )
                    }

                    if (bestPerformer != null) {
                        BalanceMetricRow(
                            label = "Najlepší výkon",
                            value = "${bestPerformer.symbol} (${bestPerformer.pnlPercentage.toPercentageString()})",
                            valueColor = PositiveGreen
                        )
                    }

                    if (worstPerformer != null && positions.size > 1) {
                        BalanceMetricRow(
                            label = "Najhorší výkon",
                            value = "${worstPerformer.symbol} (${worstPerformer.pnlPercentage.toPercentageString()})",
                            valueColor = if (worstPerformer.pnlPercentage >= 0) PositiveGreen else NegativeRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceMetricRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = TextSecondary
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
                color = valueColor
            )
        )
    }
}

// --- Expandable Margin Card ---

@Composable
fun ExpandableMarginCard(
    marginRequirements: MarginRequirements?,
    isExpanded: Boolean,
    selectedMarginTab: MarginTab,
    onToggleExpand: () -> Unit,
    onMarginTabSelected: (MarginTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Margin požiadavky",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Rozbaliť margin",
                        tint = TextSecondary
                    )
                }
            }

            val usedMargin = marginRequirements?.usedMargin ?: 0.0
            val availableMargin = marginRequirements?.availableMargin ?: 1.0
            val totalCap = (usedMargin + availableMargin).coerceAtLeast(1.0)
            val progress = (usedMargin / totalCap).toFloat().coerceIn(0f, 1f)

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Využité: ${usedMargin.toCurrencyString()}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
                Text(
                    text = "K dispozícii: ${availableMargin.toCurrencyString()}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = DarkPill,
                trackColor = UnselectedPillBg
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = CardBorderColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MarginTab.entries.forEach { tab ->
                            val isSelected = tab == selectedMarginTab
                            Surface(
                                onClick = { onMarginTabSelected(tab) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) DarkPill else UnselectedPillBg,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tab.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else UnselectedPillText
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    BalanceMetricRow(
                        label = "Počiatočný margin",
                        value = (marginRequirements?.initialMarginReq ?: 0.0).toCurrencyString()
                    )
                    BalanceMetricRow(
                        label = "Udržiavací margin",
                        value = (marginRequirements?.maintMarginReq ?: 0.0).toCurrencyString()
                    )
                    BalanceMetricRow(
                        label = "Voľné prostriedky",
                        value = (marginRequirements?.availableMargin ?: 0.0).toCurrencyString()
                    )
                    BalanceMetricRow(
                        label = "Nadbytočná likvidita",
                        value = (marginRequirements?.excessLiquidity ?: 0.0).toCurrencyString(),
                        isBold = true
                    )
                    BalanceMetricRow(
                        label = "Pomer marginu",
                        value = "${marginRequirements?.marginRatioPercentage ?: 0.0}%"
                    )
                }
            }
        }
    }
}

// --- Analytics Breakdown Card (Sector / Geography / Category) ---

@Composable
fun BreakdownAnalyticsCard(
    selectedBreakdownTab: BreakdownTab,
    sectorAllocations: List<SectorAllocation>,
    geographyAllocations: List<GeographyAllocation>,
    positions: List<Position>,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    onBreakdownTabSelected: (BreakdownTab) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rozdelenie: ${selectedBreakdownTab.displayName}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Rozbaliť rozdelenie",
                        tint = TextSecondary
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = CardBorderColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BreakdownTab.entries.forEach { tab ->
                            val isSelected = tab == selectedBreakdownTab
                            Surface(
                                onClick = { onBreakdownTabSelected(tab) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) DarkPill else UnselectedPillBg,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tab.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else UnselectedPillText
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (selectedBreakdownTab) {
                        BreakdownTab.INDUSTRY -> {
                            sectorAllocations.forEach { item ->
                                BreakdownProgressItem(
                                    title = item.sector,
                                    percentage = item.percentage,
                                    valueStr = item.value.toCurrencyString(),
                                    barColor = parseColorHex(item.colorHex)
                                )
                            }
                        }
                        BreakdownTab.GEOGRAPHY -> {
                            geographyAllocations.forEachIndexed { index, item ->
                                val colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6))
                                BreakdownProgressItem(
                                    title = item.country,
                                    percentage = item.percentage,
                                    valueStr = item.value.toCurrencyString(),
                                    barColor = colors[index % colors.size]
                                )
                            }
                        }
                        BreakdownTab.CATEGORY -> {
                            val categoryBreakdown = rememberCategoryBreakdown(positions)
                            categoryBreakdown.forEach { item ->
                                BreakdownProgressItem(
                                    title = item.title,
                                    percentage = item.percentage,
                                    valueStr = item.valueStr,
                                    barColor = item.barColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownProgressItem(
    title: String,
    percentage: Double,
    valueStr: String,
    barColor: Color
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(barColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            }
            Row {
                Text(
                    text = "$valueStr ",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Text(
                    text = "(${percentage}%)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        val progress = (percentage / 100.0).toFloat().coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = barColor,
            trackColor = UnselectedPillBg
        )
    }
}

private data class CategoryBreakdownItem(
    val title: String,
    val percentage: Double,
    val valueStr: String,
    val barColor: Color
)

@Composable
private fun rememberCategoryBreakdown(positions: List<Position>): List<CategoryBreakdownItem> {
    if (positions.isEmpty()) return emptyList()

    val totalMarketValue = positions.sumOf { it.marketValue.coerceAtLeast(0.0) }
    if (totalMarketValue <= 0) return emptyList()

    val grouped = positions.groupBy { it.assetCategory }
    val colors = listOf(Color(0xFF2563EB), Color(0xFFD97706), Color(0xFF059669), Color(0xFF7C3AED), Color(0xFFDC2626))

    return grouped.entries.mapIndexed { index, entry ->
        val catName = entry.key.name
        val sumVal = entry.value.sumOf { it.marketValue.coerceAtLeast(0.0) }
        val pct = kotlin.math.round((sumVal / totalMarketValue) * 10000.0) / 100.0
        CategoryBreakdownItem(
            title = catName,
            percentage = pct,
            valueStr = sumVal.toCurrencyString(),
            barColor = colors[index % colors.size]
        )
    }.sortedByDescending { it.percentage }
}

private fun parseColorHex(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val colorInt = cleaned.toLong(16) or 0xFF000000L
        Color(colorInt)
    } catch (_: Exception) {
        Color(0xFF3B82F6)
    }
}

// --- Your Assets Holdings Section ---

@Composable
fun YourAssetsHeader(
    positionsCount: Int,
    searchQuery: String,
    selectedCategoryFilter: CategoryFilter,
    onSearchQueryChanged: (String) -> Unit,
    onCategoryFilterSelected: (CategoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchVisible by remember { mutableStateOf(true) }
    var isSearchFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Moje aktíva",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Surface(
                    shape = CircleShape,
                    color = DarkPill
                ) {
                    Text(
                        text = positionsCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(
                onClick = { isSearchVisible = !isSearchVisible },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CardBackground)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = "Filter vyhľadávania",
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(
            visible = isSearchVisible,
            modifier = Modifier.fillMaxWidth(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = if (!isSearchFocused && searchQuery.isEmpty()) {
                    {
                        Text(
                            text = "Hľadať podľa symbolu, názvu alebo sektora...",
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else null,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Vyhľadávanie",
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Vymazať",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = InputFieldBg,
                    unfocusedContainerColor = InputFieldBg,
                    focusedBorderColor = ActiveLimePill,
                    unfocusedBorderColor = CardBorderColor,
                    cursorColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .onFocusChanged { isSearchFocused = it.isFocused }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CategoryFilter.entries.toTypedArray()) { category ->
                val isSelected = category == selectedCategoryFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategoryFilterSelected(category) },
                    label = {
                        Text(
                            text = category.displayName,
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00D2C8),
                        selectedLabelColor = Color(0xFF0F172A),
                        containerColor = Color.White,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = CardBorderColor,
                        selectedBorderColor = Color(0xFF00D2C8),
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp,
                        enabled = true,
                        selected = isSelected
                    ),
                    modifier = Modifier.wrapContentWidth()
                )
            }
        }
    }
}

@Composable
fun YourAssetsEmptyState(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Text(
            text = "Žiadne aktíva nezodpovedajú vyhľadávaniu alebo filtru.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
        )
    }
}

@Composable
fun YourAssetsSection(
    positions: List<Position>,
    searchQuery: String,
    selectedCategoryFilter: CategoryFilter,
    expandedPositionIds: Set<String>,
    onSearchQueryChanged: (String) -> Unit,
    onCategoryFilterSelected: (CategoryFilter) -> Unit,
    onTogglePositionExpanded: (String) -> Unit,
    onAddTradeClick: (Position) -> Unit = {},
    onEditTickerClick: (Position) -> Unit = {},
    onEditTradeClick: (Position, Trade) -> Unit = { _, _ -> },
    onDeleteTradeClick: (Position, Trade) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        YourAssetsHeader(
            positionsCount = positions.size,
            searchQuery = searchQuery,
            selectedCategoryFilter = selectedCategoryFilter,
            onSearchQueryChanged = onSearchQueryChanged,
            onCategoryFilterSelected = onCategoryFilterSelected
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (positions.isEmpty()) {
            YourAssetsEmptyState()
        } else {
            positions.forEach { position ->
                val isExpanded = expandedPositionIds.contains(position.id)
                PositionCard(
                    position = position,
                    isExpanded = isExpanded,
                    onToggleExpand = { onTogglePositionExpanded(position.id) },
                    onAddTradeClick = { onAddTradeClick(position) },
                    onEditTickerClick = { onEditTickerClick(position) },
                    onEditTradeClick = { trade -> onEditTradeClick(position, trade) },
                    onDeleteTradeClick = { trade -> onDeleteTradeClick(position, trade) }
                )
            }
        }
    }
}

// --- Single Asset / Position Card ---

@Composable
fun PositionCard(
    position: Position,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddTradeClick: () -> Unit = {},
    onEditTickerClick: () -> Unit = {},
    onEditTradeClick: (Trade) -> Unit = {},
    onDeleteTradeClick: (Trade) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPositive = position.dailyPnl >= 0
    val categoryLabel = when (position.assetCategory) {
        com.example.portfoliomanager.data.model.AssetCategory.STK -> "AKCIE"
        com.example.portfoliomanager.data.model.AssetCategory.CRYPTO -> "KRYPTO"
        com.example.portfoliomanager.data.model.AssetCategory.OPT -> "OPCIE"
        com.example.portfoliomanager.data.model.AssetCategory.ETF -> "ETF"
        com.example.portfoliomanager.data.model.AssetCategory.BOND -> "DLHOPISY"
        com.example.portfoliomanager.data.model.AssetCategory.CASH -> "HOTOVOSŤ"
        else -> "OSTATNÉ"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = parseAssetCategoryColor(position.assetCategory.name),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = position.symbol.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = position.symbol,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = categoryLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (!position.alternativeTicker.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        text = "Alt: ${position.alternativeTicker}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB)
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = position.description,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (position.sparklinePoints.isNotEmpty()) {
                        SparklineCanvas(
                            points = position.sparklinePoints,
                            lineColor = if (isPositive) PositiveGreen else NegativeRed,
                            modifier = Modifier
                                .width(50.dp)
                                .height(28.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Rozbaliť pozíciu",
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Trhová hodnota",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = position.marketValue.toCurrencyString("EUR"),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Column {
                    Text(
                        text = "Denný P&L",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = position.dailyPnl.toSignedCurrencyString("EUR"),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) PositiveGreen else NegativeRed
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Celkový P&L",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = "${position.totalPnl.toSignedCurrencyString("EUR")} (${position.pnlPercentage.toPercentageString()})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (position.totalPnl >= 0) PositiveGreen else NegativeRed
                        )
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = CardBorderColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricDetailItem("Množstvo", String.format(Locale.US, "%,.2f ks", position.quantity))
                        MetricDetailItem("Nákupná cena", position.costBasisPrice.toCurrencyString(position.currency))
                        MetricDetailItem("Trhová cena", position.markPrice.toCurrencyString(position.currency))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricDetailItem("Sektor", position.sector.ifBlank { "N/A" })
                        MetricDetailItem("Krajina", position.country.ifBlank { "N/A" })
                        MetricDetailItem(
                            "Mena / Kurz",
                            if (position.currency == "EUR") "EUR (1.0)" else "${position.currency} (${String.format(Locale.US, "%.4f", position.fxRateToPortfolio)} €)"
                        )
                    }

                    // --- Trades / Purchase History Section ---
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = CardBorderColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    var isTradesExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isTradesExpanded = !isTradesExpanded }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isTradesExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Toggle Trade History",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Transakcie (${position.trades.size})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onEditTickerClick,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, Color(0xFF64748B))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Upraviť ticker",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (position.alternativeTicker.isNullOrBlank()) "Ticker" else position.alternativeTicker,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                )
                            }

                            OutlinedButton(
                                onClick = onAddTradeClick,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, Color(0xFF2563EB))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Pridať nákup",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Pridať nákup",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isTradesExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (position.trades.isEmpty()) {
                                Text(
                                    text = "Zatiaľ neboli zaevidované žiadne transakcie.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                position.trades.forEach { trade ->
                                    val isBuy = trade.buySell.uppercase() == "BUY"
                                    val tradeCurr = trade.currency.ifBlank { position.currency }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = InputFieldBg,
                                        border = BorderStroke(1.dp, CardBorderColor),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (isBuy) PositiveGreenBg else NegativeRedBg
                                                    ) {
                                                        Text(
                                                            text = if (isBuy) "NÁKUP" else "PREDAJ",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isBuy) PositiveGreen else NegativeRed
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }

                                                    Text(
                                                        text = trade.date,
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = TextPrimary
                                                        )
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(2.dp))

                                                Text(
                                                    text = "${String.format(Locale.US, "%,.2f", trade.quantity)} ks @ ${trade.price.toCurrencyString(tradeCurr)}" +
                                                            if (trade.commission > 0) " (poplatok: ${trade.commission.toCurrencyString(tradeCurr)})" else "",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = TextSecondary,
                                                        fontSize = 12.sp
                                                    )
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = trade.totalAmount.toCurrencyString(tradeCurr),
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                )

                                                IconButton(
                                                    onClick = { onEditTradeClick(trade) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Edit,
                                                        contentDescription = "Upraviť transakciu",
                                                        tint = Color(0xFF64748B),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { onDeleteTradeClick(trade) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Delete,
                                                        contentDescription = "Odstrániť transakciu",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricDetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )
    }
}

private fun parseAssetCategoryColor(category: String): Color {
    return when (category.uppercase(Locale.US)) {
        "STK", "STOCKS" -> Color(0xFF2563EB)
        "CRYPTO" -> Color(0xFFD97706)
        "OPT", "OPTIONS" -> Color(0xFF7C3AED)
        "ETF", "ETFS" -> Color(0xFF059669)
        "BOND", "BONDS" -> Color(0xFF0891B2)
        "CASH" -> Color(0xFF475569)
        else -> Color(0xFF6B7280)
    }
}

// --- IBKR Settings & Sync Modal Dialog ---

@Composable
fun IbkrSettingsDialog(
    isOpen: Boolean,
    isImportingCsv: Boolean = false,
    importSuccessMessage: String? = null,
    errorMessage: String? = null,
    onImportCsvClick: () -> Unit = {},
    onClearImportedData: () -> Unit = {},
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Import Portfólia (CSV / XLSX)",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Vyberte .csv alebo .xlsx výpis vygenerovaný z IBKR (Flex Query), XTB (Excel .xlsx) alebo Trading 212. Aplikácia automaticky rozozná formát a naimportuje vaše pozície a zostatky.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )

                Button(
                    onClick = onImportCsvClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isImportingCsv
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileUpload,
                        contentDescription = "Import súboru",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isImportingCsv) "Spracovávam súbor..." else "Vybrať súbor (CSV / XLSX)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                importSuccessMessage?.let { msg ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = PositiveGreen,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        TextButton(
                            onClick = onClearImportedData,
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = "Obnoviť vzorové dáta",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFDC2626))
                            )
                        }
                    }
                }

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zatvoriť", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBackground
    )
}

// --- Live Sync & Error Banners ---

@Composable
fun SyncingBanner(
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = "Synchronizácia",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Synchronizácia trhových dát s Yahoo Finance...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E40AF)
                    )
                )
            }
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2563EB),
                trackColor = Color(0xFFDBEAFE)
            )
        }
    }
}

@Composable
fun ErrorBanner(
    errorMessage: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Upozornenie",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Chyba synchronizácie s Yahoo Finance",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFB91C1C)
                        )
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Obnoviť",
                        tint = Color(0xFF991B1B)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Zatvoriť",
                        tint = Color(0xFF991B1B)
                    )
                }
            }
        }
    }
}

// --- Bottom Navigation Bar ---

@Composable
fun PortfolioBottomNavBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = DarkPill,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val backgroundColor = if (isSelected) ActiveLimePill else Color.Transparent
                val contentColor = if (isSelected) ActiveLimePillText else Color.White.copy(alpha = 0.7f)

                Surface(
                    onClick = { onTabSelected(tab) },
                    shape = RoundedCornerShape(22.dp),
                    color = backgroundColor,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                BottomNavTab.HOME -> Icons.Rounded.Home
                                BottomNavTab.ASSETS -> Icons.AutoMirrored.Rounded.List
                            },
                            contentDescription = tab.displayName,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tab.displayName,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = contentColor
                            )
                        )
                    }
                }
            }
        }
    }
}

// --- Manual Purchase / Add Trade Dialog ---

@Composable
fun AddTradeDialog(
    position: Position?,
    onDismiss: () -> Unit,
    onSaveTrade: (quantity: Double, price: Double, date: String, commission: Double) -> Unit
) {
    if (position == null) return

    val defaultDate = remember {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        java.time.LocalDate.now().format(formatter)
    }

    var quantityInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf(if (position.markPrice > 0) String.format(Locale.US, "%.2f", position.markPrice) else "") }
    var dateInput by remember { mutableStateOf(defaultDate) }
    var commissionInput by remember { mutableStateOf("0.0") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(dateInput) {
                try {
                    java.time.LocalDate.parse(dateInput, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneOffset.UTC)
                                .toLocalDate()
                            dateInput = selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            errorMessage = null
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Zrušiť", style = MaterialTheme.typography.labelLarge)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Pridať nákup",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${position.symbol} - ${position.description}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF991B1B)),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = {
                        quantityInput = it
                        errorMessage = null
                    },
                    label = { Text("Množstvo / Počet kusov") },
                    placeholder = { Text("napr. 10.0") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = InputFieldBg,
                        unfocusedContainerColor = InputFieldBg,
                        focusedBorderColor = ActiveLimePill,
                        unfocusedBorderColor = CardBorderColor,
                        cursorColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceInput,
                    onValueChange = {
                        priceInput = it
                        errorMessage = null
                    },
                    label = { Text("Nákupná cena za kus (${position.currency})") },
                    placeholder = { Text("napr. 150.00") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = InputFieldBg,
                        unfocusedContainerColor = InputFieldBg,
                        focusedBorderColor = ActiveLimePill,
                        unfocusedBorderColor = CardBorderColor,
                        cursorColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dátum nákupu") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.DateRange,
                                    contentDescription = "Vybrať dátum z kalendára",
                                    tint = TextPrimary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = InputFieldBg,
                            unfocusedContainerColor = InputFieldBg,
                            focusedBorderColor = ActiveLimePill,
                            unfocusedBorderColor = CardBorderColor,
                            cursorColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }

                OutlinedTextField(
                    value = commissionInput,
                    onValueChange = {
                        commissionInput = it
                        errorMessage = null
                    },
                    label = { Text("Poplatok za nákup (voliteľné)") },
                    placeholder = { Text("0.0") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = InputFieldBg,
                        unfocusedContainerColor = InputFieldBg,
                        focusedBorderColor = ActiveLimePill,
                        unfocusedBorderColor = CardBorderColor,
                        cursorColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityInput.replace(',', '.').toDoubleOrNull()
                    val price = priceInput.replace(',', '.').toDoubleOrNull()
                    val comm = commissionInput.replace(',', '.').toDoubleOrNull() ?: 0.0

                    if (qty == null || qty <= 0) {
                        errorMessage = "Zadajte platné kladné množstvo."
                        return@Button
                    }
                    if (price == null || price <= 0) {
                        errorMessage = "Zadajte platnú kladnú nákupnú cenu."
                        return@Button
                    }
                    if (dateInput.isBlank()) {
                        errorMessage = "Zadajte dátum nákupu."
                        return@Button
                    }

                    onSaveTrade(qty, price, dateInput.trim(), comm)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Uložiť nákup", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušiť", style = MaterialTheme.typography.labelLarge)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBackground
    )
}

// --- Edit Alternative Ticker Dialog ---

@Composable
fun EditTickerDialog(
    position: Position?,
    onDismiss: () -> Unit,
    onSave: (originalSymbol: String, alternativeTicker: String) -> Unit
) {
    if (position == null) return

    var tickerInput by remember(position) {
        mutableStateOf(position.alternativeTicker ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Alternatívny ticker pre ${position.symbol}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Zadajte alternatívny ticker akcie pre Yahoo Finance (napr. MPW.US alebo MPW). " +
                            "Tento ticker sa použije na stiahnutie trhových cien, výpočet denného P&L a trhovej hodnoty namiesto pôvodného symbolu z importu (${position.symbol}).",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )

                OutlinedTextField(
                    value = tickerInput,
                    onValueChange = { tickerInput = it.uppercase() },
                    label = { Text("Alternatívny ticker") },
                    placeholder = { Text("napr. MPW.US") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = InputFieldBg,
                        unfocusedContainerColor = InputFieldBg,
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = CardBorderColor,
                        cursorColor = TextPrimary
                    )
                )

                if (!position.alternativeTicker.isNullOrBlank()) {
                    Text(
                        text = "Aktuálne nastavený alternatívny ticker: ${position.alternativeTicker}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF2563EB))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(position.symbol, tickerInput.trim())
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Uložiť", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!position.alternativeTicker.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            onSave(position.symbol, "")
                        }
                    ) {
                        Text("Obnoviť pôvodný", color = NegativeRed, style = MaterialTheme.typography.labelLarge)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Zrušiť", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBackground
    )
}

// --- Edit Trade Dialog ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTradeDialog(
    position: Position?,
    trade: Trade?,
    onDismiss: () -> Unit,
    onSaveTrade: (quantity: Double, price: Double, date: String, commission: Double, buySell: String) -> Unit
) {
    if (position == null || trade == null) return

    var buySellState by remember(trade) { mutableStateOf(trade.buySell.uppercase()) }
    var quantityInput by remember(trade) { mutableStateOf(String.format(Locale.US, "%.2f", trade.quantity).replace(',', '.')) }
    var priceInput by remember(trade) { mutableStateOf(String.format(Locale.US, "%.2f", trade.price).replace(',', '.')) }
    var dateInput by remember(trade) { mutableStateOf(trade.date) }
    var commissionInput by remember(trade) { mutableStateOf(String.format(Locale.US, "%.2f", trade.commission).replace(',', '.')) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(dateInput) {
                try {
                    java.time.LocalDate.parse(dateInput, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneOffset.UTC)
                                .toLocalDate()
                            dateInput = selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            errorMessage = null
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Zrušiť", style = MaterialTheme.typography.labelLarge)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Upraviť transakciu",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${position.symbol} - ${position.description}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF991B1B)),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = buySellState == "BUY",
                        onClick = { buySellState = "BUY" },
                        label = { Text("NÁKUP", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PositiveGreenBg,
                            selectedLabelColor = PositiveGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = buySellState == "SELL",
                        onClick = { buySellState = "SELL" },
                        label = { Text("PREDAJ", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NegativeRedBg,
                            selectedLabelColor = NegativeRed
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = {
                        quantityInput = it
                        errorMessage = null
                    },
                    label = { Text("Množstvo / Počet kusov") },
                    placeholder = { Text("napr. 10.0") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = InputFieldBg,
                        unfocusedContainerColor = InputFieldBg,
                        focusedBorderColor = ActiveLimePill,
                        unfocusedBorderColor = CardBorderColor,
                        cursorColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceInput,
                    onValueChange = {
                        priceInput = it
                        errorMessage = null
                    },
                    label = { Text("Cena za kus (${position.currency})") },
                    placeholder = { Text("napr. 150.00") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = InputFieldBg,
                        unfocusedContainerColor = InputFieldBg,
                        focusedBorderColor = ActiveLimePill,
                        unfocusedBorderColor = CardBorderColor,
                        cursorColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dátum transakcie") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.DateRange,
                                    contentDescription = "Vybrať dátum z kalendára",
                                    tint = TextPrimary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = InputFieldBg,
                            unfocusedContainerColor = InputFieldBg,
                            focusedBorderColor = ActiveLimePill,
                            unfocusedBorderColor = CardBorderColor,
                            cursorColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }

                OutlinedTextField(
                    value = commissionInput,
                    onValueChange = {
                        commissionInput = it
                        errorMessage = null
                    },
                    label = { Text("Poplatok (voliteľné)") },
                    placeholder = { Text("0.0") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = InputFieldBg,
                        unfocusedContainerColor = InputFieldBg,
                        focusedBorderColor = ActiveLimePill,
                        unfocusedBorderColor = CardBorderColor,
                        cursorColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityInput.replace(',', '.').toDoubleOrNull()
                    val prc = priceInput.replace(',', '.').toDoubleOrNull()
                    val comm = commissionInput.replace(',', '.').toDoubleOrNull() ?: 0.0

                    if (qty == null || qty <= 0) {
                        errorMessage = "Zadajte platné množstvo väčšie ako 0."
                        return@Button
                    }
                    if (prc == null || prc < 0) {
                        errorMessage = "Zadajte platnú cenu."
                        return@Button
                    }
                    if (dateInput.isBlank()) {
                        errorMessage = "Zadajte dátum transakcie."
                        return@Button
                    }

                    onSaveTrade(qty, prc, dateInput.trim(), comm, buySellState)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Uložiť zmeny", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušiť", style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary))
            }
        }
    )
}

// --- Delete Trade Confirmation Dialog ---

@Composable
fun DeleteTradeDialog(
    position: Position?,
    trade: Trade?,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    if (position == null || trade == null) return

    val tradeTypeLabel = if (trade.buySell.uppercase() == "BUY") "Nákup" else "Predaj"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Odstrániť transakciu?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = "Naozaj chcete odstrániť túto transakciu (${tradeTypeLabel} ${String.format(Locale.US, "%,.2f", trade.quantity)} ks ze dňa ${trade.date}) pre ${position.symbol}?",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Text("Odstrániť", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušiť", style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary))
            }
        }
    )
}

