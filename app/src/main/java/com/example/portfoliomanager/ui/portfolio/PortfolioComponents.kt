package com.example.portfoliomanager.ui.portfolio

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
import androidx.compose.material.icons.rounded.Dashboard
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.portfoliomanager.data.model.Position
import com.example.portfoliomanager.data.model.SectorAllocation
import com.example.portfoliomanager.ui.theme.ActiveLimePill
import com.example.portfoliomanager.ui.theme.ActiveLimePillText
import com.example.portfoliomanager.ui.theme.CardBackground
import com.example.portfoliomanager.ui.theme.CardBorderColor
import com.example.portfoliomanager.ui.theme.DarkPill
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

// --- Format Utilities ---

fun Double.toCurrencyString(symbol: String = "$"): String {
    return String.format(Locale.US, "%s%,.2f", symbol, this)
}

fun Double.toPercentageString(includeSign: Boolean = true): String {
    val sign = if (includeSign && this > 0) "+" else ""
    return String.format(Locale.US, "%s%.2f%%", sign, this)
}

fun Double.toSignedCurrencyString(symbol: String = "$"): String {
    val sign = if (this > 0) "+" else if (this < 0) "-" else ""
    return String.format(Locale.US, "%s%s%,.2f", sign, symbol, kotlin.math.abs(this))
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
                            contentDescription = "Import CSV",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Import CSV",
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
    positionsCount: Int,
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
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Text(
                        text = "$positionsCount Positions",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = DarkPill
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Net Liquidation Value",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            )

            val netLiquidation = accountSummary?.netLiquidation ?: 0.0
            val dailyPnl = accountSummary?.dailyPnl ?: 0.0
            val dailyPnlPct = accountSummary?.dailyPnlPercentage ?: 0.0
            val isPositive = dailyPnl >= 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = netLiquidation.toCurrencyString(),
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
                                text = "${dailyPnl.toSignedCurrencyString()} (${dailyPnlPct.toPercentageString()})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) PositiveGreen else NegativeRed
                                )
                            )
                        }
                    }
                }

                val sampleSparkline = listOf(1230000.0, 1235000.0, 1232000.0, 1240000.0, 1238000.0, 1245890.50)
                SparklineCanvas(
                    points = sampleSparkline,
                    lineColor = if (isPositive) PositiveGreen else NegativeRed,
                    modifier = Modifier
                        .width(90.dp)
                        .height(36.dp)
                )
            }
        }
    }
}

@Composable
private fun rememberTimeStr(): String {
    val sdf = remember { SimpleDateFormat("hh:mm:ss a", Locale.US) }
    return sdf.format(Date())
}

// --- Expandable Balances Card ---

