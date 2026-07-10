package com.trader.salesmanager.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.core.domain.model.Transaction
import com.trader.salesmanager.ui.components.AnimatedCounter
import com.trader.salesmanager.ui.theme.Cyan500
import com.trader.salesmanager.ui.theme.DebtRed
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.Emerald700
import com.trader.salesmanager.ui.theme.PaidGreen
import com.trader.salesmanager.ui.theme.UnpaidAmber
import com.trader.salesmanager.ui.theme.Violet500
import com.trader.salesmanager.ui.theme.appColors
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HEADER_CONTENT_HEIGHT = 140.dp
private val OVERLAP = 40.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCustomers: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToInventory: () -> Unit = {},
    onAddTransaction: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onCustomerClick: (Long) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
    syncViewModel: SyncStatusViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncState by syncViewModel.syncState.collectAsStateWithLifecycle()
    val unsyncedItems by syncViewModel.unsyncedItems.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showSyncSheet by rememberSaveable { mutableStateOf(false) }

    if (showSyncSheet) {
        SyncStatusBottomSheet(
            syncState = syncState,
            unsyncedItems = unsyncedItems,
            onDismiss = { showSyncSheet = false },
            onRetrySync = { syncViewModel.retrySync() }
        )
    }

    Scaffold(
        containerColor = appColors.screenBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = {
                    Icon(Icons.Rounded.Add, null)
                },
                text = {
                    Text("عملية جديدة", fontWeight = FontWeight.SemiBold)
                },
                containerColor = Emerald500,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(scrollState)
        ) {
            // ── Header + Stats Card ──────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HEADER_CONTENT_HEIGHT + OVERLAP)
                        .background(
                            Brush.horizontalGradient(listOf(Emerald700, Cyan500))
                        )
                )
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 20.dp, end = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "مرحباً 👋",
                                color = Color.White.copy(0.8f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "مدير المبيعات",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SyncStatusIconButton(
                                syncState = syncState,
                                onClick = { showSyncSheet = true }
                            )
                            // ── زر الشات مع Badge ────────────────
                            Box {
                                IconButton(
                                    onClick = onNavigateToChat,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(0.2f))
                                ) {
                                    Icon(Icons.Rounded.Forum, null, tint = Color.White)
                                }
                                if (uiState.unreadChatCount > 0) {
                                    val badgeText = when {
                                        uiState.unreadChatCount > 99 -> "99+"
                                        uiState.unreadChatCount > 9 -> "${uiState.unreadChatCount}"
                                        else -> "${uiState.unreadChatCount}"
                                    }
                                    val badgeWidth = when {
                                        uiState.unreadChatCount > 99 -> 26.dp
                                        uiState.unreadChatCount > 9 -> 22.dp
                                        else -> 18.dp
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .height(18.dp)
                                            .width(badgeWidth)
                                            .clip(RoundedCornerShape(50))
                                            .background(DebtRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            lineHeight = 9.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.2f))
                            ) {
                                Icon(Icons.Rounded.Settings, null, tint = Color.White)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Stats Card ───────────────────────────────
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .offset(y = OVERLAP / 2)
                            .shadow(12.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "إجمالي اليوم",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            AnimatedCounter(
                                value = uiState.todayTotal,
                                style = MaterialTheme.typography.displayMedium,
                                color = Emerald500
                            )
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.4f))
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                MiniStat(Modifier.weight(1f), "مدفوع", uiState.todayPaid, PaidGreen)
                                VerticalDivider(
                                    Modifier.height(40.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(0.3f)
                                )
                                MiniStat(
                                    Modifier.weight(1f),
                                    "غير مدفوع",
                                    uiState.todayUnpaid,
                                    UnpaidAmber
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(OVERLAP / 2 + 24.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                Text(
                    "لوحة التحليلات",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
                Spacer(Modifier.height(12.dp))
                AnalyticsDashboardSection(
                    state = uiState.dashboard,
                    onCustomerClick = onCustomerClick
                )
                Spacer(Modifier.height(28.dp))

                // ── Quick Nav ────────────────────────────────────
                Text(
                    "القوائم",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val navItems = listOf(
                    NavItem("الزبائن", Icons.Rounded.People, Emerald500, onNavigateToCustomers),
                    NavItem("العمليات", Icons.Rounded.Receipt, Cyan500, onNavigateToTransactions),
                    NavItem("التقارير", Icons.Rounded.BarChart, Violet500, onNavigateToReports),
                    NavItem("الديون", Icons.Rounded.Warning, DebtRed, onNavigateToDebts),
                    NavItem("المخزن", Icons.Rounded.Inventory2, Cyan500, onNavigateToInventory),
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    navItems.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach {
                                NavCard(Modifier.weight(1f), it)
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                // ── آخر 5 عمليات ─────────────────────────────────
                if (uiState.recentTransactions.isNotEmpty()) {
                    Spacer(Modifier.height(28.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "آخر العمليات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToTransactions) {
                            Text(
                                "عرض الكل", color = Emerald500,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.recentTransactions.forEachIndexed { index, tx ->
                            RecentTransactionCard(
                                tx = tx,
                                index = index,
                                onClick = {
                                    onTransactionClick(tx.id)
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

// ── آخر عملية Card ───────────────────────────────────────────
@Composable
private fun RecentTransactionCard(tx: Transaction, index: Int, onClick: () -> Unit) {
    val visible = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }
    AnimatedVisibility(
        visible,
        enter = slideInVertically(tween(300, index * 60)) {
            it / 2
        } + fadeIn(tween(300, index * 60))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // أيقونة الحالة
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (tx.isPaid) PaidGreen.copy(0.12f) else UnpaidAmber.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (tx.isPaid) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
                        null,
                        tint = if (tx.isPaid) PaidGreen else UnpaidAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
                // اسم الزبون + وقت
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tx.customerName.ifEmpty {
                            "—"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatTxDate(tx.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // المبلغ + حالة
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        String.format("%.0f", tx.amount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (tx.isPaid) PaidGreen else UnpaidAmber
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (tx.isPaid) PaidGreen.copy(0.12f) else UnpaidAmber.copy(0.12f)
                    ) {
                        Text(
                            if (tx.isPaid) "مدفوع" else "معلق",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tx.isPaid) PaidGreen else UnpaidAmber,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────
@Composable
private fun MiniStat(modifier: Modifier, label: String, value: Double, color: Color) {
    Column(modifier = modifier) {
        Text(
            label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        AnimatedCounter(value = value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun NavCard(modifier: Modifier, item: NavItem) {
    Card(
        modifier = modifier.clickable {
            item.onClick()
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = item.color.copy(0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.color.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, null, tint = item.color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                item.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Icon(
                Icons.Default.ArrowForward,
                null,
                tint = item.color,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun formatTxDate(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 60_000L -> "الآن"
        diff < 3_600_000L -> "${diff / 60_000} د"
        diff < 86_400_000L -> "${diff / 3_600_000} س"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(millis))
    }
}