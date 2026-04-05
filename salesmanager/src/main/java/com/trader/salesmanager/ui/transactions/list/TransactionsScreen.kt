package com.trader.salesmanager.ui.transactions.list

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.salesmanager.domain.model.Transaction
import com.trader.salesmanager.ui.components.*
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.util.DateUtils.toDateString
import org.koin.androidx.compose.koinViewModel

@Composable
fun TransactionsScreen(
    onNavigateUp: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: TransactionsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = Cyan500,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Rounded.Add, null) }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Header ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Cyan500, Emerald500)))
                    .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("العمليات", style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${uiState.transactions.size} عملية", color = Color.White.copy(0.7f),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // ── Filter Chips ─────────────────────────────
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to "الكل", true to "مدفوع", false to "غير مدفوع").forEach { (value, label) ->
                            val selected = uiState.filterPaid == value
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setFilter(value) },
                                label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.White,
                                    selectedLabelColor = Emerald700,
                                    containerColor = Color.White.copy(0.2f),
                                    labelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true, selected = selected,
                                    borderColor = Color.White.copy(0.3f),
                                    selectedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }

            // ── List ─────────────────────────────────────────────
            AnimatedContent(
                targetState = uiState.transactions.isEmpty(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "list"
            ) { empty ->
                if (empty) {
                    EmptyState(
                        icon = Icons.Rounded.Receipt,
                        title = "لا توجد عمليات",
                        subtitle = "اضغط + لإضافة عملية جديدة",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(uiState.transactions, key = { _, t -> t.id }) { index, tx ->
                            val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                            AnimatedVisibility(
                                visibleState = visible,
                                enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300, delayMillis = index * 40)) + fadeIn()
                            ) {
                                TransactionCard(tx = tx, onClick = { onTransactionClick(tx.id) })
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(tx: Transaction, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Amount circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        Brush.radialGradient(
                            if (tx.isPaid) listOf(PaidGreen.copy(0.2f), PaidGreen.copy(0.05f))
                            else listOf(UnpaidAmber.copy(0.2f), UnpaidAmber.copy(0.05f))
                        ),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (tx.isPaid) Icons.Rounded.CheckCircle else Icons.Rounded.PendingActions,
                    null,
                    tint = if (tx.isPaid) PaidGreen else UnpaidAmber,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.customerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tx.date.toDateString(), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (tx.paymentMethodName.isNotEmpty()) {
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text(tx.paymentMethodName, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    String.format("%.2f", tx.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (tx.isPaid) PaidGreen else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                StatusChip(isPaid = tx.isPaid)
            }
        }
    }
}
