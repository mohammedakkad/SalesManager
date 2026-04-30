package com.trader.salesmanager.ui.inventory.session

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InventorySessionScreen(
    onNavigateUp: () -> Unit,
    viewModel: InventorySessionViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val items by viewModel.sessionItems.collectAsState()

    var showFinishDialog by remember {
        mutableStateOf(false)
    }
    var showCancelDialog by remember {
        mutableStateOf(false)
    }
    var editingItem by remember {
        mutableStateOf<InventorySessionItem?>(null)
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = {
                showFinishDialog = false
            },
            icon = {
                Icon(Icons.Rounded.Inventory2, null, tint = Emerald500)
            },
            title = {
                Text("إنهاء الجرد", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سيتم تطبيق التعديلات على المخزن:")
                    Surface(shape = RoundedCornerShape(12.dp), color = Emerald500.copy(0.08f)) {
                        Row(Modifier
                            .fillMaxWidth()
                            .padding(12.dp), Arrangement.SpaceEvenly) {
                            StatItem("${viewModel.countedItems}", "تم عدّه", Emerald500)
                            StatItem(
                                "${viewModel.totalItems - viewModel.countedItems}",
                                "لم يُعدّ",
                                appColors.textSubtle
                            )
                            StatItem("${viewModel.adjustmentsCount}", "تعديل", UnpaidAmber)
                        }
                    }
                    if (viewModel.totalItems - viewModel.countedItems > 0)
                        Text(
                            "⚠️ الأصناف غير المعدودة لن يتم تعديل كمياتها.",
                            style = MaterialTheme.typography.bodySmall, color = UnpaidAmber
                        )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishSession(); showFinishDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                    enabled = !state.isFinishing
                ) {
                    if (state.isFinishing) CircularProgressIndicator(
                        Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    else Text("تطبيق وإنهاء")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showFinishDialog = false
                }) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = {
                showCancelDialog = false
            },
            icon = {
                Icon(Icons.Rounded.Cancel, null, tint = DebtRed)
            },
            title = {
                Text("إلغاء الجرد", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("سيتم إلغاء جلسة الجرد الحالية دون تطبيق أي تغييرات.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelSession(); showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DebtRed)
                ) {
                    Text("إلغاء الجرد")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showCancelDialog = false
                }) {
                    Text("رجوع")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    editingItem?.let { item ->
        CountInputDialog(
            item = item,
            onConfirm = { qty ->
                viewModel.updateItemCount(item, qty); editingItem = null
            },
            onClear = {
                viewModel.clearItemCount(item); editingItem = null
            },
            onDismiss = {
                editingItem = null
            }
        )
    }

    Scaffold(containerColor = appColors.screenBackground) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(bottom = padding.calculateBottomPadding())) {

            // ── Header ──────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0F766E), Emerald500)))
                    .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Text(
                            "جرد المخزون", fontWeight = FontWeight.Bold,
                            color = Color.White, style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.activeSession != null) {
                            IconButton(
                                onClick = {
                                    showCancelDialog = true
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.15f))
                            ) {
                                Icon(Icons.Rounded.Cancel, null, tint = Color.White)
                            }
                        }
                    }

                    state.activeSession?.let {
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(
                                    "التقدم: ${viewModel.countedItems} / ${viewModel.totalItems}",
                                    color = Color.White.copy(0.9f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "${(viewModel.progressPercent * 100).toInt()}%",
                                    color = Color.White, fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            LinearProgressIndicator(
                                progress = {
                                    viewModel.progressPercent
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color.White, trackColor = Color.White.copy(0.3f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.searchQuery, onValueChange = viewModel::setSearch,
                            placeholder = {
                                Text("بحث بالاسم...", color = Color.White.copy(0.5f))
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.7f))
                            },
                            trailingIcon = {
                                Row {
                                    if (state.searchQuery.isNotEmpty())
                                        IconButton(onClick = {
                                            viewModel.setSearch("")
                                        }) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                null,
                                                tint = Color.White.copy(0.7f)
                                            )
                                        }
                                    IconButton(onClick = viewModel::togglePendingOnly) {
                                        Icon(
                                            Icons.Rounded.FilterList, null,
                                            tint = if (state.showOnlyPending) UnpaidAmber else Color.White.copy(
                                                0.7f
                                            )
                                        )
                                    }
                                }
                            },
                            singleLine = true, shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White.copy(0.5f),
                                unfocusedBorderColor = Color.White.copy(0.25f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(0.1f),
                                unfocusedContainerColor = Color.White.copy(0.07f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Emerald500)
                }

                state.activeSession == null -> NoActiveSession(
                    pastSessions = state.pastSessions,
                    onStart = viewModel::startNewSession
                )

                else -> {
                    val filtered = items.filter { item ->
                        val matchQuery = state.searchQuery.isEmpty() ||
                                item.productName.contains(state.searchQuery, ignoreCase = true)
                        val matchPending =
                            if (state.showOnlyPending) item.actualQuantity == null else true
                        matchQuery && matchPending
                    }

                    LazyColumn(
                        Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.showOnlyPending && filtered.isEmpty()) {
                            item {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            null,
                                            Modifier.size(56.dp),
                                            tint = Emerald500
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "جميع الأصناف تم عدّها ✅",
                                            color = Emerald500,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        val grouped = filtered.groupBy {
                            it.productName
                        }
                        grouped.forEach { (productName, productItems) ->
                            item(key = "header_$productName") {
                                Text(
                                    productName, style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold, color = appColors.textSecondary,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                                )
                            }
                            items(productItems, key = { "${it.productId}_${it.unitId}" }) { item ->
                                InventoryItemCard(item = item, onClick = {
                                    editingItem = item
                                })
                            }
                        }
                        item {
                            Spacer(Modifier.height(80.dp))
                        }
                    }

                    Surface(Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = Color.White) {
                        Button(
                            onClick = {
                                showFinishDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            enabled = !state.isFinishing
                        ) {
                            Icon(Icons.Rounded.Done, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("إنهاء الجرد وتطبيق التعديلات", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryItemCard(item: InventorySessionItem, onClick: () -> Unit) {
    val isCounted = item.actualQuantity != null
    val diff = item.difference
    val hasDiff = isCounted && kotlin.math.abs(diff) > 0.001

    Card(
        onClick = onClick, shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCounted && hasDiff -> UnpaidAmber.copy(0.06f)
                isCounted -> PaidGreen.copy(0.04f)
                else -> Color.White
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCounted && hasDiff -> UnpaidAmber.copy(0.15f)
                            isCounted -> PaidGreen.copy(0.15f)
                            else -> appColors.cardBackgroundVariant
                        }
                    ), contentAlignment = Alignment.Center
            ) {
                Icon(
                    when {
                        isCounted && hasDiff -> Icons.Rounded.SwapVert
                        isCounted -> Icons.Rounded.CheckCircle
                        else -> Icons.Rounded.RadioButtonUnchecked
                    },
                    null,
                    tint = when {
                        isCounted && hasDiff -> UnpaidAmber; isCounted -> PaidGreen; else -> appColors.divider
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    item.unitLabel,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "النظام: ${formatQty(item.systemQuantity)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.textSubtle
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (isCounted) {
                    Text(
                        "الفعلي: ${formatQty(item.actualQuantity!!)}",
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall,
                        color = if (hasDiff) UnpaidAmber else PaidGreen
                    )
                    if (hasDiff) Text(
                        "${if (diff > 0) "+" else ""}${formatQty(diff)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (diff > 0) PaidGreen else DebtRed, fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "لم يُعدّ",
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.divider
                    )
                }
                Icon(
                    Icons.Rounded.Edit,
                    null,
                    tint = appColors.divider,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun CountInputDialog(
    item: InventorySessionItem,
    onConfirm: (Double) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit
) {
    var input by remember {
        mutableStateOf(item.actualQuantity?.let {
            formatQty(it)
        } ?: "")
    }
    AlertDialog(
        onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(Icons.Rounded.Calculate, null, tint = Emerald500)
        },
        title = {
            Column {
                Text(item.productName, fontWeight = FontWeight.Bold)
                Text(
                    item.unitLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.textSubtle
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(appColors.cardBackgroundVariant)
                        .padding(10.dp),
                    Arrangement.SpaceBetween
                ) {
                    Text(
                        "كمية النظام:",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.textSecondary
                    )
                    Text(
                        formatQty(item.systemQuantity),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = input, onValueChange = {
                        input = it
                    },
                    label = {
                        Text("الكمية الفعلية")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                input.toDoubleOrNull()?.let { qty ->
                    val diff = qty - item.systemQuantity
                    val hasDiff = kotlin.math.abs(diff) > 0.001
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasDiff) UnpaidAmber.copy(0.1f) else PaidGreen.copy(0.1f)
                    ) {
                        Row(Modifier
                            .fillMaxWidth()
                            .padding(10.dp), Arrangement.SpaceBetween) {
                            Text("الفرق:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (hasDiff) "${if (diff > 0) "+" else ""}${formatQty(diff)}" else "لا يوجد فرق ✓",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    !hasDiff -> PaidGreen; diff > 0 -> Color(0xFF16A34A); else -> DebtRed
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    input.toDoubleOrNull()?.let {
                        onConfirm(it)
                    }
                },
                enabled = input.toDoubleOrNull() != null,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
            ) {
                Text("تأكيد")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.actualQuantity != null)
                    TextButton(onClick = onClear) {
                        Text("مسح العدّ", color = DebtRed)
                    }
                OutlinedButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )
}

@Composable
private fun NoActiveSession(pastSessions: List<InventorySession>, onStart: () -> Unit) {
    val fmt = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("ar"))
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Rounded.Inventory2,
                        null,
                        Modifier.size(64.dp),
                        tint = Emerald500.copy(0.6f)
                    )
                    Text(
                        "لا توجد جلسة جرد نشطة", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "ابدأ جلسة جرد جديدة لمطابقة الكميات الفعلية مع النظام",
                        style = MaterialTheme.typography.bodyMedium, color = appColors.textSubtle,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onStart, modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("بدء جرد جديد", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (pastSessions.isNotEmpty()) {
            item {
                Text(
                    "جلسات سابقة", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall, color = appColors.textSecondary
                )
            }
            items(pastSessions, key = {
                it.id
            }) { session ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Emerald500.copy(0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                null,
                                tint = Emerald500,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                fmt.format(Date(session.startedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${session.totalAdjustments} تعديل",
                                style = MaterialTheme.typography.labelSmall,
                                color = appColors.textSubtle
                            )
                        }
                        Surface(shape = RoundedCornerShape(20.dp), color = PaidGreen.copy(0.12f)) {
                            Text(
                                "مكتمل", Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall, color = PaidGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(0.7f))
    }
}

private fun formatQty(qty: Double): String =
    if (qty == qty.toLong().toDouble()) qty.toLong().toString()
    else String.format("%.3f", qty).trimEnd('0').trimEnd('.')