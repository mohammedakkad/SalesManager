package com.trader.salesmanager.ui.customers.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.salesmanager.domain.model.Transaction
import com.trader.salesmanager.ui.theme.DebtRed
import com.trader.salesmanager.ui.theme.PaidGreen
import com.trader.salesmanager.ui.theme.UnpaidOrange
import com.trader.salesmanager.util.DateUtils.toDateString
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.customer?.name ?: "...", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = onEditCustomer) { Icon(Icons.Default.Edit, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = "إضافة عملية")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // ── Debt Card ──
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (uiState.totalDebt > 0) DebtRed.copy(0.1f) else PaidGreen.copy(0.1f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (uiState.totalDebt > 0) Icons.Default.Warning else Icons.Default.CheckCircle, null,
                                tint = if (uiState.totalDebt > 0) DebtRed else PaidGreen, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("إجمالي الديون", style = MaterialTheme.typography.labelMedium)
                                Text(String.format("%.2f", uiState.totalDebt), style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold, color = if (uiState.totalDebt > 0) DebtRed else PaidGreen)
                            }
                        }
                    }
                }
                item { Text("العمليات (${uiState.transactions.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                if (uiState.transactions.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد عمليات بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(uiState.transactions, key = { it.id }) { t ->
                    TransactionMiniCard(transaction = t, onClick = { onTransactionClick(t.id) })
                }
            }
        }
    }
}

@Composable
private fun TransactionMiniCard(transaction: Transaction, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), onClick = onClick, elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.date.toDateString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format("%.2f", transaction.amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                if (transaction.paymentMethodName.isNotEmpty())
                    Text(transaction.paymentMethodName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Chip(isPaid = transaction.isPaid)
        }
    }
}

@Composable
private fun Chip(isPaid: Boolean) {
    Surface(shape = RoundedCornerShape(20.dp), color = if (isPaid) PaidGreen.copy(0.15f) else UnpaidOrange.copy(0.15f)) {
        Text(
            text = if (isPaid) "مدفوع" else "غير مدفوع",
            color = if (isPaid) PaidGreen else UnpaidOrange,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}