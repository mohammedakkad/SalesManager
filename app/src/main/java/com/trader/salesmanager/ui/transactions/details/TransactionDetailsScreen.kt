package com.trader.salesmanager.ui.transactions.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.salesmanager.domain.model.Transaction
import com.trader.salesmanager.domain.repository.TransactionRepository
import com.trader.salesmanager.ui.theme.PaidGreen
import com.trader.salesmanager.ui.theme.UnpaidOrange
import com.trader.salesmanager.util.DateUtils.toDateTimeString
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    transactionId: Long,
    onNavigateUp: () -> Unit,
    onEdit: (Long) -> Unit,
    repo: TransactionRepository = koinInject()
) {
    var transaction by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(transactionId) {
        transaction = repo.getTransactionById(transactionId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("حذف العملية") },
            text = { Text("هل تريد حذف هذه العملية؟") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { transaction?.let { repo.deleteTransaction(it) } }
                    onNavigateUp()
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("إلغاء") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل العملية", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { onEdit(transactionId) }) { Icon(Icons.Default.Edit, null) }
                    IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        val t = transaction
        if (t == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailCard(label = "الزبون",         value = t.customerName)
                DetailCard(label = "المبلغ",          value = String.format("%.2f", t.amount), isBold = true)
                DetailCard(label = "طريقة الدفع",    value = t.paymentMethodName.ifEmpty { "غير محدد" })
                DetailCard(label = "التاريخ",         value = t.date.toDateTimeString())
                if (t.note.isNotEmpty()) DetailCard(label = "ملاحظة", value = t.note)
                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(
                    containerColor = if (t.isPaid) PaidGreen.copy(0.1f) else UnpaidOrange.copy(0.1f)
                )) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (t.isPaid) Icons.Default.CheckCircle else Icons.Default.Pending, null,
                            tint = if (t.isPaid) PaidGreen else UnpaidOrange)
                        Spacer(Modifier.width(12.dp))
                        Text(if (t.isPaid) "مدفوع" else "غير مدفوع",
                            fontWeight = FontWeight.Bold,
                            color = if (t.isPaid) PaidGreen else UnpaidOrange)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(label: String, value: String, isBold: Boolean = false) {
    Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        }
    }
}