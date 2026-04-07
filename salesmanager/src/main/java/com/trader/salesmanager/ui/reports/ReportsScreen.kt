package com.trader.salesmanager.ui.reports

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.salesmanager.ui.components.AnimatedCounter
import com.trader.salesmanager.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReportsScreen(
    onNavigateUp: () -> Unit,
    viewModel: ReportsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Violet500, Cyan500)))
                .padding(top = 48.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text("التقارير", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("تحليل أداء المبيعات", color = Color.White.copy(0.7f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Period switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.15f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(ReportPeriod.DAY to "اليوم", ReportPeriod.WEEK to "الأسبوع", ReportPeriod.MONTH to "الشهر")
                        .forEach { (p, label) ->
                            val sel = state.period == p
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) Color.White else Color.Transparent)
                                    .clickable { viewModel.setPeriod(p) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(label,
                                    color = if (sel) Violet500 else Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                }
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(300.dp), Alignment.Center) {
                CircularProgressIndicator(color = Violet500)
            }
            return@Column
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Summary Cards ────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard(Modifier.weight(1f), "إجمالي المبيعات", state.totalSales, Violet500, Icons.Rounded.TrendingUp)
                SummaryCard(Modifier.weight(1f), "عدد العمليات", state.transactionCount.toDouble(), Cyan500, Icons.Rounded.Receipt, isInt = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard(Modifier.weight(1f), "مدفوع", state.totalPaid, PaidGreen, Icons.Rounded.CheckCircle)
                SummaryCard(Modifier.weight(1f), "غير مدفوع", state.totalUnpaid, UnpaidAmber, Icons.Rounded.PendingActions)
            }

            // ── Line Chart ───────────────────────────────────────────
            if (state.dailyStats.isNotEmpty()) {
                SectionCard(title = "منحنى المبيعات", icon = Icons.Rounded.ShowChart) {
                    LineChartView(stats = state.dailyStats, modifier = Modifier.fillMaxWidth().height(160.dp))
                }
            }

            // ── Paid vs Unpaid Bar ───────────────────────────────────
            if (state.totalSales > 0) {
                SectionCard(title = "المدفوع مقابل غير المدفوع", icon = Icons.Rounded.BarChart) {
                    PaidVsUnpaidBar(paid = state.totalPaid, unpaid = state.totalUnpaid)
                }
            }

            // ── Donut — Payment Methods ──────────────────────────────
            if (state.paymentStats.isNotEmpty()) {
                SectionCard(title = "طرق الدفع", icon = Icons.Rounded.PieChart) {
                    DonutChart(stats = state.paymentStats)
                }
            }

            // ── Top Customers ────────────────────────────────────────
            if (state.topCustomers.isNotEmpty()) {
                SectionCard(title = "أبرز الزبائن", icon = Icons.Rounded.Star) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.topCustomers.forEachIndexed { i, c ->
                            TopCustomerRow(rank = i + 1, customer = c, maxTotal = state.topCustomers.first().total)
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ── Summary Card ─────────────────────────────────────────────────────
@Composable
private fun SummaryCard(
    modifier: Modifier, label: String, value: Double, color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector, isInt: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(if (visible) 1f else 0.85f,
        spring(Spring.DampingRatioMediumBouncy), label = "scale")

    Card(
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                Box(Modifier.size(28.dp).clip(CircleShape).background(color.copy(0.15f)),
                    Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            if (isInt) {
                val animVal by animateIntAsState(value.toInt(), spring(Spring.DampingRatioMediumBouncy), label = "int")
                Text("$animVal", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = color)
            } else {
                AnimatedCounter(value = value, style = MaterialTheme.typography.headlineMedium, color = color)
            }
        }
    }
}

// ── Section Card Wrapper ─────────────────────────────────────────────
@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = Violet500, modifier = Modifier.size(18.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

// ── Line Chart ────────────────────────────────────────────────────────
@Composable
private fun LineChartView(stats: List<DailyStat>, modifier: Modifier = Modifier) {
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "line"
    )
    val maxVal = stats.maxOf { it.total }.coerceAtLeast(1.0)
    val gradientBrush = Brush.verticalGradient(listOf(Violet500.copy(0.4f), Color.Transparent))
    val lineBrush = Brush.horizontalGradient(listOf(Violet500, Cyan500))

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height - 20.dp.toPx()
        val step = if (stats.size > 1) w / (stats.size - 1) else w

        val points = stats.mapIndexed { i, s ->
            Offset(i * step, h - (s.total / maxVal * h * animProgress).toFloat())
        }

        // Fill under line
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, h)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, h)
                close()
            }
            drawPath(path, gradientBrush)
        }

        // Line
        if (points.size > 1) {
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]; val curr = points[i]
                    val cpX = (prev.x + curr.x) / 2
                    cubicTo(cpX, prev.y, cpX, curr.y, curr.x, curr.y)
                }
            }
            drawPath(linePath, lineBrush, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }

        // Dots
        points.forEach { pt ->
            drawCircle(Color.White, radius = 5.dp.toPx(), center = pt)
            drawCircle(Violet500, radius = 3.dp.toPx(), center = pt)
        }
    }
}

