package com.trader.salesmanager.ui.transactions.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AssignmentReturn
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.core.data.local.appDataStore
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.ReturnSummary
import com.trader.core.util.DateUtils.toDateTimeString
import com.trader.salesmanager.ui.components.StatusChip
import com.trader.salesmanager.ui.inventory.invoice.formatAmount
import com.trader.salesmanager.ui.inventory.invoice.formatQty
import com.trader.salesmanager.ui.theme.Cyan500
import com.trader.salesmanager.ui.theme.DebtRed
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.Emerald700
import com.trader.salesmanager.ui.theme.PaidGreen
import com.trader.salesmanager.ui.theme.UnpaidAmber
import com.trader.salesmanager.ui.theme.Violet500
import com.trader.salesmanager.ui.theme.appColors
import com.trader.salesmanager.util.InvoiceSharer
import com.trader.salesmanager.util.export.ExportManager
import com.trader.salesmanager.util.export.ExportState
import com.trader.salesmanager.util.export.ExportSuccessBottomSheet
import com.trader.salesmanager.util.export.ExportViewModel
import com.trader.salesmanager.util.pdf.PdfPrintManager
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    transactionId: Long,
    onNavigateUp: () -> Unit,
    onEdit: (Long) -> Unit,
    onNavigateToReturn: (Long) -> Unit,
    viewModel: TransactionDetailsViewModel = koinViewModel { parametersOf(transactionId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current

    val storeNameFlow = remember(context) {
        context.appDataStore.data.map {
            it[com.trader.salesmanager.ui.settings.STORE_NAME_KEY] ?: ""
        }
    }
    val storeName by storeNameFlow.collectAsState(initial = "")

    val exportVm: ExportViewModel = koinViewModel()
    val exportState by exportVm.state.collectAsStateWithLifecycle()
    var showExportSheet by remember {
        mutableStateOf(false)
    }
    var pendingExportAction by remember {
        mutableStateOf<PdfExportAction?>(null)
    }

    LaunchedEffect(exportState) {
        val success = exportState as? ExportState.Success ?: return@LaunchedEffect
        if (pendingExportAction == PdfExportAction.PRINT && success.type == com.trader.salesmanager.util.export.ExportType.PDF) {
            PdfPrintManager.print(context, java.io.File(success.filePath), success.fileName)
            pendingExportAction = null
            exportVm.reset()
        } else {
            showExportSheet = true
        }
    }

    if (showExportSheet && exportState is ExportState.Success) {
        val success = exportState as ExportState.Success
        val file = java.io.File(success.filePath)
        ExportSuccessBottomSheet(
            state = success,
            onShare = {
                ExportManager.shareFile(context, file, success.type.mimeType)
                showExportSheet = false
                pendingExportAction = null
                exportVm.reset()
            },
            onWhatsApp = {
                ExportManager.shareToWhatsApp(context, file, success.type.mimeType)
                showExportSheet = false
                pendingExportAction = null
                exportVm.reset()
            },
            onDownload = {
                ExportManager.saveToDownloads(context, file, success.fileName)
                showExportSheet = false
                pendingExportAction = null
                exportVm.reset()
            },
            onPrint = {
                PdfPrintManager.print(context, file, success.fileName)
                showExportSheet = false
                pendingExportAction = null
                exportVm.reset()
            },
            onDismiss = {
                showExportSheet = false
                pendingExportAction = null
                exportVm.reset()
            }
        )
    }

    // الانتقال للخلف فور اكتمال الحذف
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateUp()
    }

    // ✅ تحديث بيانات الإرجاع عند العودة لهذه الشاشة (بعد popBackStack)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.currentStateFlow.collect {
            state ->
            if (state == Lifecycle.State.RESUMED) {
                viewModel.refreshReturnSummary()
            }
        }
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
                        // ✅ Fix 4: دفاعي — يعتمد على hasItems أو وجود أصناف فعلية في الـ state
                        // يحمي الـ UI في حال كانت قيمة hasItems في DB خاطئة لأي سبب
                        val hasReturnableItems = t.hasItems || uiState.invoiceItems.isNotEmpty()
                        if (hasReturnableItems) {
                            IconButton(
                                onClick = {
                                    onNavigateToReturn(transactionId)
                                },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.15f))
                            ) {
                                Icon(
                                    Icons.Rounded.AssignmentReturn,
                                    contentDescription = "مرتجع",
                                    tint = Color.White
                                )
                            }
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
                            // ✅ المبلغ الأصلي مشطوب إذا حدث إرجاع
                            if (t.hasAnyReturn && t.amountChanged) {
                                Text(
                                    "₪${t.originalAmount.formatAmount()}",
                                    color = Color.White.copy(0.5f),
                                    style = MaterialTheme.typography.titleSmall,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                            Text(
                                "₪${t.amount.formatAmount()}",
                                color = Color.White,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusChip(isPaid = t.isPaid)
                                // ✅ Badge الإرجاع في الهيدر
                                if (t.isFullyReturned || t.isPartiallyReturned) {
                                    val returnColor = if (t.isFullyReturned) DebtRed else UnpaidAmber
                                    val returnText = if (t.isFullyReturned) "↩ مرتجع كامل"
                                    else "↩ مرتجع جزئي"
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = returnColor.copy(0.25f)
                                    ) {
                                        Text(
                                            returnText,
                                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
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
                            InvoiceItemRow(
                                item = item,
                                returnSummary = uiState.returnSummary
                            )
                            if (index < uiState.invoiceItems.lastIndex)
                                HorizontalDivider(color = appColors.divider)
                        }
                        HorizontalDivider(color = appColors.border, thickness = 1.dp)
                        Column(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val itemsTotal = uiState.invoiceItems.sumOf {
                                it.totalPrice
                            }
                            val baseAmount = (t.originalAmount - itemsTotal).coerceAtLeast(0.0)
                            val hasReturn = uiState.returnSummary.totalRefunded > 0

                            // مبلغ إضافي (عملية بدون أصناف أُضيفت لاحقاً)
                            if (baseAmount > 0.001) {
                                TotalsRow("مجموع الأصناف",
                                    "₪${itemsTotal.formatAmount()}", appColors.textSubtle)
                                TotalsRow("مبلغ إضافي",
                                    "₪${baseAmount.formatAmount()}", appColors.textSubtle)
                                HorizontalDivider(color = appColors.divider,
                                    modifier = Modifier.padding(vertical = 2.dp))
                            }

                            // ✅ قسم الإرجاع — مُفصَّل وواضح
                            if (hasReturn) {
                                // إجمالي قبل الإرجاع
                                TotalsRow(
                                    label = "إجمالي الأصناف",
                                    value = "₪${itemsTotal.formatAmount()}",
                                    valueColor = appColors.textSubtle,
                                    strikethrough = true
                                )
                                // المبلغ المرتجع — الكمية والمبلغ منفصلان
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Rounded.Undo, null,
                                            tint = DebtRed, modifier = Modifier.size(14.dp))
                                        Text("مرتجع",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DebtRed)
                                    }
                                    Text(
                                        // ✅ فصل الكمية عن العملة
                                        "- ₪${uiState.returnSummary.totalRefunded.formatAmount()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DebtRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                HorizontalDivider(color = appColors.divider,
                                    modifier = Modifier.padding(vertical = 2.dp))
                            }

                            // ✅ الإجمالي النهائي دائماً من t.amount
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الإجمالي",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "₪${t.amount.formatAmount()}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Violet500
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            val isExporting = exportState is ExportState.Loading
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        pendingExportAction = PdfExportAction.SHARE
                        exportVm.exportInvoicePdf(
                            transaction = t,
                            items = uiState.invoiceItems,
                            storeName = storeName,
                            customer = uiState.customer,
                            priorDebtBalance = uiState.priorDebtBalance,
                            currentDebtBalance = uiState.currentDebtBalance,
                            cacheDir = context.cacheDir
                        )
                    },
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    if (isExporting && pendingExportAction == PdfExportAction.SHARE) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Share, null, Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("مشاركة PDF", fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            pendingExportAction = PdfExportAction.PRINT
                            exportVm.exportInvoicePdf(
                                transaction = t,
                                items = uiState.invoiceItems,
                                storeName = storeName,
                                customer = uiState.customer,
                                priorDebtBalance = uiState.priorDebtBalance,
                                currentDebtBalance = uiState.currentDebtBalance,
                                cacheDir = context.cacheDir
                            )
                        },
                        enabled = !isExporting,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isExporting && pendingExportAction == PdfExportAction.PRINT) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Print, null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("طباعة")
                    }
                    OutlinedButton(
                        onClick = {
                            InvoiceSharer.shareInvoice(
                                context = context,
                                transaction = t,
                                items = uiState.invoiceItems,
                                storeName = storeName
                            )
                        },
                        enabled = !isExporting,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Chat, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("مشاركة نصية سريعة", maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private enum class PdfExportAction {
    SHARE,
    PRINT
}

@Composable
private fun InvoiceItemRow(item: InvoiceItem, returnSummary: ReturnSummary) {
    val returnedQty = returnSummary.returnedByUnit[item.unitId] ?: 0.0
    val isFullReturn = returnedQty >= item.quantity && returnedQty > 0
    val isPartReturn = returnedQty > 0 && returnedQty < item.quantity
    val netQty = (item.quantity - returnedQty).coerceAtLeast(0.0)

    Row(
        Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // أيقونة الصنف
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
            .background(if (isFullReturn) DebtRed.copy(0.1f) else Violet500.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(item.productName.firstOrNull()?.toString() ?: "؟",
                color = if (isFullReturn) DebtRed else Violet500,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall)
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // اسم الصنف + badge الإرجاع
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.productName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFullReturn) appColors.textSubtle else appColors.textPrimary)

                // ✅ Badge حالة الإرجاع
                if (isFullReturn || isPartReturn) {
                    val badgeColor = if (isFullReturn) DebtRed else UnpaidAmber
                    val badgeText = if (isFullReturn) "مرتجع كامل"
                    else "أُرجع ${returnedQty.formatQty()}"
                    Surface(shape = RoundedCornerShape(20.dp), color = badgeColor.copy(0.12f)) {
                        Text(badgeText,
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ✅ الكمية: مشطوبة إذا تغيّرت + الكمية المتبقية
            if (isFullReturn || isPartReturn) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    // الكمية الأصلية مشطوبة
                    Text(
                        "${item.quantity.formatQty()} ${item.unitLabel}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            textDecoration = TextDecoration.LineThrough
                        ),
                        color = appColors.textSubtle
                    )
                    if (!isFullReturn) {
                        Icon(Icons.Rounded.ArrowForward, null,
                            tint = appColors.textSubtle, modifier = Modifier.size(10.dp))
                        Text("${netQty.formatQty()} ${item.unitLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = appColors.textPrimary,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Text("× ₪${item.pricePerUnit.formatAmount()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.textSubtle)
                }
            } else {
                Text("${item.quantity.formatQty()} ${item.unitLabel}  ×  ₪${item.pricePerUnit.formatAmount()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.textSubtle)
            }
        }

        // ✅ السعر الصافي — مشطوب إذا تغيّر
        Column(horizontalAlignment = Alignment.End) {
            if (isPartReturn) {
                Text("₪${item.totalPrice.formatAmount()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = appColors.textSubtle)
                Text("₪${(netQty * item.pricePerUnit).formatAmount()}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = UnpaidAmber)
            } else {
                Text("₪${item.totalPrice.formatAmount()}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFullReturn) appColors.textSubtle else Violet500,
                    textDecoration = if (isFullReturn) TextDecoration.LineThrough else null)
            }
        }
    }
}

@Composable
private fun TotalsRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    strikethrough: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label,
            style = MaterialTheme.typography.bodySmall,
            color = appColors.textSubtle)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            textDecoration = if (strikethrough) TextDecoration.LineThrough else null
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
                // Label — uses onSurfaceVariant for a clear secondary hierarchy in both themes
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                // Value — uses the semantic appColors.textPrimary which correctly maps to
                // DarkOnSurface (#E8EDF2) in dark mode for high contrast readability
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = appColors.textPrimary
                )
            }
        }
    }
}