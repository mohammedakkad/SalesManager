package com.trader.salesmanager.ui.inventory.detail

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.unit.sp
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProductDetailScreen(
    productId: String,
    onNavigateUp: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: ProductDetailViewModel = koinViewModel(parameters = { parametersOf(productId) })
) {
    val state by viewModel.uiState.collectAsState()
    var showDelete by remember { mutableStateOf(false) }
    var expandedUnit by remember { mutableStateOf<String?>(null) }

    // Dialog حذف
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            icon = { Icon(Icons.Rounded.DeleteForever, null, tint = DebtRed) },
            title = { Text("حذف الصنف", fontWeight = FontWeight.Bold) },
            text = { Text("سيتم حذف الصنف \"${state.product?.product?.name}\" وكل وحداته نهائياً.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteProduct { onNavigateUp() } },
                    colors = ButtonDefaults.buttonColors(containerColor = DebtRed)
                ) { Text("حذف") }
            },
            dismissButton = { OutlinedButton(onClick = { showDelete = false }) { Text("إلغاء") } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Dialog تعديل يدوي
    val adj = state.adjustState
    if (adj.show) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAdjust,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    if (adj.isAdd) Icons.Rounded.AddCircle else Icons.Rounded.RemoveCircle,
                    null, tint = if (adj.isAdd) PaidGreen else DebtRed
                )
            },
            title = {
                Text(
                    "${if (adj.isAdd) "إضافة" else "خصم"} من ${adj.unitLabel}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = adj.quantity,
                        onValueChange = viewModel::setAdjustQty,
                        label = { Text("الكمية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = adj.note,
                        onValueChange = viewModel::setAdjustNote,
                        label = { Text("ملاحظة (اختياري)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::applyAdjust,
                    enabled = adj.quantity.toDoubleOrNull() != null && adj.quantity.toDoubleOrNull()!! > 0 && !state.isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (adj.isAdd) PaidGreen else DebtRed
                    )
                ) { Text("تطبيق") }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::dismissAdjust) { Text("إلغاء") }
            }
        )
    }

    val p = state.product
    if (p == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = Emerald500)
        }
        return
    }

    val initial = p.product.name.firstOrNull()?.toString() ?: "؟"
    val bgColors = listOf(Emerald500, Cyan500, Violet500, UnpaidAmber)
    val bg = bgColors[p.product.name.length % bgColors.size]

    LazyColumn(Modifier.fillMaxSize().background(appColors.screenBackground)) {

        // ── Header ────────────────────────────────────────────────
        item {
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(bg.copy(0.8f), bg)))
                    .padding(top = 48.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { onEdit(productId) }) {
                            Icon(Icons.Rounded.Edit, null, tint = Color.White)
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Rounded.Delete, null, tint = Color.White.copy(0.8f))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) { Text(initial, color = Color.White, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium) }
                        Column {
                            Text(p.product.name, color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold)
                            if (p.product.category.isNotEmpty())
                                Text(p.product.category, color = Color.White.copy(0.75f),
                                    style = MaterialTheme.typography.bodySmall)
                            if (p.product.barcode != null)
                                Text("⬛ ${p.product.barcode}", color = Color.White.copy(0.6f),
                                    style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // ── الوحدات ───────────────────────────────────────────────
        item {
            Column(Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الوحدات والمخزون", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = appColors.textSecondary)
                p.units.forEach { unit ->
                    UnitCard(
                        unit = unit,
                        isExpanded = expandedUnit == unit.id,
                        onToggle = { expandedUnit = if (expandedUnit == unit.id) null else unit.id },
                        onAddStock = { viewModel.showAdjust(unit, true) },
                        onDeductStock = { viewModel.showAdjust(unit, false) },
                        movementsFlow = viewModel.getMovementsForUnit(unit.id)
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun UnitCard(
    unit: ProductUnit,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddStock: () -> Unit,
    onDeductStock: () -> Unit,
    movementsFlow: kotlinx.coroutines.flow.Flow<List<StockMovement>>
) {
    val movements by movementsFlow.collectAsState(initial = emptyList())
    val isLow = unit.quantityInStock > 0 && unit.quantityInStock <= unit.lowStockThreshold
    val isOut = unit.quantityInStock <= 0
    val statusColor = when { isOut -> DebtRed; isLow -> UnpaidAmber; else -> PaidGreen }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // رأس الوحدة
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(statusColor.copy(0.12f)),
                    contentAlignment = Alignment.Center) {
                    Icon(
                        when (unit.unitType) {
                            UnitType.PIECE -> Icons.Rounded.Category
                            UnitType.CARTON -> Icons.Rounded.Inventory2
                            UnitType.WEIGHT -> Icons.Rounded.Scale
                        },
                        null, tint = statusColor, modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(unit.unitLabel, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium)
                        if (unit.isDefault)
                            Surface(shape = RoundedCornerShape(20.dp), color = Emerald500.copy(0.12f)) {
                                Text("⭐", Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall)
                            }
                    }
                    val qtyText = when (unit.unitType) {
                        UnitType.WEIGHT -> "${String.format("%.3f", unit.quantityInStock)} كجم"
                        else -> "${unit.quantityInStock.toInt()} ${unit.unitLabel}"
                    }
                    Text(qtyText, color = statusColor, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall)
                    if (unit.unitType == UnitType.CARTON && unit.itemsPerCarton != null)
                        Text("${unit.itemsPerCarton} قطعة/كرتون", style = MaterialTheme.typography.labelSmall,
                            color = appColors.textSubtle)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₪${String.format("%.2f", unit.price)}",
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("تنبيه < ${unit.lowStockThreshold.let { if (unit.unitType == UnitType.WEIGHT) String.format("%.1f", it) else it.toInt().toString() }}",
                        style = MaterialTheme.typography.labelSmall, color = appColors.textSubtle)
                }
                Icon(
                    if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null, tint = appColors.divider, modifier = Modifier.size(20.dp)
                )
            }

            // أزرار التعديل + سجل الحركات (عند التوسيع)
            AnimatedVisibility(isExpanded) {
                Column {
                    HorizontalDivider(color = Color.WhiteVariant)
                    // أزرار التعديل
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onAddStock, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(PaidGreen)
                            )
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = PaidGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("إضافة", color = PaidGreen)
                        }
                        OutlinedButton(
                            onClick = onDeductStock, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(DebtRed)
                            )
                        ) {
                            Icon(Icons.Rounded.Remove, null, tint = DebtRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("خصم", color = DebtRed)
                        }
                    }

                    // سجل الحركات
                    if (movements.isNotEmpty()) {
                        HorizontalDivider(color = Color.WhiteVariant)
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("سجل الحركات", fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge,
                                color = appColors.textSecondary)
                            movements.take(10).forEach { mov ->
                                MovementRow(mov)
                            }
                            if (movements.size > 10) {
                                Text("+ ${movements.size - 10} حركة أخرى",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.textSubtle,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementRow(mov: StockMovement) {
    val isIn = mov.quantity > 0
    val color = if (isIn) PaidGreen else DebtRed
    val icon = when (mov.movementType) {
        MovementType.SALE_OUT -> Icons.Rounded.ShoppingCart
        MovementType.RETURN_IN -> Icons.Rounded.Undo
        MovementType.MANUAL_IN -> Icons.Rounded.AddCircle
        MovementType.MANUAL_OUT -> Icons.Rounded.RemoveCircle
        MovementType.INVENTORY_ADJUST -> Icons.Rounded.Inventory2
    }
    val typeLabel = when (mov.movementType) {
        MovementType.SALE_OUT -> "بيع"
        MovementType.RETURN_IN -> "إرجاع"
        MovementType.MANUAL_IN -> "إضافة يدوية"
        MovementType.MANUAL_OUT -> "خصم يدوي"
        MovementType.INVENTORY_ADJUST -> "تعديل جرد"
    }
    val fmt = SimpleDateFormat("dd/MM hh:mm a", Locale("ar"))
    val timeStr = fmt.format(Date(mov.createdAt))

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(color.copy(0.1f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(typeLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            if (mov.note.isNotEmpty())
                Text(mov.note, style = MaterialTheme.typography.labelSmall, color = appColors.textSubtle)
            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = appColors.divider,
                fontSize = 10.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${if (isIn) "+" else ""}${String.format("%.2f", mov.quantity)}",
                color = color, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "${String.format("%.2f", mov.quantityBefore)} → ${String.format("%.2f", mov.quantityAfter)}",
                style = MaterialTheme.typography.labelSmall, color = appColors.textSubtle,
                fontSize = 9.sp
            )
        }
    }
}
