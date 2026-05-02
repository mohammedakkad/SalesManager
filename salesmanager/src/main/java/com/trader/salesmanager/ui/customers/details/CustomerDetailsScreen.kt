package com.trader.salesmanager.ui.customers.details

import android.content.Intent
import android.net.Uri
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.Transaction
import com.trader.salesmanager.ui.components.*
import com.trader.salesmanager.ui.theme.*
import com.trader.core.util.DateUtils.toDateString
import com.trader.salesmanager.util.export.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import com.trader.core.data.local.appDataStore

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
val context = LocalContext.current
val name = uiState.customer?.name ?: ""
val phone = uiState.customer?.phone ?: ""
val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

// clean phone: keep digits only, strip leading 0
val rawPhone = phone.filter {
    it.isDigit()
}.trimStart('0')
val storeName by context.appDataStore.data
.map {
    it[com.trader.salesmanager.ui.settings.STORE_NAME_KEY] ?: ""
}
.collectAsState(initial = "")

val exportVm: ExportViewModel = koinViewModel()
val exportState by exportVm.state.collectAsState()
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
            ExportManager.shareFile(context, file, success.type.mimeType); showExportSheet = false
        },
        onWhatsApp = {
            ExportManager.shareToWhatsApp(context, file, success.type.mimeType); showExportSheet = false
        },
        onDownload = {
            ExportManager.saveToDownloads(context, file, success.fileName); showExportSheet = false; exportVm.reset()
        },
        onDismiss = {
            showExportSheet = false; exportVm.reset()
        }
    )
}

Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
        FloatingActionButton(
            onClick = onAddTransaction,
            containerColor = Emerald500, contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.Add, null)
        }
    }
) {
    padding ->
    LazyColumn(
        modifier = Modifier
        .fillMaxSize()
        .padding(bottom = padding.calculateBottomPadding()),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            // ── Header gradient ─────────────────────────────
            Box(
                modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Emerald700, Cyan500)))
                .padding(top = 48.dp, bottom = 28.dp, start = 20.dp, end = 20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Spacer(Modifier.weight(1f))
                        // ✅ إخفاء زر التعديل للزبون الزائر (id = -1)
                        if (customerId != -1L) {
                            IconButton(onClick = onEditCustomer) {
                                Icon(Icons.Rounded.Edit, null, tint = Color.White)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                initial,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                name,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${uiState.transactions.size} عملية",
                                color = Color.White.copy(0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (phone.isNotEmpty())
                                Text(
                                phone,
                                color = Color.White.copy(0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // ── WhatsApp buttons (only when phone exists) ─────────
        if (rawPhone.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                // ── Direct call ─────────────────────────────
                Row(
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Direct call — no prefix, uses raw number
                    Button(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:$phone")
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A73E8)
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Call, null,
                            tint = Color.White, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "اتصال مباشر",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── WhatsApp buttons ─────────────────────────
                Row(
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WhatsAppButton(
                        label = "واتساب 972",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val url = "https://wa.me/972$rawPhone"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    )
                    WhatsAppButton(
                        label = "واتساب 970",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val url = "https://wa.me/970$rawPhone"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    )
                }
            }
        }

        item {
            uiState.customer?.let {
                customer ->
                val isExporting = exportState is ExportState.Loading
                Surface(
                    onClick = {
                        if (!isExporting) {
                            exportVm.exportCustomerStatementPdf(
                                customer = customer,
                                transactions = uiState.transactions,
                                storeName = storeName,
                                cacheDir = context.cacheDir
                            )
                        }
                    },
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Violet500.copy(if (isExporting) 0.06f else 0.08f),
                    border = BorderStroke(1.dp, Violet500.copy(if (isExporting) 0.2f else 0.3f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                            .background(Violet500.copy(0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(Modifier.size(18.dp), color = Violet500, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.PictureAsPdf, null, tint = Violet500, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isExporting) "جارٍ تحضير الكشف..." else "كشف حساب PDF",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Violet500
                            )
                            Text(
                                "كل العمليات مع ملخص الديون",
                                style = MaterialTheme.typography.bodySmall,
                                color = appColors.textSubtle
                            )
                        }
                        if (!isExporting) {
                            Icon(Icons.Rounded.ChevronRight, null,
                                tint = Violet500.copy(0.6f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        item {
            // ── Debt Card ────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.totalDebt > 0) DebtRed.copy(0.08f) else PaidGreen.copy(
                        0.08f
                    )
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (uiState.totalDebt > 0) DebtRed.copy(0.15f) else PaidGreen.copy(
                                0.15f
                            )
                        ),
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
                "العمليات", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(10.dp))
        }

        itemsIndexed(uiState.transactions, key = {
            _, t -> t.id
        }) {
            index, tx ->
            val visible =
            remember {
                MutableTransitionState(false).apply {
                    targetState = true
                }
            }
            AnimatedVisibility(
                visibleState = visible,
                enter = slideInVertically(
                    initialOffsetY = {
                        it / 2
                    },
                    animationSpec = tween(250, delayMillis = index * 40)
                ) + fadeIn()
            ) {
                TxMiniCard(
                    tx = tx, onClick = {
                        onTransactionClick(tx.id)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
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
private fun WhatsAppButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
Button(
onClick = onClick,
modifier = modifier.height(44.dp),
colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
shape = RoundedCornerShape(12.dp)
) {
Icon(Icons.Rounded.Chat, null, modifier = Modifier.size(16.dp))
Spacer(Modifier.width(6.dp))
Text(label, style = MaterialTheme.typography.labelMedium)
}
}

@Composable
private fun TxMiniCard(tx: Transaction, onClick: () -> Unit, modifier: Modifier = Modifier) {
Card(
modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), onClick = onClick,
elevation = CardDefaults.cardElevation(1.dp)
) {
Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
Column(modifier = Modifier.weight(1f)) {
Text(
tx.date.toDateString(),
style = MaterialTheme.typography.labelSmall,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
if (tx.paymentMethodName.isNotEmpty())
Text(
tx.paymentMethodName,
style = MaterialTheme.typography.bodySmall,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
}
Spacer(Modifier.width(12.dp))
Text(
String.format("%.2f", tx.amount),
style = MaterialTheme.typography.titleSmall,
fontWeight = FontWeight.Bold
)
Spacer(Modifier.width(8.dp))
StatusChip(isPaid = tx.isPaid)
}
}
}