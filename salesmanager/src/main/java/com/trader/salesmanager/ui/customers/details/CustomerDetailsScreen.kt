package com.trader.salesmanager.ui.customers.details

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.salesmanager.domain.model.Transaction
import com.trader.salesmanager.ui.components.*
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.util.DateUtils.toDateString
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CustomerDetailsScreen(
    customerId: Long,
    onNavigateUp: () -> Unit,
    onEditCustomer: () -> Unit,
    onAddTransaction: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: CustomerDetailsViewModel = koinViewModel(parameters = { parametersOf(customerId) })
) {
    val uiState by viewModel.uiState.collectAsState()
    val name = uiState.customer?.name ?: ""
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = Emerald500,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Rounded.Add, null) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                // ── Header ─────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Emerald700, Cyan500)))
                        .padding(top = 48.dp, bottom = 28.dp, start = 20.dp, end = 20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = onEditCustomer) {
                                Icon(Icons.Rounded.Edit, null, tint = Color.White)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(initial, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text("${uiState.transactions.size} عملية", color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            item {
                // ── Debt Card ───────────────────────────────────
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.totalDebt > 0) DebtRed.copy(0.08f) else PaidGreen.copy(0.08f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (uiState.totalDebt > 0) DebtRed.copy(0.15f) else PaidGreen.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (uiState.totalDebt > 0) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                                null,
                                tint = if (uiState.totalDebt > 0) DebtRed else PaidGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (uiState.totalDebt > 0) "إجمالي الديون" else "لا توجد ديون",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AnimatedCounter(
                                value = uiState.totalDebt,
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (uiState.totalDebt > 0) DebtRed else PaidGreen
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "العمليات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            itemsIndexed(uiState.transactions, key = { _, t -> t.id }) { index, tx ->
                val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = visible,
                    enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(250, delayMillis = index * 40)) + fadeIn()
                ) {
                    TxMiniCard(tx = tx, onClick = { onTransactionClick(tx.id) }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }

            if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Receipt,
                        title = "لا توجد عمليات",
                        subtitle = "اضغط + لإضافة أول عملية"
                    )
                }
            }
        }
    }
}

@Composable
private fun TxMiniCard(tx: Transaction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.date.toDateString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (tx.paymentMethodName.isNotEmpty())
                    Text(tx.paymentMethodName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Text(String.format("%.2f", tx.amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            StatusChip(isPaid = tx.isPaid)
        }
    }
}