@Composable
fun ExpandableBalancesCard(
    balances: Balances?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
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
                    text = "Balances",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Expand Balances",
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

                    BalanceMetricRow(label = "Total Cash", value = (balances?.totalCash ?: 0.0).toCurrencyString())
                    BalanceMetricRow(label = "Settled Cash", value = (balances?.settledCash ?: 0.0).toCurrencyString())
                    BalanceMetricRow(label = "Accrued Cash", value = (balances?.accruedCash ?: 0.0).toCurrencyString())

                    val unrealized = balances?.unrealizedPnl ?: 0.0
                    BalanceMetricRow(
                        label = "Unrealized P&L",
                        value = unrealized.toSignedCurrencyString(),
                        valueColor = if (unrealized >= 0) PositiveGreen else NegativeRed
                    )

                    val realized = balances?.realizedPnl ?: 0.0
                    BalanceMetricRow(
                        label = "Realized P&L",
                        value = realized.toSignedCurrencyString(),
                        valueColor = if (realized >= 0) PositiveGreen else NegativeRed
                    )

                    BalanceMetricRow(
                        label = "Net Liquidation",
                        value = (balances?.netLiquidation ?: 0.0).toCurrencyString(),
                        isBold = true
                    )
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
                    text = "Margin Requirements",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Expand Margin",
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
                    text = "Used: ${usedMargin.toCurrencyString()}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
                Text(
                    text = "Available: ${availableMargin.toCurrencyString()}",
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
                        label = "Initial Margin Req",
                        value = (marginRequirements?.initialMarginReq ?: 0.0).toCurrencyString()
                    )
                    BalanceMetricRow(
                        label = "Maintenance Margin Req",
                        value = (marginRequirements?.maintMarginReq ?: 0.0).toCurrencyString()
                    )
                    BalanceMetricRow(
                        label = "Available Funds",
                        value = (marginRequirements?.availableMargin ?: 0.0).toCurrencyString()
                    )
                    BalanceMetricRow(
                        label = "Excess Liquidity",
                        value = (marginRequirements?.excessLiquidity ?: 0.0).toCurrencyString(),
                        isBold = true
                    )
                    BalanceMetricRow(
                        label = "Margin Ratio",
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
            Text(
                text = "${selectedBreakdownTab.displayName} Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

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
fun YourAssetsSection(
    positions: List<Position>,
    searchQuery: String,
    selectedCategoryFilter: CategoryFilter,
    expandedPositionIds: Set<String>,
    onSearchQueryChanged: (String) -> Unit,
    onCategoryFilterSelected: (CategoryFilter) -> Unit,
    onTogglePositionExpanded: (String) -> Unit,
    onAddTradeClick: (Position) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isSearchVisible by remember { mutableStateOf(true) }

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
                    text = "Your Assets",
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
                        text = "${positions.size}",
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
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = "Search Filter",
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(
            visible = isSearchVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search by symbol, name, or sector...", color = TextSecondary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = DarkPill,
                    unfocusedBorderColor = CardBorderColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
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

        Spacer(modifier = Modifier.height(8.dp))

        if (positions.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                Text(
                    text = "No assets match your search or filter criteria.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }
        } else {
            positions.forEach { position ->
                val isExpanded = expandedPositionIds.contains(position.id)
                PositionCard(
                    position = position,
                    isExpanded = isExpanded,
                    onToggleExpand = { onTogglePositionExpanded(position.id) },
                    onAddTradeClick = { onAddTradeClick(position) }
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
    modifier: Modifier = Modifier
) {
    val isPositive = position.dailyPnl >= 0

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
                                    text = position.assetCategory.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
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
                            contentDescription = "Expand Position",
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
                        text = "Market Value",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = position.marketValue.toCurrencyString(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Column {
                    Text(
                        text = "Daily P&L",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = position.dailyPnl.toSignedCurrencyString(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) PositiveGreen else NegativeRed
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total P&L",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = "${position.totalPnl.toSignedCurrencyString()} (${position.pnlPercentage.toPercentageString()})",
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
                        MetricDetailItem("Quantity", String.format(Locale.US, "%,.2f", position.quantity))
                        MetricDetailItem("Cost Basis", position.costBasisPrice.toCurrencyString())
                        MetricDetailItem("AVG Price", position.markPrice.toCurrencyString())
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricDetailItem("Sector", position.sector.ifBlank { "N/A" })
                        MetricDetailItem("Country", position.country.ifBlank { "N/A" })
                        MetricDetailItem("Daily Change %", position.pnlPercentage.toPercentageString())
                    }

                    // --- Trades / Purchase History Section ---
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = CardBorderColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "História nákupov (${position.trades.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

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

                    Spacer(modifier = Modifier.height(8.dp))

                    if (position.trades.isEmpty()) {
                        Text(
                            text = "Zatiaľ neboli zaevidované žiadne nákupy.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            position.trades.forEach { trade ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                                    color = if (trade.buySell.uppercase() == "BUY") Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                                ) {
                                                    Text(
                                                        text = if (trade.buySell.uppercase() == "BUY") "NÁKUP" else "PREDAJ",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (trade.buySell.uppercase() == "BUY") Color(0xFF166534) else Color(0xFF991B1B)
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
                                                text = "${String.format(Locale.US, "%,.2f", trade.quantity)} ks @ ${trade.price.toCurrencyString()}" +
                                                        if (trade.commission > 0) " (poplatok: ${trade.commission.toCurrencyString()})" else "",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }

                                        Text(
                                            text = trade.totalAmount.toCurrencyString(),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
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
        containerColor = Color.White
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
                    contentDescription = "Syncing",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Syncing live IBKR portfolio data...",
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
                    contentDescription = "Warning",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "IBKR Live Sync Error",
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
                        contentDescription = "Retry",
                        tint = Color(0xFF991B1B)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceInput,
                    onValueChange = {
                        priceInput = it
                        errorMessage = null
                    },
                    label = { Text("Nákupná cena za kus") },
                    placeholder = { Text("napr. 150.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateInput,
                    onValueChange = {
                        dateInput = it
                        errorMessage = null
                    },
                    label = { Text("Dátum nákupu (YYYY-MM-DD)") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = commissionInput,
                    onValueChange = {
                        commissionInput = it
                        errorMessage = null
                    },
                    label = { Text("Poplatok za nákup (voliteľné)") },
                    placeholder = { Text("0.0") },
                    singleLine = true,
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
        }
    )
}
