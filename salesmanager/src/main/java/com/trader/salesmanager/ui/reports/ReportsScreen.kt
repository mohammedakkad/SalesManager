package com.trader.salesmanager.ui.reports

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.salesmanager.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateUp: () -> Unit,
    viewModel: ReportsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقارير", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Period Switcher ──────────────────────────────────
            item {
                PeriodSwitcher(selected = uiState.period, onSelect = viewModel::setPeriod)
            }

            // ── Summary Cards ────────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard(label = "الإجمالي", value = uiState.totalAmount, color = Emerald500, modifier = Modifier.weight(1f))
                    SummaryCard(label = "المدفوع", value = uiState.paidAmount, color = PaidGreen, modifier = Modifier.weight(1f))
                    SummaryCard(label = "الديون", value = uiState.unpaidAmount, color = DebtRed, modifier = Modifier.weight(1f))
                }
            }

            // ── Line Chart ───────────────────────────────────────
            if (uiState.dailySales.isNotEmpty()) {
                item {
                    ChartCard(title = "منحنى المبيعات اليومية") {
                        LineChart(data = uiState.dailySales, modifier = Modifier.fillMaxWidth().height(180.dp))
                    }
                }
            }

            // ── Bar Chart ────────────────────────────────────────
            if (uiState.dailySales.isNotEmpty()) {
                item {
                    ChartCard(title = "مدفوع مقابل غير مدفوع") {
                        BarChart(data = uiState.dailySales, modifier = Modifier.fillMaxWidth().height(160.dp))
                    }
                }
            }

            // ── Donut Chart ──────────────────────────────────────
            if (uiState.paymentShares.isNotEmpty()) {
                item {
                    ChartCard(title = "توزيع طرق الدفع") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DonutChart(
                                data = uiState.paymentShares,
                                modifier = Modifier.size(140.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val donutColors = listOf(Emerald500, Color(0xFF3B82F6), Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6))
                                uiState.paymentShares.forEachIndexed { i, share ->
                                    val total = uiState.paymentShares.sumOf { it.amount }.takeIf { it > 0 } ?: 1.0
                                    val pct = (share.amount / total * 100).toInt()
                                    LegendItem(label = share.name, percent = pct, color = donutColors[i % donutColors.size])
                                }
                            }
                        }
                    }
                }
            }

            // ── Top Spenders ─────────────────────────────────────
            if (uiState.topSpenders.isNotEmpty()) {
                item {
                    ChartCard(title = "أعلى 5 زبائن شراءً") {
                        RankList(items = uiState.topSpenders, color = Emerald500)
                    }
                }
            }

            // ── Top Debtors ──────────────────────────────────────
            if (uiState.topDebtors.isNotEmpty()) {
                item {
                    ChartCard(title = "أعلى 5 زبائن ديناً") {
                        RankList(items = uiState.topDebtors, color = DebtRed)
                    }
                }
            }
        }
    }
}

