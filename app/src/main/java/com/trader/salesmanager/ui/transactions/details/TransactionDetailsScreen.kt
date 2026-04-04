package com.trader.salesmanager.ui.transactions.details

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.salesmanager.domain.model.Transaction
import com.trader.salesmanager.domain.repository.TransactionRepository
import com.trader.salesmanager.ui.components.StatusChip
import com.trader.salesmanager.ui.theme.*
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
    var tx by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(transactionId) { tx = repo.getTransactionById(transactionId) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Rounded.DeleteForever, null, tint = DebtRed) },
            title = { Text("حذف العملية", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد حذف هذه العملية نهائياً؟") },
            confirmButton = {
                Button(
                    onClick = { scope.launch { tx?.let { repo.deleteTransaction(it) } }; onNavigateUp() },
                    colors = ButtonDefaults.buttonColors(containerColor = DebtRed)
                ) { Text("حذف") }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteDialog = false }) { Text("إلغاء") } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold { padding ->
        tx?.let { t ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                if (t.isPaid) listOf(Emerald700, PaidGreen) else listOf(Color(0xFFB45309), UnpaidAmber)
                            )
                        )
                        .padding(top = 48.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateUp) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { onEdit(transactionId) }) { Icon(Icons.Rounded.Edit, null, tint = Color.White) }
                            IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Rounded.Delete, null, tint = Color.White.copy(0.8f)) }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (t.isPaid) Icons.Rounded.CheckCircle else Icons.Rounded.PendingActions,
                                    null, tint = Color.White, modifier = Modifier.size(32.dp)
                                )
                            }
                            Column {
                                Text(String.format("%.2f", t.amount), color = Color.White, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                                StatusChip(isPaid = t.isPaid)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailRow(Icons.Rounded.Person, "الزبون", t.customerName, Emerald500)
                    DetailRow(Icons.Rounded.Payment, "طريقة الدفع", t.paymentMethodName.ifEmpty { "غير محدد" }, Cyan500)
                    DetailRow(Icons.Rounded.CalendarToday, "التاريخ", t.date.toDateTimeString(), Violet500)
                    if (t.note.isNotEmpty())
                        DetailRow(Icons.Rounded.Notes, "ملاحظة", t.note, UnpaidAmber)
                }
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Emerald500)
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.06f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
