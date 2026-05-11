package com.trader.salesmanager.ui.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.DebtAging
import com.trader.core.domain.model.Transaction
import com.trader.salesmanager.ui.theme.DebtRed
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.Emerald700
import com.trader.salesmanager.ui.theme.PaidGreen
import com.trader.salesmanager.ui.theme.UnpaidAmber
import com.trader.salesmanager.ui.theme.Violet500
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import com.trader.salesmanager.util.export.*
import com.trader.core.domain.model.FeatureFlags
import com.trader.salesmanager.ui.components.PremiumLockChip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.CircularProgressIndicator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import com.trader.core.data.local.appDataStore
import com.trader.salesmanager.ui.theme.Cyan500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateUp: () -> Unit,
    onViewDayTransactions: (Long) -> Unit = {},
    onUpgrade: () -> Unit = {},
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
                        PremiumLockChip(feature = "تصدير", onUpgrade = onUpgrade)
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
                    onUpgrade = onUpgrade
                )
            }

            // ── Summary Cards ────────────────────────────────────
            item {
                if (uiState.isAdvancedReportsEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TotalSalesHeroCard(
                            label = "إجمالي المبيعات",
                            value = uiState.totalAmount,
                            modifier = Modifier.fillMaxWidth()
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
                    TotalSalesHeroCard(
                        label = "إجمالي المبيعات",
                        value = uiState.totalAmount,
                        modifier = Modifier.fillMaxWidth()
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
                item {
                    PremiumReportsLockedPreview(onUpgrade = onUpgrade)
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

// ── Total Sales Hero Card — 2026 premium look ──────────────────
// A prominent gradient card with an animated counter, decorative blob,
// and trailing icon. Reuses the brand Emerald → Cyan gradient for
// consistency with other elevated surfaces (Home, Inventory headers).
@Composable
private fun TotalSalesHeroCard(label: String, value: Double, modifier: Modifier) {
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(value) {
        target = value.toFloat()
    }
    val animated by animateFloatAsState(
        target,
        tween(1200, easing = FastOutSlowInEasing),
        label = "totalSalesCount"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Emerald700, Emerald500, Cyan500)
                    )
                )
        ) {
            // Decorative soft blob — purely visual.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f))
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            String.format("%,.0f", animated),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            "₪",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Analytics,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: Double, color: Color, modifier: Modifier) {
    var target by remember {
        mutableFloatStateOf(0f)
    }
    LaunchedEffect(value) {
        target = value.toFloat()
    }
    val animated by animateFloatAsState(
        target,
        tween(1200, easing = FastOutSlowInEasing),
        label = "count"
    )
    Card(
        modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Spacer(Modifier.height(4.dp))
            Text(
                String.format("%.0f", animated),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold, color = color
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
    Card(shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Box {
            Column(
                modifier = Modifier
                    .blur(5.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("صافي الربح", 1280.0, PaidGreen, Modifier.weight(1f))
                    MetricCard("قيمة المخزون", 18450.0, Violet500, Modifier.weight(1f))
                }
                SalesProfitBarChart(
                    data = listOf(
                        SalesProfitDayEntry("1", 850.0, 230.0),
                        SalesProfitDayEntry("2", 1200.0, 420.0),
                        SalesProfitDayEntry("3", 640.0, 180.0),
                        SalesProfitDayEntry("4", 1580.0, 610.0),
                        SalesProfitDayEntry("5", 960.0, 300.0),
                        SalesProfitDayEntry("6", 1420.0, 520.0),
                        SalesProfitDayEntry("7", 1100.0, 390.0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
            Surface(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = UnpaidAmber,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "التقارير الذكية متاحة في الخطة المميزة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "فعّل التحليلات المتقدمة لعرض صافي الربح، قيمة المخزون، أعمار الديون، ومقارنة المبيعات بالربح.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onUpgrade,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = UnpaidAmber)
                    ) {
                        Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ترقية إلى Premium", fontWeight = FontWeight.Bold)
                    }
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

            // Paid bar — gradient Emerald → Cyan for 2026 premium look
            drawTopRoundedBar(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(0f to Emerald500, 1f to PaidGreen.copy(0.7f)),
                    startY = size.height - paidH, endY = size.height
                ),
                left = left,
                barWidth = barW,
                barHeight = paidH,
                bottomY = size.height,
                cornerRadius = r
            )

            // Unpaid bar — gradient Amber → DebtRed
            drawTopRoundedBar(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(0f to DebtRed, 1f to DebtRed.copy(0.5f)),
                    startY = size.height - unpaidH, endY = size.height
                ),
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
 * Draws a gradient-filled rectangle with rounded top corners only —
 * the modern "card pillar" bar appearance.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTopRoundedBar(
    brush: Brush,
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
    drawPath(path, brush)
}

// 2026-style dual Bezier line chart for sales vs profit.
// Replaces the flat bar chart with smooth curves + gradient fills for a
// premium feel that matches the TotalSalesHeroCard design language.
@Composable
private fun SalesProfitBarChart(data: List<SalesProfitDayEntry>, modifier: Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
    }
    val anim by progress.asState()

    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    val salesColor = Emerald500
    val profitColor = Cyan500
    val lossColor = DebtRed

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorDotLabel("المبيعات", salesColor)
            ColorDotLabel("الربح", profitColor)
        }

        Canvas(modifier = modifier) {
            if (data.isEmpty()) return@Canvas

            val h = size.height
            val w = size.width
            val n = data.size

            val maxSales = data.maxOf { it.sales }.coerceAtLeast(1.0)
            val maxProfit = data.maxOf { maxOf(it.profit, 0.0) }.coerceAtLeast(1.0)
            val minProfit = data.minOf { it.profit }
            val maxDown = if (minProfit < 0.0) -minProfit else 0.0
            val profitRange = maxProfit + maxDown

            val yZero = if (maxDown > 0.0) (h * maxProfit / profitRange).toFloat() else h

            // Grid lines
            repeat(4) { i ->
                val y = h * (i + 1) / 5f
                drawLine(gridColor, Offset(0f, y), Offset(w, y), 1.dp.toPx())
            }
            // Baseline
            drawLine(
                gridColor.copy(alpha = 0.35f),
                Offset(0f, yZero),
                Offset(w, yZero),
                strokeWidth = 1.dp.toPx()
            )

            fun xOf(i: Int) = if (n == 1) w / 2f else i * w / (n - 1).toFloat()

            // Build animated point lists
            val salesPoints = data.mapIndexed { i, e ->
                Offset(xOf(i), h - (e.sales / maxSales * h * anim).toFloat())
            }
            val profitPoints = data.mapIndexed { i, e ->
                val y = if (e.profit >= 0)
                    yZero - (e.profit / maxProfit * yZero * anim).toFloat()
                else
                    yZero + (-e.profit / profitRange.coerceAtLeast(1e-9) * (h - yZero) * anim).toFloat()
                Offset(xOf(i), y)
            }

            // Helper: build a Bezier path through a list of points
            fun buildBezierPath(pts: List<Offset>): Path = Path().apply {
                if (pts.isEmpty()) return@apply
                moveTo(pts.first().x, pts.first().y)
                for (k in 0 until pts.size - 1) {
                    val p0 = pts[k]; val p1 = pts[k + 1]
                    val cpX1 = p0.x + (p1.x - p0.x) * 0.4f
                    val cpX2 = p0.x + (p1.x - p0.x) * 0.6f
                    cubicTo(cpX1, p0.y, cpX2, p1.y, p1.x, p1.y)
                }
            }

            fun buildFillPath(linePath: Path, pts: List<Offset>, baseline: Float): Path =
                Path().apply {
                    addPath(linePath)
                    lineTo(pts.last().x, baseline)
                    lineTo(pts.first().x, baseline)
                    close()
                }

            // ── Sales curve (Emerald gradient fill) ──────────────────
            val salesLine = buildBezierPath(salesPoints)
            drawPath(
                buildFillPath(salesLine, salesPoints, h),
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to salesColor.copy(alpha = 0.38f),
                        0.55f to salesColor.copy(alpha = 0.12f),
                        1f to Color.Transparent
                    ),
                    startY = 0f, endY = h
                )
            )
            drawPath(salesLine, salesColor, style = Stroke(2.5f.dp.toPx(), cap = StrokeCap.Round))
            salesPoints.forEach { pt ->
                drawCircle(Color.White, 4.5f.dp.toPx(), pt)
                drawCircle(salesColor, 2.8f.dp.toPx(), pt)
            }

            // ── Profit curve (Cyan gradient fill above zero; Red below) ──
            val profitLine = buildBezierPath(profitPoints)
            val profitFill = buildFillPath(profitLine, profitPoints, yZero)
            val hasPosProfit = profitPoints.any { it.y < yZero }
            val hasNegProfit = profitPoints.any { it.y > yZero }

            if (hasPosProfit) {
                drawPath(
                    profitFill,
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to profitColor.copy(alpha = 0.35f),
                            0.6f to profitColor.copy(alpha = 0.08f),
                            1f to Color.Transparent
                        ),
                        startY = 0f, endY = yZero
                    )
                )
            }
            if (hasNegProfit) {
                drawPath(
                    profitFill,
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.4f to lossColor.copy(alpha = 0.08f),
                            1f to lossColor.copy(alpha = 0.28f)
                        ),
                        startY = yZero, endY = h
                    )
                )
            }
            drawPath(profitLine, profitColor, style = Stroke(2.5f.dp.toPx(), cap = StrokeCap.Round))
            profitPoints.forEach { pt ->
                drawCircle(Color.White, 4.5f.dp.toPx(), pt)
                val dotColor = if (pt.y > yZero) lossColor else profitColor
                drawCircle(dotColor, 2.8f.dp.toPx(), pt)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            data.forEach { entry ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
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
                            .background(color.copy(0.12f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(prog)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(color, color.copy(0.6f)))
                                )
                        )
                    }
                }
            }
        }
    }
}