// ── Period Switcher ──────────────────────────────────────────
@Composable
private fun PeriodSwitcher(selected: ReportPeriod, onSelect: (ReportPeriod) -> Unit) {
    val labels = mapOf(ReportPeriod.TODAY to "اليوم", ReportPeriod.WEEK to "الأسبوع", ReportPeriod.MONTH to "الشهر")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        labels.forEach { (period, label) ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Emerald500 else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = { onSelect(period) }, modifier = Modifier.fillMaxWidth()) {
                    Text(label, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

// ── Summary Card with count-up animation ────────────────────
@Composable
private fun SummaryCard(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(value) { target = value.toFloat() }
    val animated by animateFloatAsState(target, tween(1200, easing = FastOutSlowInEasing), label = "count")
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.1f))) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Spacer(Modifier.height(4.dp))
            Text(
                String.format("%.0f", animated),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

// ── Chart Card wrapper ───────────────────────────────────────
@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ── Line Chart ───────────────────────────────────────────────
@Composable
private fun LineChart(data: List<DaySalesEntry>, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
    }
    val anim by progress.asState()
    val lineColor = Emerald500
    val fillColor = Emerald500.copy(0.15f)

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val maxVal = data.maxOf { it.total }.takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (data.size - 1).coerceAtLeast(1)
        val points = data.mapIndexed { i, e ->
            Offset(i * stepX, size.height * (1f - (e.total / maxVal).toFloat()))
        }
        val drawCount = (points.size * anim).toInt().coerceAtLeast(1).coerceAtMost(points.size)
        val visiblePoints = points.take(drawCount)

        // Fill path
        val path = Path()
        visiblePoints.forEachIndexed { i, pt ->
            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        path.lineTo(visiblePoints.last().x, size.height)
        path.lineTo(visiblePoints.first().x, size.height)
        path.close()
        drawPath(path, Brush.verticalGradient(listOf(fillColor, Color.Transparent)))

        // Line
        val linePath = Path()
        visiblePoints.forEachIndexed { i, pt ->
            if (i == 0) linePath.moveTo(pt.x, pt.y) else linePath.lineTo(pt.x, pt.y)
        }
        drawPath(linePath, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

        // Dots
        visiblePoints.forEach { pt ->
            drawCircle(Color.White, 5.dp.toPx(), pt)
            drawCircle(lineColor, 3.dp.toPx(), pt)
        }
    }
}

// ── Bar Chart ─────────────────────────────────────────────────
@Composable
private fun BarChart(data: List<DaySalesEntry>, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }
    val anim by progress.asState()
    val paidColor = PaidGreen
    val unpaidColor = DebtRed

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val maxVal = data.maxOf { it.total }.takeIf { it > 0 } ?: 1.0
        val groupW = size.width / data.size
        val barW = groupW * 0.35f
        val gap = barW * 0.2f

        data.forEachIndexed { i, entry ->
            val left = i * groupW + gap
            val paidH = (entry.paid / maxVal * size.height * anim).toFloat()
            val unpaidH = ((entry.total - entry.paid) / maxVal * size.height * anim).toFloat()
            // Paid bar
            drawRoundRect(paidColor, topLeft = Offset(left, size.height - paidH),
                size = Size(barW, paidH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
            // Unpaid bar
            drawRoundRect(unpaidColor.copy(0.7f), topLeft = Offset(left + barW + 2.dp.toPx(), size.height - unpaidH),
                size = Size(barW, unpaidH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
        }
    }
}

// ── Donut Chart ───────────────────────────────────────────────
@Composable
private fun DonutChart(data: List<PaymentShare>, modifier: Modifier = Modifier) {
    val donutColors = listOf(Emerald500, Color(0xFF3B82F6), Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6))
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1400, easing = FastOutSlowInEasing))
    }
    val anim by progress.asState()
    val total = data.sumOf { it.amount }.takeIf { it > 0 } ?: 1.0

    Canvas(modifier = modifier) {
        val stroke = 28.dp.toPx()
        val radius = min(size.width, size.height) / 2f - stroke / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        var startAngle = -90f
        data.forEachIndexed { i, share ->
            val sweep = (share.amount / total * 360 * anim).toFloat()
            drawArc(
                color = donutColors[i % donutColors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            startAngle += sweep
        }
    }
}

// ── Legend Item ───────────────────────────────────────────────
@Composable
private fun LegendItem(label: String, percent: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text("$label ($percent%)", style = MaterialTheme.typography.labelSmall)
    }
}

// ── Rank List ─────────────────────────────────────────────────
@Composable
private fun RankList(items: List<CustomerRank>, color: Color) {
    val maxVal = items.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, item ->
            val barProgress = remember { Animatable(0f) }
            LaunchedEffect(item.amount) {
                barProgress.snapTo(0f)
                barProgress.animateTo((item.amount / maxVal).toFloat(), tween(900, delayMillis = i * 100, easing = FastOutSlowInEasing))
            }
            val prog by barProgress.asState()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${i + 1}", style = MaterialTheme.typography.labelSmall,
                    color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(item.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text(String.format("%.0f", item.amount), style = MaterialTheme.typography.labelSmall, color = color)
                    }
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            .background(color.copy(0.15f))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(prog).fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp)).background(color))
                    }
                }
            }
        }
    }
}
