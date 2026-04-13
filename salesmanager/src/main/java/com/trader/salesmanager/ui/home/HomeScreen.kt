package com.trader.salesmanager.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.trader.core.domain.model.Transaction
import com.trader.salesmanager.ui.components.AnimatedCounter
import com.trader.salesmanager.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

private val HEADER_CONTENT_HEIGHT = 140.dp
private val OVERLAP = 40.dp

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
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = onAddTransaction,
                icon           = { Icon(Icons.Rounded.Add, null) },
                text           = { Text("عملية جديدة", fontWeight = FontWeight.SemiBold) },
                containerColor = Emerald500,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // ── Header + Stats Card ──────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HEADER_CONTENT_HEIGHT + OVERLAP)
                        .background(
                            Brush.linearGradient(
                                listOf(Emerald700, Cyan500),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end   = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                )
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 20.dp, end = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("مرحباً 👋",
                                color = Color.White.copy(0.8f),
                                style = MaterialTheme.typography.bodyLarge)
                            Text("مدير المبيعات",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (uiState.unreadChatCount > 99) "99+"
                                            else "${uiState.unreadChatCount}",
                                            color  = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick  = onNavigateToSettings,
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
                        shape  = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("إجمالي اليوم",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                MiniStat(Modifier.weight(1f), "مدفوع",     uiState.todayPaid,   PaidGreen)
                                VerticalDivider(Modifier.height(40.dp), color = MaterialTheme.colorScheme.outline.copy(0.3f))
                                MiniStat(Modifier.weight(1f), "غير مدفوع", uiState.todayUnpaid, UnpaidAmber)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(OVERLAP / 2 + 24.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // ── Quick Nav ────────────────────────────────────
                Text("القوائم",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp))

                val navItems = listOf(
                    NavItem("الزبائن",  Icons.Rounded.People,   Emerald500, onNavigateToCustomers),
                    NavItem("العمليات", Icons.Rounded.Receipt,  Cyan500,    onNavigateToTransactions),
                    NavItem("التقارير", Icons.Rounded.BarChart, Violet500,  onNavigateToReports),
                    NavItem("الديون",   Icons.Rounded.Warning,  DebtRed,    onNavigateToDebts),
                    NavItem("المخزن",   Icons.Rounded.Inventory2, Cyan500,  onNavigateToInventory),
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    navItems.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { NavCard(Modifier.weight(1f), it) }
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
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("آخر العمليات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        TextButton(onClick = onNavigateToTransactions) {
                            Text("عرض الكل", color = Emerald500,
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.recentTransactions.forEachIndexed { index, tx ->
                            RecentTransactionCard(
                                tx      = tx,
                                index   = index,
                                onClick = { onTransactionClick(tx.id) }
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
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visible,
        enter = slideInVertically(tween(300, index * 60)) { it / 2 } + fadeIn(tween(300, index * 60))
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            onClick   = onClick,
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        tint   = if (tx.isPaid) PaidGreen else UnpaidAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
                // اسم الزبون + وقت
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tx.customerName.ifEmpty { "—" },
                        style    = MaterialTheme.typography.bodyMedium,
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
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = if (tx.isPaid) PaidGreen else UnpaidAmber
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (tx.isPaid) PaidGreen.copy(0.12f) else UnpaidAmber.copy(0.12f)
                    ) {
                        Text(
                            if (tx.isPaid) "مدفوع" else "معلق",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = if (tx.isPaid) PaidGreen else UnpaidAmber,
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
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        AnimatedCounter(value = value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

private data class NavItem(val label: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

@Composable
private fun NavCard(modifier: Modifier, item: NavItem) {
    Card(
        modifier = modifier.clickable { item.onClick() },
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = item.color.copy(0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(item.color.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(item.icon, null, tint = item.color, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(item.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Icon(Icons.Default.ArrowForward, null, tint = item.color, modifier = Modifier.size(14.dp))
        }
    }
}

private fun formatTxDate(millis: Long): String {
    val now  = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 60_000L              -> "الآن"
        diff < 3_600_000L           -> "${diff / 60_000} د"
        diff < 86_400_000L          -> "${diff / 3_600_000} س"
        else                        -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(millis))
    }
}
