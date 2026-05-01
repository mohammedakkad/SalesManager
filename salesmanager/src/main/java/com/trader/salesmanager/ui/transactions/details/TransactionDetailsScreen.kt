package com.trader.salesmanager.ui.transactions.details

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.core.data.local.appDataStore
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.Transaction
import com.trader.core.util.DateUtils.toDateTimeString
import com.trader.salesmanager.ui.components.StatusChip
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors
import com.trader.salesmanager.util.InvoiceSharer
import kotlinx.coroutines.flow.map
import com.trader.salesmanager.util.export.ExportTarget
import com.trader.salesmanager.util.export.ExportViewModel
import com.trader.salesmanager.util.export.ExportActionButton

import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    transactionId: Long,
    onNavigateUp: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: TransactionDetailsViewModel = koinViewModel { parametersOf(transactionId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current

    val storeName by context.appDataStore.data
    .map {
        it[com.trader.salesmanager.ui.settings.STORE_NAME_KEY] ?: ""
    }
    .collectAsState(initial = "")

    val exportVm: ExportViewModel = koinViewModel()
    val exportState by exportVm.state.collectAsState()

    // الانتقال للخلف فور اكتمال الحذف
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateUp()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            icon = {
                Icon(Icons.Rounded.DeleteForever, null, tint = DebtRed)
            },
            title = {
                Text("حذف العملية", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("هل تريد حذف هذه العملية؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DebtRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDeleteDialog = false
                }) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    val t = uiState.transaction
    if (t == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = Emerald500)
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().background(appColors.screenBackground)) {

        // ── Header ────────────────────────────────────────────────
        item {
            Box(
                Modifier.fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        if (t.isPaid) listOf(Emerald700, PaidGreen)
                        else listOf(Color(0xFFB45309), UnpaidAmber)
                    )
                )
                .padding(top = 48.dp, bottom = 28.dp, start = 20.dp, end = 20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                exportVm.exportInvoicePdf(
                                    transaction = t,
                                    items = uiState.invoiceItems,
                                    storeName = storeName,
                                    cacheDir = context.cacheDir
                                )
                            },
                            modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.15f))
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, null, tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                InvoiceSharer.shareInvoice(
                                    context = context,
                                    transaction = t,
                                    items = uiState.invoiceItems,
                                    storeName = storeName
                                )
                            },
                            modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.15f))
                        ) {
                            Icon(Icons.Rounded.Share, null, tint = Color.White)
                        }
                        IconButton(onClick = {
                            onEdit(transactionId)
                        }) {
                            Icon(Icons.Rounded.Edit, null, tint = Color.White)
                        }
                        IconButton(onClick = {
                            showDeleteDialog = true
                        }) {
                            Icon(Icons.Rounded.Delete, null, tint = Color.White.copy(0.8f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            Modifier.size(64.dp).clip(CircleShape)
                            .background(Color.White.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (t.isPaid) Icons.Rounded.CheckCircle else Icons.Rounded.PendingActions,
                                null, tint = Color.White, modifier = Modifier.size(32.dp)
                            )
                        }
                        Column {
                            Text(
                                "₪${String.format("%.2f", t.amount)}",
                                color = Color.White,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )
                            StatusChip(isPaid = t.isPaid)
                        }
                    }
                }
            }
        }

        // ── تفاصيل ────────────────────────────────────────────────
        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow(Icons.Rounded.Person, "الزبون", t.customerName, Emerald500)
                DetailRow(Icons.Rounded.CreditCard, "طريقة الدفع", t.paymentMethodName.ifEmpty {
                    "غير محدد"
                }, Cyan500)
                DetailRow(Icons.Rounded.CalendarToday, "التاريخ", t.date.toDateTimeString(), Violet500)
                val paymentTypeLabel = when (t.paymentType) {
                    com.trader.core.domain.model.PaymentType.CASH -> "كاش 💵"
                    com.trader.core.domain.model.PaymentType.DEBT -> "دين 📋"
                    com.trader.core.domain.model.PaymentType.BANK -> "بنك 🏦"
                    com.trader.core.domain.model.PaymentType.WALLET -> "محفظة 💳"
                    com.trader.core.domain.model.PaymentType.OTHER -> "أخرى 💰"
                }
                DetailRow(Icons.Rounded.Payment, "نوع الدفع", paymentTypeLabel, UnpaidAmber)
                if (t.note.isNotEmpty())
                    DetailRow(Icons.Rounded.Notes, "ملاحظة", t.note, Color(0xFF8B5CF6))
            }
        }

        // ── أصناف الفاتورة ────────────────────────────────────────
        if (uiState.invoiceItems.isNotEmpty()) {
            item {
                Text(
                    "أصناف الفاتورة",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = appColors.textSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        uiState.invoiceItems.forEachIndexed {
                            index, item ->
                            InvoiceItemRow(item)
                            if (index < uiState.invoiceItems.lastIndex)
                                HorizontalDivider(color = appColors.divider)
                        }
                        HorizontalDivider(color = appColors.border, thickness = 1.dp)
                        Column(Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val itemsTotal = uiState.invoiceItems.sumOf {
                                it.totalPrice
                            }
                            val baseAmount = t.amount - itemsTotal

                            // ✅ إذا يوجد مبلغ أساسي (عملية أُضيفت أصناف عليها لاحقاً)
                            // نُفصِّل المبلغين حتى يفهم التاجر التركيبة
                            if (baseAmount > 0.001) {
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("مجموع الأصناف",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = appColors.textSubtle)
                                    Text("₪${String.format("%.2f", itemsTotal)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = appColors.textSubtle)
                                }
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("مبلغ إضافي",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = appColors.textSubtle)
                                    Text("₪${String.format("%.2f", baseAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = appColors.textSubtle)
                                }
                                HorizontalDivider(color = appColors.divider,
                                    modifier = Modifier.padding(vertical = 2.dp))
                            }
                            // ✅ الإجمالي دائماً من t.amount — يشمل الأصناف + المبلغ الإضافي
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الإجمالي", fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text("₪${String.format("%.2f", t.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Violet500)
                            }
                        }
                    }
                }
            }
        }

        // ── زر المشاركة السفلي ────────────────────────────────────
        item {
            com.trader.salesmanager.util.export.ExportActionButton(
                target = com.trader.salesmanager.util.export.ExportTarget.INVOICE_PDF,
                state = exportState,
                onExport = {
                    exportVm.exportInvoicePdf(
                        transaction = t,
                        items = uiState.invoiceItems,
                        storeName = storeName,
                        cacheDir = context.cacheDir
                    )
                },
                onDismissError = exportVm::dismissError,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InvoiceItemRow(item: InvoiceItem) {
    Row(
        Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
            .background(Violet500.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                item.productName.firstOrNull()?.toString() ?: "؟",
                color = Violet500, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Column(Modifier.weight(1f)) {
            Text(item.productName, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall)
            val qtyStr = if (item.quantity == item.quantity.toLong().toDouble())
                item.quantity.toLong().toString()
            else String.format("%.3f", item.quantity).trimEnd('0').trimEnd('.')
            Text(
                "$qtyStr ${item.unitLabel}  ×  ₪${String.format("%.2f", item.pricePerUnit)}",
                style = MaterialTheme.typography.labelSmall,
                color = appColors.textSubtle
            )
        }
        Text(
            "₪${String.format("%.2f", item.totalPrice)}",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall,
            color = Violet500
        )
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, color: Color) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.06f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(color.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}