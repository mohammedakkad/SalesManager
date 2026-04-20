package com.trader.salesmanager.ui.inventory.addedit

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.UnitType
import com.trader.core.domain.model.WeightUnit
import com.trader.salesmanager.ui.scanner.BarcodeScannerScreen
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddEditProductScreen(
    productId: String? = null,
    initialBarcode: String? = null,
    onNavigateUp: () -> Unit,
    viewModel: AddEditProductViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showScanner by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(productId) {
        if (productId != null) viewModel.loadProduct(productId)
        else if (initialBarcode != null) viewModel.initWithBarcode(initialBarcode)
    }
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            kotlinx.coroutines.delay(300)
            onNavigateUp()
        }
    }

    if (showScanner) {
        BarcodeScannerScreen(
            onBarcodeDetected = {
                barcode ->
                viewModel.setBarcode(barcode)
                showScanner = false
            },
            onDismiss = {
                showScanner = false
            }
        )
        return
    }

    Column(
        Modifier
        .fillMaxSize()
        .background(appColors.screenBackground)
        .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────
        Box(
            Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Emerald700, Emerald500)))
            .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                }
                Text(
                    if (state.isEditing) "تعديل صنف" else "إضافة صنف جديد",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── باركود ────────────────────────────────────────────
            SectionCard("الباركود", Icons.Rounded.QrCode, Cyan500) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = state.barcode,
                        onValueChange = viewModel::setBarcode,
                        label = {
                            Text("الباركود (اختياري)")
                        },
                        placeholder = {
                            Text("6223001234567")
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        isError = state.barcodeConflict != null,
                        // ── trailing icon: spinner أثناء التحقق، خطأ عند التعارض ──
                        trailingIcon = {
                            when {
                                state.barcodeChecking -> CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Cyan500
                                )
                                state.barcodeConflict != null -> Icon(
                                    Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                state.barcode.isNotBlank() && !state.barcodeChecking -> Icon(
                                    Icons.Rounded.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = Emerald500
                                )
                            }
                        },
                        // ── رسالة التعارض تظهر تحت الحقل مباشرةً ──
                        supportingText = state.barcodeConflict?.let {
                            conflict ->
                            {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        conflict,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    )
                    OutlinedIconButton(
                        onClick = {
                            showScanner = true
                        },
                        modifier = Modifier
                        .size(56.dp)
                        .padding(top = 4.dp) // محاذاة مع الـ TextField
                    ) {
                        Icon(Icons.Rounded.QrCodeScanner, null, tint = Cyan500)
                    }
                }
            }

            // ── معلومات الصنف ─────────────────────────────────────
            SectionCard("معلومات الصنف", Icons.Rounded.Info, Emerald500) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = {
                        Text("اسم الصنف *")
                    },
                    singleLine = true,
                    isError = state.name.isEmpty() && state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.category,
                    onValueChange = viewModel::setCategory,
                    label = {
                        Text("الفئة (اختياري)")
                    },
                    placeholder = {
                        Text("مشروبات، مواد غذائية...")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── الوحدات ───────────────────────────────────────────
            SectionCard("الوحدات", Icons.Rounded.Scale, Violet500) {
                state.units.forEachIndexed {
                    index, unit ->
                    UnitEditor(
                        unit = unit,
                        index = index,
                        canDelete = state.units.size > 1,
                        onUpdate = {
                            viewModel.updateUnit(index, it)
                        },
                        onUpdatePrice = {
                            viewModel.updateUnitPrice(index, it)
                        },
                        onUpdateQty = {
                            viewModel.updateUnitQty(index, it)
                        },
                        onUpdateLowStock = {
                            viewModel.updateUnitLowStock(index, it)
                        },
                        onDelete = {
                            viewModel.removeUnit(index)
                        },
                        onSetDefault = {
                            viewModel.setDefaultUnit(index)
                        }
                    )
                    if (index < state.units.lastIndex) {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = appColors.divider)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::addUnit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder(true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Emerald500)
                    )
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Emerald500, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("إضافة وحدة أخرى", color = Emerald500)
                }
            }

            // ── خطأ عام ───────────────────────────────────────────
            state.error?.let {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DebtRed.copy(0.1f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ErrorOutline, null, tint = DebtRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = DebtRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // ── زر الحفظ ──────────────────────────────────────────
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                enabled = state.isValid && !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.isEditing) "حفظ التعديلات" else "إضافة الصنف",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UnitEditor(
    unit: UnitDraft,
    index: Int,
    canDelete: Boolean,
    onUpdate: (UnitDraft) -> Unit,
    onUpdatePrice: (String) -> Unit,
    onUpdateQty: (String) -> Unit,
    onUpdateLowStock: (String) -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "وحدة ${index + 1}", fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)
            )
            if (unit.isDefault) {
                Surface(shape = RoundedCornerShape(20.dp), color = Emerald500.copy(0.12f)) {
                    Text(
                        "افتراضي ⭐", Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall, color = Emerald500,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                TextButton(onClick = onSetDefault) {
                    Text("تعيين افتراضي", style = MaterialTheme.typography.labelSmall, color = Cyan500)
                }
            }
            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Delete, null, tint = DebtRed, modifier = Modifier.size(18.dp))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UnitType.entries.forEach {
                type ->
                val label = when (type) {
                    UnitType.PIECE -> "حبة"; UnitType.CARTON -> "كرتون"; UnitType.WEIGHT -> "كيلو"
                }
                val sel = unit.unitType == type
                FilterChip(
                    selected = sel,
                    onClick = {
                        onUpdate(
                            unit.copy(
                                unitType = type,
                                unitLabel = label,
                                weightUnit = WeightUnit.KG,
                                itemsPerCarton = if (type == UnitType.CARTON) unit.itemsPerCarton else ""
                            )
                        )
                    },
                    label = {
                        Text(label)
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Emerald500.copy(0.15f),
                        selectedLabelColor = Emerald500
                    )
                )
            }
        }

        OutlinedTextField(
            value = unit.unitLabel,
            onValueChange = {
                onUpdate(unit.copy(unitLabel = it))
            },
            label = {
                Text("اسم الوحدة")
            },
            placeholder = {
                Text("حبة / علبة / كيلو")
            },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = unit.price,
                onValueChange = {
                    onUpdatePrice(it)
                },
                label = {
                    Text("السعر ₪")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = unit.quantityInStock,
                onValueChange = {
                    onUpdateQty(it)
                },
                label = {
                    Text(if (unit.unitType == UnitType.WEIGHT) "الكمية (كجم)" else "الكمية")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(unit.unitType == UnitType.CARTON) {
            OutlinedTextField(
                value = unit.itemsPerCarton,
                onValueChange = {
                    onUpdate(unit.copy(itemsPerCarton = it))
                },
                label = {
                    Text("عدد القطع في الكرتون")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }

        AnimatedVisibility(unit.unitType == UnitType.WEIGHT) {
            Surface(shape = RoundedCornerShape(8.dp), color = Emerald500.copy(alpha = 0.1f)) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Scale, null, tint = Emerald500, modifier = Modifier.size(16.dp))
                    Text(
                        "وحدة التخزين: كيلوغرام (كجم)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Emerald500
                    )
                }
            }
        }

        OutlinedTextField(
            value = unit.lowStockThreshold,
            onValueChange = {
                onUpdateLowStock(it)
            },
            label = {
                Text("تنبيه عند نقص الكمية إلى")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    Icons.Rounded.NotificationsActive, null,
                    tint = UnpaidAmber, modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}