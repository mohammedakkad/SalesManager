package com.trader.salesmanager.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.layout.fullWidth
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.DefaultPointConnector
import com.patrykandpatrick.vico.core.chart.layout.HorizontalLayout
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.trader.core.domain.model.DailySales
import com.trader.core.domain.model.TopDebtorCustomer
import com.trader.core.domain.model.TopSellingProduct
import com.trader.salesmanager.ui.components.AnimatedCounter
import com.trader.salesmanager.ui.theme.Cyan500
import com.trader.salesmanager.ui.theme.DebtRed
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.UnpaidAmber
import com.trader.salesmanager.ui.theme.Violet500
import com.trader.salesmanager.ui.theme.appColors
import java.util.Calendar
import java.util.Locale

@Composable
fun AnalyticsDashboardSection(
    state: DashboardUiState,
    onCustomerClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var animationsStarted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationsStarted = true
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DashboardEntrance(animationsStarted, 0) {
            TopIndicatorsRow(
                todaySales = state.todaySales,
                todayInvoiceCount = state.todayInvoiceCount,
                totalOutstandingDebt = state.totalOutstandingDebt
            )
        }
        DashboardEntrance(animationsStarted, 80) {
            SalesChartCard(state.lastSevenDaysSales)
        }
        DashboardEntrance(animationsStarted, 160) {
            TopProductsCard(state.topSellingProducts)
        }
        DashboardEntrance(animationsStarted, 240) {
            TopDebtorsCard(
                customers = state.topDebtorCustomers,
                onCustomerClick = onCustomerClick
            )
        }
    }
}

@Composable
private fun DashboardEntrance(
    visible: Boolean,
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 300, delayMillis = delayMillis),
            initialOffsetY = { it / 2 }
        ) + fadeIn(tween(durationMillis = 300, delayMillis = delayMillis))
    ) {
        content()
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TopIndicatorsRow(
    todaySales: Double,
    todayInvoiceCount: Int,
    totalOutstandingDebt: Double,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cardWidth = if (maxWidth >= 360.dp) {
            (maxWidth - 16.dp) / 3
        } else {
            (maxWidth - 8.dp) / 2
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IndicatorCard(
                modifier = Modifier.width(cardWidth),
                label = "مبيعات اليوم",
                value = todaySales,
                icon = Icons.Rounded.TrendingUp,
                color = Emerald500,
                prefix = "₪ "
            )
            IndicatorCard(
                modifier = Modifier.width(cardWidth),
                label = "عدد الفواتير اليوم",
                value = todayInvoiceCount.toDouble(),
                icon = Icons.Rounded.ReceiptLong,
                color = Violet500
            )
            IndicatorCard(
                modifier = Modifier.width(cardWidth),
                label = "إجمالي الديون",
                value = totalOutstandingDebt,
                icon = Icons.Rounded.People,
                color = DebtRed,
                prefix = "₪ "
            )
        }
    }
}

@Composable
private fun IndicatorCard(
    label: String,
    value: Double,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    prefix: String = ""
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            AnimatedCounter(
                value = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                decimals = 0,
                prefix = prefix,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = appColors.textSecondary,
                minLines = 2
            )
        }
    }
}

