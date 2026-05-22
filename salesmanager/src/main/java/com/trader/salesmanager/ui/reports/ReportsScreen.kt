package com.trader.salesmanager.ui.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.core.data.local.appDataStore
import com.trader.core.domain.model.DebtAging
import com.trader.core.domain.model.FeatureFlags
import com.trader.core.domain.model.Transaction
import com.trader.salesmanager.ui.components.PremiumLockChip
import com.trader.salesmanager.ui.theme.Cyan500
import com.trader.salesmanager.ui.theme.DebtRed
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.PaidGreen
import com.trader.salesmanager.ui.theme.UnpaidAmber
import com.trader.salesmanager.ui.theme.Violet500
import com.trader.salesmanager.util.export.ExportManager
import com.trader.salesmanager.util.export.ExportState
import com.trader.salesmanager.util.export.ExportSuccessBottomSheet
import com.trader.salesmanager.util.export.ExportViewModel
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateUp: () -> Unit,
    onViewDayTransactions: (Long) -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    viewModel: ReportsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val flags by FeatureFlags.flow.collectAsStateWithLifecycle(initialValue = FeatureFlags.current)
    val context = LocalContext.current
    val storeName by context.appDataStore.data
        .map {
            it[com.trader.salesmanager.ui.settings.STORE_NAME_KEY] ?: ""
        }
        .collectAsState(initial = "")

    val exportVm: ExportViewModel = koinViewModel()
    val exportState by exportVm.state.collectAsStateWithLifecycle()
    var showExportSheet by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(exportState) {
        if (exportState is ExportState.Success) showExportSheet = true
    }

    if (showExportSheet && exportState is ExportState.Success) {
        val success = exportState as ExportState.Success
        val file = java.io.File(success.filePath)
        ExportSuccessBottomSheet(
            state = success,
            onShare = {
                ExportManager.shareFile(context, file, success.type.mimeType); showExportSheet =
                false
            },
            onWhatsApp = {
                ExportManager.shareToWhatsApp(
                    context,
                    file,
                    success.type.mimeType
                ); showExportSheet = false
            },
            onDownload = {
                ExportManager.saveToDownloads(context, file, success.fileName); showExportSheet =
                false; exportVm.reset()
            },
            onDismiss = {
                showExportSheet = false; exportVm.reset()
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("التقارير", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                },
                actions = {
                    val isExporting = exportState is ExportState.Loading
                    // ── Export — Premium only ──────────────────────────
                    if (flags.reportExport) {
                        IconButton(
                            onClick = {
                                if (!isExporting) {
                                    exportVm.exportSalesReportExcel(
                                        transactions = uiState.filteredTransactions,
                                        periodLabel = uiState.period.name,
                                        storeName = storeName,
                                        dailySales = uiState.dailySales,
                                        topSpenders = uiState.topSpenders,
                                        paymentShares = uiState.paymentShares,
                                        cacheDir = context.cacheDir
                                    )
                                }
                            }
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Emerald500
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Analytics,
                                    contentDescription = "تصدير Excel",
                                    tint = Emerald500
                                )
                            }
                        }
                    } else {
                        // 🔒 Free: navigate to subscription screen
                        PremiumLockChip(feature = "تصدير", onUpgrade = onNavigateToSubscription)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Period Switcher ──────────────────────────────────
            item {
                PeriodSwitcher(
                    selected = uiState.period,
                    onSelect = viewModel::setPeriod,
                    flags = flags,
                    onUpgrade = onNavigateToSubscription
                )
            }

            // ── Summary Cards ────────────────────────────────────
            item {
                if (uiState.isAdvancedReportsEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryCard(
                            "إجمالي المبيعات",
                            uiState.totalAmount,
                            Emerald500,
                            Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricCard(
                                "صافي الربح",
                                uiState.netProfit,
                                PaidGreen,
                                Modifier.weight(1f)
                            )
                            MetricCard(
                                "قيمة المخزون",
                                uiState.inventorySaleValue,
                                Violet500,
                                Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricCard(
                                "تكلفة المخزون",
                                uiState.inventoryCostValue,
                                UnpaidAmber,
                                Modifier.weight(1f)
                            )
                            MetricCard(
                                "ديون متأخرة",
                                uiState.debtAging.totalAmount,
                                DebtRed,
                                Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    SummaryCard(
                        label = "إجمالي المبيعات",
                        value = uiState.totalAmount,
                        color = Emerald500,
                        modifier = Modifier.fillMaxWidth(),
                        paidAmount = uiState.paidAmount,
                        unpaidAmount = uiState.unpaidAmount,
                        txCount = uiState.txCount
                    )
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Emerald500)
                    }
                }
            } else if (!uiState.isAdvancedReportsEnabled) {

                // ── Section المجاني ───────────────────────────────

                // 1️⃣ توزيع الحالة — مدفوع مقابل معلق
                item {
                    FreeStatusBreakdownCard(
                        paid = uiState.paidAmount,
                        unpaid = uiState.unpaidAmount,
                        total = uiState.totalAmount
                    )
                }

                // 2️⃣ أفضل 3 زبائن شراءً
                if (uiState.topSpenders.isNotEmpty()) {
                    item {
                        FreeTopCustomersCard(
                            customers = uiState.topSpenders.take(3),
                            onUpgrade = onNavigateToSubscription
                        )
                    }
                }

                // 3️⃣ توزيع طرق الدفع
                if (uiState.paymentShares.isNotEmpty()) {
                    item {
                        FreePaymentBreakdownCard(
                            shares = uiState.paymentShares,
                            onUpgrade = onNavigateToSubscription
                        )
                    }
                }

                // 4️⃣ Premium Lock — في الأسفل فقط
                item {
                    PremiumReportsLockedPreview(onUpgrade = onNavigateToSubscription)
                }
            } else {
                // ── تقويم الشهر ──────────────────────────────────────
                item {
                    CalendarCard(
                        month = uiState.calendarMonth,
                        year = uiState.calendarYear,
                        dayTotals = uiState.dayTotals,
                        selectedDay = uiState.selectedDay,
                        onDayClick = viewModel::selectDay,
                        onPrev = viewModel::prevMonth,
                        onNext = viewModel::nextMonth
                    )
                }

                // ── عمليات اليوم المحدد ──────────────────────────────
                if (uiState.selectedDay != null) {
                    item {
                        SelectedDayDetail(
                            onViewTransactions = onViewDayTransactions,
                            day = uiState.selectedDay!!,
                            month = uiState.calendarMonth,
                            year = uiState.calendarYear,
                            transactions = uiState.selectedDayTransactions,
                            summary = uiState.selectedDaySummary
                        )
                    }
                }

                // ── تحليل اليوم (صباح / ظهر / مساء) ─────────────────
                item {
                    TodayAnalysisCard(analysis = uiState.todayAnalysis)
                }

                // ── Line Chart ───────────────────────────────────────
                if (uiState.salesProfitLast7Days.isNotEmpty()) {
                    item {
                        ChartCard("المبيعات مقابل الربح - آخر 7 أيام") {
                            SalesProfitBarChart(
                                data = uiState.salesProfitLast7Days,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                            )
                        }
                    }
                }

                item {
                    DebtAgingCard(uiState.debtAging)
                }

                if (uiState.dailySales.isNotEmpty()) {
                    item {
                        ChartCard("منحنى المبيعات اليومية") {
                            LineChart(
                                uiState.dailySales, Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    }
                }

                // ── Bar Chart ────────────────────────────────────────
                if (uiState.dailySales.isNotEmpty()) {
                    item {
                        ChartCard("مدفوع مقابل غير مدفوع") {
                            BarChart(
                                uiState.dailySales, Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                        }
                    }
                }

                // ── Donut Chart ──────────────────────────────────────
                if (uiState.paymentShares.isNotEmpty()) {
                    item {
                        ChartCard("توزيع طرق الدفع") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DonutChart(uiState.paymentShares, Modifier.size(140.dp))
                                Spacer(Modifier.width(16.dp))
                                val donutColors = listOf(
                                    Emerald500,
                                    Color(0xFF3B82F6),
                                    Color(0xFFF59E0B),
                                    Color(0xFFEF4444),
                                    Color(0xFF8B5CF6)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    uiState.paymentShares.forEachIndexed { i, share ->
                                        val total =
                                            uiState.paymentShares.sumOf {
                                                it.amount
                                            }.takeIf {
                                                it > 0
                                            }
                                                ?: 1.0
                                        LegendItem(
                                            share.name,
                                            (share.amount / total * 100).toInt(),
                                            donutColors[i % donutColors.size]
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Top Spenders / Debtors ───────────────────────────
                if (uiState.topSpenders.isNotEmpty()) {
                    item {
                        ChartCard("أعلى 5 زبائن شراءً") {
                            RankList(
                                uiState.topSpenders,
                                Emerald500
                            )
                        }
                    }
                }
                if (uiState.topDebtors.isNotEmpty()) {
                    item {
                        ChartCard("أعلى 5 زبائن ديناً") {
                            RankList(uiState.topDebtors, DebtRed)
                        }
                    }
                }
            }
        }
    }
}

// ── التقويم ───────────────────────────────────────────────────
@Composable
private fun CalendarCard(
    month: Int, year: Int,
    dayTotals: Map<Int, Double>,
    selectedDay: Int?,
    onDayClick: (Int) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val arabicMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    val maxDayTotal = dayTotals.values.maxOrNull()?.takeIf {
        it > 0
    } ?: 1.0

    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - 1 + 7) % 7 // 0=Sun

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // رأس التقويم
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        arabicMonths[month],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$year", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null)
                }
            }

            Spacer(Modifier.height(8.dp))

            // أيام الأسبوع
            val weekDays = listOf("أح", "إث", "ثل", "أر", "خم", "جم", "سب")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { d ->
                    Text(
                        d, modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // خلايا الأيام
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - firstDayOfWeek + 1
                        if (day < 1 || day > daysInMonth) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            val hasData = dayTotals.containsKey(day)
                            val intensity =
                                if (hasData) (dayTotals[day]!! / maxDayTotal).toFloat() else 0f
                            val isSelected = day == selectedDay
                            val isToday = run {
                                val now = Calendar.getInstance()
                                day == now.get(Calendar.DAY_OF_MONTH) &&
                                        month == now.get(Calendar.MONTH) &&
                                        year == now.get(Calendar.YEAR)
                            }

                            CalendarDay(
                                day = day,
                                intensity = intensity,
                                hasData = hasData,
                                isSelected = isSelected,
                                isToday = isToday,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onDayClick(day)
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int, intensity: Float, hasData: Boolean,
    isSelected: Boolean, isToday: Boolean,
    modifier: Modifier, onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> Emerald500
        hasData -> Emerald500.copy(alpha = 0.15f + intensity * 0.5f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> Color.White
        isToday -> Emerald500
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$day",
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (hasData && !isSelected) {
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Emerald500)
                )
            }
        }
    }
}

// ── تفاصيل اليوم المحدد ──────────────────────────────────────
@Composable
private fun SelectedDayDetail(
    onViewTransactions: (Long) -> Unit = {},
    day: Int, month: Int, year: Int,
    transactions: List<Transaction>,
    summary: Triple<Double, Double, Double>
) {
    val arabicMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    val (total, paid, unpaid) = summary

    Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            // عنوان
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Emerald500),
                    Alignment.Center
                ) {
                    Text(
                        "$day", color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "${arabicMonths[month]} $year",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            if (transactions.isEmpty()) {
                // لا توجد عمليات
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.EventBusy, null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "لا توجد عمليات في هذا اليوم",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // ملخص اليوم
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DaySummaryChip("الإجمالي", total, Emerald500, Modifier.weight(1f))
                    DaySummaryChip("مدفوع", paid, PaidGreen, Modifier.weight(1f))
                    DaySummaryChip("معلق", unpaid, DebtRed, Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.3f))
                Spacer(Modifier.height(10.dp))

                // قائمة العمليات
                transactions.take(5).forEach { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (tx.isPaid) PaidGreen else UnpaidAmber)
                        )
                        Text(
                            tx.customerName.ifEmpty {
                                "—"
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            String.format("%.0f", tx.amount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (tx.isPaid) PaidGreen else UnpaidAmber
                        )
                    }
                }
                if (transactions.size > 5) {
                    Text(
                        "+ ${transactions.size - 5} عمليات أخرى",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.YEAR, year)
                        cal.set(Calendar.MONTH, month)
                        cal.set(Calendar.DAY_OF_MONTH, day)
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        onViewTransactions(cal.timeInMillis)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "عرض كل العمليات (${transactions.size})",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySummaryChip(label: String, value: Double, color: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = color.copy(0.1f)) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Spacer(Modifier.height(2.dp))
            Text(
                String.format("%.0f", value),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = color
            )
        }
    }
}

// ── تحليل اليوم (صباح/ظهر/مساء) ─────────────────────────────
@Composable
private fun TodayAnalysisCard(analysis: TimeOfDayAnalysis) {
    val total = (analysis.morningTotal + analysis.afternoonTotal + analysis.eveningTotal)
        .takeIf {
            it > 0
        } ?: 1.0

    Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Analytics, null,
                    tint = Violet500, modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "تحليل اليوم",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(14.dp))

            listOf(
                Triple("🌅 الصباح", analysis.morningTotal, Color(0xFFF59E0B)),
                Triple("☀️ الظهيرة", analysis.afternoonTotal, Color(0xFF06B6D4)),
                Triple("🌙 المساء", analysis.eveningTotal, Color(0xFF8B5CF6))
            ).forEach { (label, value, color) ->
                TimeSlotRow(label, value, (value / total).toFloat(), color)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun TimeSlotRow(label: String, value: Double, ratio: Float, color: Color) {
    val progress = remember {
        Animatable(0f)
    }
    LaunchedEffect(ratio) {
        progress.snapTo(0f)
        progress.animateTo(ratio, tween(900, easing = FastOutSlowInEasing))
    }
    val anim by progress.asState()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(
                String.format("%.0f", value),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(anim)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(0.7f), color)))
            )
        }
    }
}

