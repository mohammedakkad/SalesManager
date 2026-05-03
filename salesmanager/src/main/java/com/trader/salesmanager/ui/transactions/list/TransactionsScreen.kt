package com.trader.salesmanager.ui.transactions.list

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.text.style.TextDecoration
import com.trader.core.domain.model.TransactionReturnStatus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.core.domain.model.Transaction
import com.trader.core.util.DateUtils.toDateString
import com.trader.salesmanager.ui.components.StatusChip
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors
import com.trader.core.domain.model.SyncStatus
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
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = Cyan500, contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, null)
            }
        }
    ) {
            padding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Cyan500, Emerald500)))
                    .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "العمليات",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = Color.White
                            )
                            // FIX: was \${...} which shows as literal text — removed backslash
                            Text(
                                "${uiState.transactions.size} عملية",
                                color = Color.White.copy(0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to "الكل", true to "مدفوع", false to "غير مدفوع").forEach {
                                (value, label) ->
                            val selected = uiState.filterPaid == value
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    viewModel.setFilter(value)
                                },
                                label = {
                                    Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                },
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

            AnimatedContent(
                targetState = uiState.transactions.isEmpty(),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "list"
            ) {
                    empty ->
                if (empty) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Receipt, null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("لا توجد عمليات", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(uiState.transactions, key = {
                                _, t -> t.id
                        }) {
                                index, tx ->
                            val visible = remember {
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
                                    animationSpec = tween(300, delayMillis = index * 40)
                                ) + fadeIn()
                            ) {
                                TransactionCard(tx = tx, onClick = {
                                    onTransactionClick(tx.id)
                                })
                            }
                        }
                        item {
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(tx: Transaction, onClick: () -> Unit) {
    val isPending       = tx.syncStatus == SyncStatus.PENDING
    val isFullReturn    = tx.returnStatus == TransactionReturnStatus.FULLY_RETURNED
    val isPartReturn    = tx.returnStatus == TransactionReturnStatus.PARTIALLY_RETURNED
    val hasReturn       = isFullReturn || isPartReturn

    // ألوان الخلفية: رمادي للمرتجع كلياً، اعتيادي للبقية
    val iconBg = when {
        isFullReturn -> listOf(DebtRed.copy(0.15f), DebtRed.copy(0.05f))
        isPartReturn -> listOf(UnpaidAmber.copy(0.2f), UnpaidAmber.copy(0.05f))
        tx.isPaid    -> listOf(PaidGreen.copy(0.2f),   PaidGreen.copy(0.05f))
        else         -> listOf(UnpaidAmber.copy(0.2f), UnpaidAmber.copy(0.05f))
    }
    val iconTint = when {
        isFullReturn -> DebtRed
        isPartReturn -> UnpaidAmber
        tx.isPaid    -> PaidGreen
        else         -> UnpaidAmber
    }
    val icon = when {
        isFullReturn -> Icons.Rounded.Undo
        isPartReturn -> Icons.Rounded.AssignmentReturn
        tx.isPaid    -> Icons.Rounded.CheckCircle
        else         -> Icons.Rounded.PendingActions
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(if (isFullReturn) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFullReturn)
                MaterialTheme.colorScheme.surface.copy(0.6f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).background(
                    Brush.radialGradient(iconBg), RoundedCornerShape(14.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(tx.customerName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, false),
                        color = if (isFullReturn) appColors.textSubtle
                        else MaterialTheme.colorScheme.onSurface)

                    // Badge مزامنة
                    if (isPending) {
                        Surface(shape = RoundedCornerShape(20.dp), color = UnpaidAmber.copy(0.15f)) {
                            Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Rounded.CloudOff, null, Modifier.size(10.dp), tint = UnpaidAmber)
                                Text("جاري المزامنة", fontSize = 9.sp, color = UnpaidAmber, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // ✅ Badge الإرجاع
                    if (isFullReturn || isPartReturn) {
                        val returnColor = if (isFullReturn) DebtRed else UnpaidAmber
                        val returnLabel = if (isFullReturn) "↩ مرتجع" else "↩ جزئي"
                        Surface(shape = RoundedCornerShape(20.dp), color = returnColor.copy(0.12f)) {
                            Text(returnLabel,
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                color = returnColor,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(tx.date.toDateString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (tx.paymentMethodName.isNotEmpty()) {
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                        Text(tx.paymentMethodName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // ✅ المبلغ — مشطوب إذا تغيّر + المبلغ الجديد
            Column(horizontalAlignment = Alignment.End) {
                if (hasReturn && tx.amountChanged) {
                    Text(
                        "₪${String.format("%.2f", tx.originalAmount)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDecoration = TextDecoration.LineThrough
                        ),
                        color = appColors.textSubtle
                    )
                }
                Text(
                    "₪${String.format("%.2f", tx.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isFullReturn -> appColors.textSubtle
                        isPartReturn -> UnpaidAmber
                        tx.isPaid    -> PaidGreen
                        else         -> MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (isFullReturn) TextDecoration.LineThrough else null
                )
                Spacer(Modifier.height(4.dp))
                if (!hasReturn) StatusChip(isPaid = tx.isPaid)
            }
        }
    }
}