// ── Paid vs Unpaid Bar ────────────────────────────────────────────────
@Composable
private fun PaidVsUnpaidBar(paid: Double, unpaid: Double) {
    val total = (paid + unpaid).coerceAtLeast(1.0)
    val paidFraction = (paid / total).toFloat()
    val animPaid by animateFloatAsState(paidFraction, tween(900, easing = EaseOutCubic), label = "bar")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(10.dp))) {
            Box(Modifier.weight(animPaid.coerceAtLeast(0.01f)).fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(PaidGreen, Emerald400))))
            Box(Modifier.weight((1f - animPaid).coerceAtLeast(0.01f)).fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(UnpaidAmber, Color(0xFFFBBF24)))))
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(PaidGreen))
                Text("مدفوع ${(paidFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall, color = PaidGreen)
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(UnpaidAmber))
                Text("غير مدفوع ${((1 - paidFraction) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall, color = UnpaidAmber)
            }
        }
    }
}

// ── Donut Chart ───────────────────────────────────────────────────────
@Composable
private fun DonutChart(stats: List<PaymentStat>) {
    val total = stats.sumOf { it.amount }.coerceAtLeast(1.0)
    val animProgress by animateFloatAsState(1f, tween(1000, easing = EaseOutCubic), label = "donut")

    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Canvas(modifier = Modifier.size(120.dp)) {
            var startAngle = -90f
            stats.forEach { stat ->
                val sweep = (stat.amount / total * 360f * animProgress).toFloat()
                drawArc(
                    color = Color(stat.color),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt),
                    topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
                    size = Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx())
                )
                startAngle += sweep + 2f
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stats.forEach { stat ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(stat.color)))
                    Text(stat.name, style = MaterialTheme.typography.bodySmall)
                    Text(String.format("%.0f", stat.amount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Top Customer Row ──────────────────────────────────────────────────
@Composable
private fun TopCustomerRow(rank: Int, customer: TopCustomer, maxTotal: Double) {
    val animWidth by animateFloatAsState(
        (customer.total / maxTotal.coerceAtLeast(1.0)).toFloat(),
        tween(800, delayMillis = rank * 100, easing = EaseOutCubic), label = "bar$rank"
    )
    val medalColors = listOf(Color(0xFFFFD700), Color(0xFFC0C0C0), Color(0xFFCD7F32))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(28.dp).clip(CircleShape)
                .background(if (rank <= 3) medalColors[rank - 1].copy(0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant),
                Alignment.Center) {
                Text("$rank", fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    color = if (rank <= 3) medalColors[rank - 1]
                            else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(customer.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text(String.format("%.0f", customer.total),
                        style = MaterialTheme.typography.labelSmall, color = Violet500, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(Modifier.fillMaxWidth(animWidth).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Violet500, Cyan500)))
                        .clip(RoundedCornerShape(3.dp)))
                }
                if (customer.debt > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text("دين: ${String.format("%.0f", customer.debt)}",
                        style = MaterialTheme.typography.labelSmall, color = DebtRed)
                }
            }
        }
    }
}