// ── الـ Composables المشتركة (من الملف الأصلي) ───────────────
@Composable
private fun PeriodSwitcher(
    selected: ReportPeriod,
    onSelect: (ReportPeriod) -> Unit,
    flags: FeatureFlags.FlagSet,
    onUpgrade: () -> Unit = {}
) {
    val allLabels = mapOf(
        ReportPeriod.TODAY to "اليوم",
        ReportPeriod.WEEK to "الأسبوع",
        ReportPeriod.MONTH to "الشهر"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        allLabels.forEach { (period, label) ->
            val isSelected = period == selected
            val isMonthLocked = period == ReportPeriod.MONTH && !flags.monthReport

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isMonthLocked -> Color.Transparent
                            isSelected -> Emerald500
                            else -> Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = { if (isMonthLocked) onUpgrade() else onSelect(period) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            color = when {
                                isMonthLocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    0.4f
                                )

                                isSelected -> Color.White
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        // 🔒 قفل صغير أمام "الشهر" للمستخدم المجاني
                        if (isMonthLocked) {
                            Icon(
                                Icons.Rounded.Lock, null,
                                modifier = Modifier.size(11.dp),
                                tint = Color(0xFFFFA500)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: Double,
    color: Color,
    modifier: Modifier,
    paidAmount: Double = 0.0,
    unpaidAmount: Double = 0.0,
    txCount: Int = 0
) {
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(value) { target = value.toFloat() }
    val animated by animateFloatAsState(
        target, tween(1200, easing = FastOutSlowInEasing), label = "total"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to color.copy(alpha = 0.18f),
                            0.6f to color.copy(alpha = 0.10f),
                            1.0f to color.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(1.dp, color.copy(alpha = 0.20f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column {
                // ── رأس البطاقة ──────────────────────────────────
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (txCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$txCount عملية",
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── الرقم الرئيسي ─────────────────────────────────
                Text(
                    text = "₪ ${String.format("%,.0f", animated)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )

                // ── مدفوع / معلق ──────────────────────────────────
                if (paidAmount > 0 || unpaidAmount > 0) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MiniAmountPill(
                            label = "مدفوع",
                            amount = paidAmount,
                            color = PaidGreen,
                            icon = Icons.Rounded.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                        MiniAmountPill(
                            label = "معلق",
                            amount = unpaidAmount,
                            color = UnpaidAmber,
                            icon = Icons.Rounded.Schedule,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAmountPill(
    label: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
            Text(
                "₪ ${String.format("%,.0f", amount)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}


@Composable
private fun MetricCard(label: String, value: Double, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.1f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                String.format("%.0f", value),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun PremiumReportsLockedPreview(onUpgrade: () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, UnpaidAmber.copy(0.25f))
    ) {
        Box {
            // خلفية ضبابية — preview مقطوع
            Column(
                modifier = Modifier.blur(6.dp).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("صافي الربح", 1280.0, PaidGreen, Modifier.weight(1f))
                    MetricCard("قيمة المخزون", 18450.0, Violet500, Modifier.weight(1f))
                }
                SalesProfitBarChart(
                    data = listOf(
                        SalesProfitDayEntry("١", 850.0, 230.0),
                        SalesProfitDayEntry("٢", 1200.0, 420.0),
                        SalesProfitDayEntry("٣", 640.0, 180.0),
                        SalesProfitDayEntry("٤", 1580.0, 610.0),
                        SalesProfitDayEntry("٥", 960.0, 300.0),
                        SalesProfitDayEntry("٦", 1420.0, 520.0),
                        SalesProfitDayEntry("٧", 1100.0, 390.0)
                    ),
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                )
            }

            // طبقة gradient للضبابية
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface.copy(0.4f),
                                MaterialTheme.colorScheme.surface.copy(0.92f)
                            )
                        )
                    )
            )

            // طبقة القفل
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(UnpaidAmber.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = UnpaidAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "افتح التحليلات المتقدمة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                listOf(
                    "📊 صافي الربح الحقيقي",
                    "📦 تقييم المخزون",
                    "⏳ تحليل أعمار الديون",
                    "📈 مقارنة المبيعات والربح"
                ).forEach { feature ->
                    Text(
                        feature,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onUpgrade,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UnpaidAmber),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ترقية إلى Premium", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ── Composables المجانية الجديدة ──────────────────────────────

@Composable
private fun FreeStatusBreakdownCard(paid: Double, unpaid: Double, total: Double) {
    val safeTotal = total.coerceAtLeast(1.0)
    val paidRatio = (paid / safeTotal).toFloat().coerceIn(0f, 1f)

    val animatedRatio = remember { Animatable(0f) }
    LaunchedEffect(paidRatio) {
        animatedRatio.snapTo(0f)
        animatedRatio.animateTo(paidRatio, tween(1000, easing = FastOutSlowInEasing))
    }
    val anim by animatedRatio.asState()

    Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.PieChart, null, tint = Cyan500, modifier = Modifier.size(18.dp))
                Text("حالة المبيعات", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))

            // Progress Bar مركّبة
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DebtRed.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(anim)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.horizontalGradient(listOf(PaidGreen.copy(0.8f), PaidGreen)))
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusLegendItem("مدفوع", paid, (paidRatio * 100).toInt(), PaidGreen, Modifier.weight(1f))
                StatusLegendItem("معلق", unpaid, ((1f - paidRatio) * 100).toInt(), DebtRed, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatusLegendItem(label: String, amount: Double, percent: Int, color: Color, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(0.08f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(
                "₪ ${String.format("%,.0f", amount)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text("$percent%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FreeTopCustomersCard(customers: List<CustomerRank>, onUpgrade: () -> Unit) {
    val maxVal = customers.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0

    Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = UnpaidAmber, modifier = Modifier.size(18.dp))
                    Text("أفضل 3 زبائن", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Surface(onClick = onUpgrade, shape = RoundedCornerShape(20.dp), color = UnpaidAmber.copy(0.12f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, null, tint = UnpaidAmber, modifier = Modifier.size(10.dp))
                        Text("أعلى 5", style = MaterialTheme.typography.labelSmall, color = UnpaidAmber)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            customers.forEachIndexed { index, customer ->
                val medal = when (index) { 0 -> "🥇"; 1 -> "🥈"; else -> "🥉" }
                val barAnim = remember { Animatable(0f) }
                LaunchedEffect(customer.amount) {
                    barAnim.snapTo(0f)
                    barAnim.animateTo((customer.amount / maxVal).toFloat(), tween(800, index * 120, FastOutSlowInEasing))
                }
                val prog by barAnim.asState()

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(medal, style = MaterialTheme.typography.bodyMedium)
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(
                                customer.name.ifEmpty { "—" },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "₪ ${String.format("%,.0f", customer.amount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald500,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.fillMaxWidth().height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Emerald500.copy(0.12f))
                        ) {
                            Box(
                                Modifier.fillMaxWidth(prog).fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Brush.horizontalGradient(listOf(Emerald500.copy(0.7f), Emerald500)))
                            )
                        }
                    }
                }
                if (index < customers.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outline.copy(0.08f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FreePaymentBreakdownCard(shares: List<PaymentShare>, onUpgrade: () -> Unit) {
    val donutColors = listOf(Emerald500, Cyan500, Violet500, UnpaidAmber, DebtRed)
    val total = shares.sumOf { it.amount }.coerceAtLeast(1.0)

    Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Payments, null, tint = Violet500, modifier = Modifier.size(18.dp))
                Text("طرق الدفع", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            shares.take(4).forEachIndexed { index, share ->
                val ratio = (share.amount / total).toFloat().coerceIn(0f, 1f)
                val color = donutColors[index % donutColors.size]
                val barAnim = remember { Animatable(0f) }
                LaunchedEffect(ratio) {
                    barAnim.snapTo(0f)
                    barAnim.animateTo(ratio, tween(900, index * 100, FastOutSlowInEasing))
                }
                val prog by barAnim.asState()

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                    Text(
                        share.name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(80.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        Modifier.weight(1f).height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color.copy(0.12f))
                    ) {
                        Box(
                            Modifier.fillMaxWidth(prog).fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                    }
                    Text(
                        "${(ratio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}


@Composable
private fun DebtAgingCard(debtAging: DebtAging) {
    ChartCard("تحليل أعمار الديون") {
        val rows = listOf(
            Triple("أقل من أسبوع", debtAging.lessThanWeekAmount, debtAging.lessThanWeekCount),
            Triple(
                "أسبوع إلى شهر",
                debtAging.oneWeekToOneMonthAmount,
                debtAging.oneWeekToOneMonthCount
            ),
            Triple(
                "شهر إلى 3 أشهر",
                debtAging.oneMonthToThreeMonthsAmount,
                debtAging.oneMonthToThreeMonthsCount
            ),
            Triple(
                "أكثر من 3 أشهر",
                debtAging.moreThanThreeMonthsAmount,
                debtAging.moreThanThreeMonthsCount
            )
        )
        val maxAmount = rows.maxOf {
            it.second
        }.takeIf {
            it > 0
        } ?: 1.0
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEachIndexed { index, row ->
                val color = when (index) {
                    0 -> UnpaidAmber
                    1 -> Color(0xFFF97316)
                    2 -> Color(0xFFEF4444)
                    else -> DebtRed
                }
                AgingRow(
                    label = row.first,
                    amount = row.second,
                    count = row.third,
                    ratio = (row.second / maxAmount).toFloat(),
                    color = color
                )
            }
        }
    }
}

@Composable
private fun AgingRow(label: String, amount: Double, count: Int, ratio: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(
                "${String.format("%.0f", amount)} · $count",
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(0.12f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

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

@Composable
private fun LineChart(data: List<DaySalesEntry>, modifier: Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
    }
    val anim by progress.asState()

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val maxVal = data.maxOf { it.total }.takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (data.size - 1).coerceAtLeast(1)
        val points = data.mapIndexed { i, e ->
            Offset(i * stepX, size.height * (1f - (e.total / maxVal).toFloat()))
        }
        val visible = points
            .take((points.size * anim).toInt().coerceAtLeast(1).coerceAtMost(points.size))

        if (visible.size < 2) {
            visible.firstOrNull()?.let { drawCircle(Emerald500, 5.dp.toPx(), it) }
            return@Canvas
        }

        // Build a smooth Bezier path through all visible points.
        // Control points are derived by offsetting 40 % of the horizontal distance
        // which gives a fluid S-curve feel without overshooting vertically.
        val linePath = Path().apply {
            moveTo(visible.first().x, visible.first().y)
            for (i in 0 until visible.size - 1) {
                val p0 = visible[i]
                val p1 = visible[i + 1]
                val cp1x = p0.x + (p1.x - p0.x) * 0.4f
                val cp2x = p0.x + (p1.x - p0.x) * 0.6f
                cubicTo(cp1x, p0.y, cp2x, p1.y, p1.x, p1.y)
            }
        }

        // Closed fill path uses the same Bezier line, then drops straight down.
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(visible.last().x, size.height)
            lineTo(visible.first().x, size.height)
            close()
        }

        // Vertical gradient fill — rich Emerald to transparent for a "2026 premium" feel.
        drawPath(
            fillPath,
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to Emerald500.copy(alpha = 0.40f),
                    0.6f to Emerald500.copy(alpha = 0.12f),
                    1.0f to Color.Transparent
                ),
                startY = 0f,
                endY = size.height
            )
        )

        // Smooth Bezier stroke
        drawPath(linePath, Emerald500, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

        // Data-point dots
        visible.forEach { pt ->
            drawCircle(Color.White, 5.dp.toPx(), pt)
            drawCircle(Emerald500, 3.dp.toPx(), pt)
        }
    }
}

@Composable
private fun BarChart(data: List<DaySalesEntry>, modifier: Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }
    val anim by progress.asState()

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val maxVal = data.maxOf { it.total }.takeIf { it > 0 } ?: 1.0
        val groupW = size.width / data.size
        val barW = groupW * 0.35f
        val r = 6.dp.toPx()

        data.forEachIndexed { i, entry ->
            val left = i * groupW + barW * 0.2f
            val paidH = (entry.paid / maxVal * size.height * anim).toFloat().coerceAtLeast(0f)
            val unpaidH = ((entry.total - entry.paid) / maxVal * size.height * anim)
                .toFloat().coerceAtLeast(0f)

            // Paid bar — top-only rounded corners for a modern card-bar look
            drawTopRoundedBar(
                color = PaidGreen,
                left = left,
                barWidth = barW,
                barHeight = paidH,
                bottomY = size.height,
                cornerRadius = r
            )

            // Unpaid bar
            drawTopRoundedBar(
                color = DebtRed.copy(alpha = 0.75f),
                left = left + barW + 2.dp.toPx(),
                barWidth = barW,
                barHeight = unpaidH,
                bottomY = size.height,
                cornerRadius = r
            )
        }
    }
}

/**
 * Draws a rectangle with rounded corners only on the top-left and top-right,
 * giving bars the modern "card pillar" appearance without extra dependencies.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTopRoundedBar(
    color: Color,
    left: Float,
    barWidth: Float,
    barHeight: Float,
    bottomY: Float,
    cornerRadius: Float
) {
    if (barHeight <= 0f) return
    val top = bottomY - barHeight
    val right = left + barWidth
    val r = cornerRadius.coerceAtMost(barWidth / 2f).coerceAtMost(barHeight / 2f)

    val path = Path().apply {
        moveTo(left, bottomY)
        lineTo(left, top + r)
        arcTo(Rect(left, top, left + r * 2f, top + r * 2f), 180f, 90f, false)
        lineTo(right - r, top)
        arcTo(Rect(right - r * 2f, top, right, top + r * 2f), 270f, 90f, false)
        lineTo(right, bottomY)
        close()
    }
    drawPath(path, color)
}

@Composable
private fun SalesProfitBarChart(data: List<SalesProfitDayEntry>, modifier: Modifier) {
    val progress = remember {
        Animatable(0f)
    }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }
    val anim by progress.asState()

    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorDotLabel("المبيعات", Emerald500)
            ColorDotLabel("الربح", Cyan500)
        }

        Canvas(modifier = modifier) {
            if (data.isEmpty()) return@Canvas

            val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            val h = size.height
            val w = size.width

            val maxUp = max(
                data.maxOf { it.sales },
                data.maxOf { max(0.0, it.profit) }
            ).coerceAtLeast(1.0)
            val minProfit = data.minOf { it.profit }
            val maxDown = if (minProfit < 0) -minProfit else 0.0
            val totalMag = maxUp + maxDown

            val yZero = when {
                maxDown <= 1e-9 -> h
                maxUp <= 1e-9 -> 0f
                else -> (h * (maxUp / totalMag)).toFloat()
            }

            val posSpan = yZero
            val negSpan = h - yZero
            val safeMaxDown = maxDown.coerceAtLeast(1e-9)

            // Horizontal grid (reference lines), evenly spaced
            repeat(4) { i ->
                val y = h * (i + 1) / 5f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Zero / baseline
            drawLine(
                color = axisColor,
                start = Offset(0f, yZero),
                end = Offset(w, yZero),
                strokeWidth = 1.dp.toPx()
            )

            val groupWidth = w / data.size
            val gap = 4.dp.toPx()
            val pairPadding = groupWidth * 0.06f

            data.forEachIndexed { index, entry ->
                val inner = groupWidth - 2f * pairPadding
                val barW = max((inner - gap) / 2f, 2.dp.toPx())
                val pairWidth = barW * 2f + gap
                val groupLeft = index * groupWidth
                val startX = groupLeft + (groupWidth - pairWidth) / 2f
                val salesLeft = startX
                val profitLeft = startX + barW + gap

                val salesH = (entry.sales / maxUp * posSpan * anim).toFloat().coerceAtLeast(0f)
                drawRoundRect(
                    color = Cyan500,
                    topLeft = Offset(salesLeft, yZero - salesH),
                    size = Size(barW, salesH),
                    cornerRadius = cornerRadius
                )

                if (entry.profit >= 0) {
                    val profitH =
                        (entry.profit / maxUp * posSpan * anim).toFloat().coerceAtLeast(0f)
                    drawRoundRect(
                        color = PaidGreen,
                        topLeft = Offset(profitLeft, yZero - profitH),
                        size = Size(barW, profitH),
                        cornerRadius = cornerRadius
                    )
                } else {
                    val profitH =
                        (-entry.profit / safeMaxDown * negSpan * anim).toFloat().coerceAtLeast(0f)
                    drawRoundRect(
                        color = DebtRed,
                        topLeft = Offset(profitLeft, yZero),
                        size = Size(barW, profitH),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            data.forEach { entry ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorDotLabel(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DonutChart(data: List<PaymentShare>, modifier: Modifier) {
    val donutColors = listOf(
        Emerald500,
        Color(0xFF3B82F6),
        Color(0xFFF59E0B),
        Color(0xFFEF4444),
        Color(0xFF8B5CF6)
    )
    val progress = remember {
        Animatable(0f)
    }
    LaunchedEffect(Unit) {
        progress.snapTo(0f); progress.animateTo(
        1f,
        tween(1400, easing = FastOutSlowInEasing)
    )
    }
    val anim by progress.asState()
    val total = data.sumOf {
        it.amount
    }.takeIf {
        it > 0
    } ?: 1.0
    Canvas(modifier = modifier) {
        val stroke = 28.dp.toPx()
        val radius = min(size.width, size.height) / 2f - stroke / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        var startAngle = -90f
        data.forEachIndexed { i, share ->
            val sweep = (share.amount / total * 360 * anim).toFloat()
            drawArc(
                donutColors[i % donutColors.size], startAngle, sweep, false,
                Offset(center.x - radius, center.y - radius),
                Size(radius * 2, radius * 2), style = Stroke(stroke, cap = StrokeCap.Round)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun LegendItem(label: String, percent: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text("$label ($percent%)", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RankList(items: List<CustomerRank>, color: Color) {
    val maxVal = items.maxOfOrNull {
        it.amount
    }?.takeIf {
        it > 0
    } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, item ->
            val barProgress = remember {
                Animatable(0f)
            }
            LaunchedEffect(item.amount) {
                barProgress.snapTo(0f)
                barProgress.animateTo(
                    (item.amount / maxVal).toFloat(),
                    tween(900, i * 100, FastOutSlowInEasing)
                )
            }
            val prog by barProgress.asState()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${i + 1}", style = MaterialTheme.typography.labelSmall,
                    color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp)
                )
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            String.format("%.0f", item.amount),
                            style = MaterialTheme.typography.labelSmall,
                            color = color
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color.copy(0.15f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(prog)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}