@Composable
fun TopProductsCard(
    products: List<TopSellingProduct>,
    modifier: Modifier = Modifier
) {
    val maxQuantity by remember(products) {
        derivedStateOf { products.maxOfOrNull { it.quantitySold }?.coerceAtLeast(1.0) ?: 1.0 }
    }
    DashboardCard(modifier) {
        SectionHeader(
            title = "الأصناف الأكثر مبيعاً",
            subtitle = "أفضل 5 أصناف هذا الشهر",
            icon = Icons.Rounded.Inventory2,
            color = Cyan500
        )
        Spacer(Modifier.height(16.dp))
        if (products.isEmpty()) {
            RankingEmptyState(
                icon = Icons.Rounded.Inventory2,
                message = "لا توجد مبيعات أصناف هذا الشهر بعد"
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                products.forEachIndexed { index, product ->
                    ProductRankingRow(
                        rank = index + 1,
                        product = product,
                        progress = (product.quantitySold / maxQuantity).toFloat()
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductRankingRow(
    rank: Int,
    product: TopSellingProduct,
    progress: Float
) {
    RankingRow(
        rank = rank,
        title = product.productName,
        value = "${formatQuantity(product.quantitySold)} مباع",
        valueColor = Cyan500,
        progress = progress,
        progressColor = Cyan500
    )
}

@Composable
fun TopDebtorsCard(
    customers: List<TopDebtorCustomer>,
    onCustomerClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxDebt by remember(customers) {
        derivedStateOf { customers.maxOfOrNull { it.totalDebt }?.coerceAtLeast(1.0) ?: 1.0 }
    }
    DashboardCard(modifier) {
        SectionHeader(
            title = "أعلى الزبائن مديونية",
            subtitle = "أكبر 5 أرصدة غير مدفوعة",
            icon = Icons.Rounded.People,
            color = UnpaidAmber
        )
        Spacer(Modifier.height(16.dp))
        if (customers.isEmpty()) {
            RankingEmptyState(
                icon = Icons.Rounded.People,
                message = "لا توجد ديون مستحقة حالياً"
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                customers.forEachIndexed { index, customer ->
                    RankingRow(
                        rank = index + 1,
                        title = customer.customerName,
                        value = "₪ ${formatAmount(customer.totalDebt)}",
                        valueColor = DebtRed,
                        progress = (customer.totalDebt / maxDebt).toFloat(),
                        progressColor = DebtRed,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onCustomerClick(customer.customerId) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RankingRow(
    rank: Int,
    title: String,
    value: String,
    valueColor: Color,
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(progressColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = progressColor
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = appColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    color = valueColor
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(appColors.border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(progressColor)
                )
            }
        }
    }
}

@Composable
fun SalesChartCard(
    sales: List<DailySales>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { ChartEntryModelProducer() }
    val labels = remember(sales) { sales.map { arabicDayName(it.dayStartMillis) } }
    val entries = remember(sales) {
        sales.mapIndexed { index, day -> entryOf(index, day.totalSales) }
    }
    LaunchedEffect(entries) {
        modelProducer.setEntriesSuspending(entries)
    }

    DashboardCard(modifier) {
        SectionHeader(
            title = "المبيعات خلال 7 أيام",
            subtitle = "إجمالي المبيعات اليومية",
            icon = Icons.Rounded.ShowChart,
            color = Emerald500
        )
        Spacer(Modifier.height(12.dp))
        val chartStyle = m3ChartStyle(
            axisLabelColor = appColors.textSecondary,
            axisGuidelineColor = appColors.divider,
            axisLineColor = appColors.border,
            entityColors = listOf(Emerald500, Cyan500),
            elevationOverlayColor = Emerald500
        )
        val lineBackground = remember {
            DynamicShaders.fromBrush(
                Brush.verticalGradient(
                    listOf(
                        Emerald500.copy(alpha = 0.28f),
                        Cyan500.copy(alpha = 0.04f)
                    )
                )
            )
        }
        val salesLine = remember(lineBackground) {
            lineSpec(
                lineColor = Emerald500,
                lineThickness = 3.dp,
                lineBackgroundShader = lineBackground,
                pointConnector = DefaultPointConnector(cubicStrength = 0.35f)
            )
        }
        val bottomFormatter = remember(labels) {
            AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                labels.getOrElse(value.toInt()) { "" }
            }
        }
        val startFormatter = remember {
            AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
                compactAmount(value)
            }
        }

        ProvideChartStyle(chartStyle) {
            Chart(
                chart = lineChart(lines = listOf(salesLine), spacing = 28.dp),
                chartModelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                startAxis = rememberStartAxis(
                    valueFormatter = startFormatter,
                    tickLength = 0.dp
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = bottomFormatter,
                    guideline = null,
                    tickLength = 0.dp
                ),
                chartScrollSpec = rememberChartScrollSpec(isScrollEnabled = false),
                isZoomEnabled = false,
                runInitialAnimation = true,
                horizontalLayout = HorizontalLayout.fullWidth(
                    scalableStartPadding = 8.dp,
                    scalableEndPadding = 8.dp
                )
            )
        }
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(21.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = appColors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = appColors.textSecondary
            )
        }
    }
}

@Composable
private fun RankingEmptyState(
    icon: ImageVector,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = appColors.textSubtle,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = appColors.textSecondary
        )
    }
}

private fun arabicDayName(timeMillis: Long): String {
    val day = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        .get(Calendar.DAY_OF_WEEK)
    return when (day) {
        Calendar.SATURDAY -> "السبت"
        Calendar.SUNDAY -> "الأحد"
        Calendar.MONDAY -> "الاثنين"
        Calendar.TUESDAY -> "الثلاثاء"
        Calendar.WEDNESDAY -> "الأربعاء"
        Calendar.THURSDAY -> "الخميس"
        else -> "الجمعة"
    }
}

private fun formatAmount(value: Double): String =
    String.format(Locale.US, "%,.0f", value)

private fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%,.0f", value)
    } else {
        String.format(Locale.US, "%,.1f", value)
    }

private fun compactAmount(value: Float): String =
    when {
        value >= 1_000_000f -> String.format(Locale.US, "%.1f م", value / 1_000_000f)
        value >= 1_000f -> String.format(Locale.US, "%.0f ألف", value / 1_000f)
        else -> String.format(Locale.US, "%.0f", value)
    }
