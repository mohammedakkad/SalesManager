package com.trader.salesmanager.ui.inventory.invoice

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.scanner.BarcodeScannerScreen
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors
import com.trader.salesmanager.ui.inventory.invoice.toLatinDigits
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceItemsScreen(
    customerName: String,
    onNavigateUp: () -> Unit,
    onConfirm: (lines: List<InvoiceLineItem>, totalAmount: Double) -> Unit,
    viewModel: InvoiceItemsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showScanner by remember {
        mutableStateOf(false)
    }
    var showSearch by remember {
        mutableStateOf(false)
    }
    var editingIndex by remember {
        mutableStateOf<Int?>(null)
    }
    var barcodeNotFound by remember {
        mutableStateOf<String?>(null)
    }

    barcodeNotFound?.let {
        barcode ->
        AlertDialog(
            onDismissRequest = {
                barcodeNotFound = null
            },
            icon = {
                Icon(Icons.Rounded.QrCodeScanner, null, tint = UnpaidAmber)
            },
            title = {
                Text("باركود غير موجود", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("\"$barcode\" غير موجود في المخزن.")
            },
            confirmButton = {
                Button(onClick = {
                    barcodeNotFound = null
                }) {
                    Text("حسناً")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ✅ الإصلاح الأول: نحصل على line مباشرة من state.lines[index]
    // هكذا أي تغيير في ViewModel (وحدة الوزن مثلاً) ينعكس فوراً على الـ Dialog
    editingIndex?.let {
        index ->
        val line = state.lines.getOrNull(index)
        if (line != null) {
            EditLineDialog(
                line = line,
                onUpdateQty = {
                    viewModel.updateDisplayQty(index, it)
                },
                onUpdatePrice = {
                    viewModel.updatePrice(index, it)
                },
                onUpdateUnit = {
                    viewModel.updateUnit(index, it)
                },
                onUpdateWeightUnit = {
                    viewModel.updateWeightUnit(index, it)
                },
                onRemove = {
                    viewModel.removeLine(index); editingIndex = null
                },
                onDismiss = {
                    editingIndex = null
                }
            )
        } else {
            // الصنف حُذف — أغلق الـ Dialog
            editingIndex = null
        }
    }

    if (showScanner) {
        BarcodeScannerScreen(
            onBarcodeDetected = {
                barcode ->
                showScanner = false
                viewModel.onBarcodeScanned(barcode, onNotFound = {
                    barcodeNotFound = it
                })
            },
            onDismiss = {
                showScanner = false
            }
        )
        return
    }

    Scaffold(containerColor = appColors.screenBackground) {
        padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Header ──────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Violet500, Cyan500)))
                .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("أصناف الفاتورة", fontWeight = FontWeight.Bold,
                                color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Text("الزبون: $customerName", color = Color.White.copy(0.75f),
                                style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(
                            onClick = {
                                showScanner = true
                            },
                            modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.15f))
                        ) {
                            Icon(Icons.Rounded.QrCodeScanner, null, tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                showSearch = !showSearch
                            },
                            modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.15f))
                        ) {
                            Icon(Icons.Rounded.Search, null, tint = if (showSearch) Cyan500 else Color.White)
                        }
                    }

                    AnimatedVisibility(showSearch) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = viewModel::setQuery,
                                placeholder = {
                                    Text("ابحث عن صنف...", color = Color.White.copy(0.5f))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
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
                            AnimatedVisibility(state.searchResults.isNotEmpty()) {
                                Card(
                                    Modifier.fillMaxWidth().padding(top = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackground)
                                ) {
                                    state.searchResults.forEach {
                                        product ->
                                        SearchResultItem(product = product, onSelect = {
                                            unit ->
                                            viewModel.addProductWithUnit(product, unit)
                                            showSearch = false
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── ملخص ──────────────────────────────────────────────
            AnimatedVisibility(state.lines.isNotEmpty()) {
                Row(
                    modifier = Modifier
                    .fillMaxWidth()
                    .background(appColors.cardBackground)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${state.lines.size} صنف",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.textSecondary)
                    Text("الإجمالي: ₪${state.totalAmount.formatAmount()}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = Violet500)
                }
            }

            // ── قائمة الأصناف ────────────────────────────────────
            if (state.lines.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.ShoppingCart, null,
                            Modifier.size(64.dp), tint = appColors.textSubtle)
                        Spacer(Modifier.height(12.dp))
                        Text("لا توجد أصناف بعد", color = appColors.textSubtle)
                        Text("امسح باركود أو ابحث عن صنف",
                            style = MaterialTheme.typography.bodySmall,
                            color = appColors.textSubtle)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.lines, key = {
                        _, l -> l.tempId
                    }) {
                        index, line ->
                        InvoiceLineCard(
                            line = line,
                            onEdit = {
                                editingIndex = index
                            }, // ✅ نحفظ الـ index فقط
                            onIncrement = {
                                viewModel.updateDisplayQty(index, line.displayQty + 1)
                            },
                            onDecrement = {
                                viewModel.updateDisplayQty(index, line.displayQty - 1)
                            }
                        )
                    }
                    item {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }

            // ── زر التأكيد ────────────────────────────────────────
            Surface(
                Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = appColors.cardBackground
            ) {
                Button(
                    onClick = {
                        onConfirm(state.lines, state.totalAmount)
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Violet500),
                    enabled = state.lines.isNotEmpty()
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.lines.isEmpty()) "أضف أصناف أولاً"
                        else "تأكيد  ₪${state.totalAmount.formatAmount()}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── بطاقة صنف واحد ───────────────────────────────────────────────
@Composable
private fun InvoiceLineCard(
    line: InvoiceLineItem,
    onEdit: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(line.product.product.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = appColors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(line.selectedUnit.unitLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.textSecondary)
                    if (line.kgLabel.isNotEmpty())
                        Text(line.kgLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan500)
                    if (line.customPrice != null)
                        Text("• سعر معدّل",
                        style = MaterialTheme.typography.labelSmall,
                        color = UnpaidAmber)
                }
                Text("₪${line.effectivePrice.formatAmount()} / ${line.selectedUnit.unitLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.textSubtle)
            }

            // أزرار الكمية
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Remove, null, tint = DebtRed, modifier = Modifier.size(18.dp))
                }
                Text(
                    line.displayQtyLabel,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.widthIn(min = 48.dp),
                    textAlign = TextAlign.Center,
                    color = appColors.textPrimary
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Add, null, tint = PaidGreen, modifier = Modifier.size(18.dp))
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("₪${line.totalPrice.formatAmount()}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = Violet500)

                // ✅ الإصلاح الثاني: زر تعديل واضح في كلا الوضعين
                // استُخدم appColors.divider قديماً وهو خافت جداً
                // الآن: دائرة ملونة صغيرة تظهر في Light و Dark
                Box(
                    modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Violet500.copy(alpha = 0.15f))
                    .clickable {
                        onEdit()
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Edit, null,
                        tint = Violet500,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ── نتيجة بحث ────────────────────────────────────────────────────
@Composable
private fun SearchResultItem(product: ProductWithUnits, onSelect: (ProductUnit) -> Unit) {
    Column {
        if (product.units.size == 1) {
            Row(
                modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSelect(product.units.first())
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(product.product.name, Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = appColors.textPrimary)
                Text("₪${product.units.first().price.formatAmount()}",
                    color = Violet500,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Text(product.product.name,
                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                color = appColors.textPrimary)
            product.units.forEach {
                unit ->
                Row(
                    modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelect(unit)
                    }
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(unit.unitLabel,
                        color = appColors.textSecondary,
                        style = MaterialTheme.typography.bodySmall)
                    Text("₪${unit.price.formatAmount()}",
                        color = Violet500,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        HorizontalDivider(color = appColors.divider)
    }
}

// ── نافذة تعديل الصنف ────────────────────────────────────────────
// ✅ الإصلاح الثاني:
// المشكلة القديمة: editingLine كان Pair<Int, InvoiceLineItem> — يحفظ نسخة قديمة من line.
// عند الضغط على وحدة الوزن:
//   - onUpdateWeightUnit يُحدّث ViewModel ✅
//   - لكن line.displayWeightUnit داخل الـ Dialog لا يتغير ❌
//   - لأن line في الـ Dialog هي نسخة مجمّدة قديمة من وقت فتح الـ Dialog
//
// الحل: نجعل الـ Dialog يستقبل line محدثة من state.lines[index] دائماً
// (يتم ذلك في الشاشة الرئيسية أعلاه عبر: val line = state.lines.getOrNull(index))
//
// داخل الـ Dialog: نستخدم line.displayWeightUnit المحدث كـ selected
// و qtyInput تُحدث عند تغيير الوحدة عبر LaunchedEffect
@Composable
private fun EditLineDialog(
    line: InvoiceLineItem,
    onUpdateQty: (Double) -> Unit,
    onUpdatePrice: (Double?) -> Unit,
    onUpdateUnit: (ProductUnit) -> Unit,
    onUpdateWeightUnit: (SaleWeightUnit) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var qtyInput by remember(line.tempId) {
        mutableStateOf(line.displayQty.formatQty())
    }
    var priceInput by remember(line.tempId) {
        mutableStateOf(line.customPrice?.formatAmount() ?: "")
    }

    // ✅ عندما تتغير displayWeightUnit في ViewModel (line تتحدث)
    // نحدّث qtyInput تلقائياً ليعكس الكمية بالوحدة الجديدة
    LaunchedEffect(line.displayWeightUnit) {
        val currentKg = line.quantity
        val newDisplay = fromKgQuantity(currentKg, line.displayWeightUnit)
        qtyInput = newDisplay.formatQty()
    }

    val isWeightProduct = line.selectedUnit.unitType == UnitType.WEIGHT

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(line.product.product.name,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // ── اختيار وحدة المنتج (كيلو / كرتون / حبة) ──────
                if (line.product.units.size > 1) {
                    Text("الوحدة:",
                        style = MaterialTheme.typography.labelLarge,
                        color = appColors.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        line.product.units.forEach {
                            unit ->
                            FilterChip(
                                selected = line.selectedUnit.id == unit.id,
                                onClick = {
                                    onUpdateUnit(unit)
                                },
                                label = {
                                    Text(unit.unitLabel)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Violet500.copy(0.15f),
                                    selectedLabelColor = Violet500
                                )
                            )
                        }
                    }
                }

                // ── اختيار وحدة الوزن التجارية ────────────────────
                if (isWeightProduct) {
                    Text("وحدة الكمية:",
                        style = MaterialTheme.typography.labelLarge,
                        color = appColors.textSecondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        SaleWeightUnit.entries.forEach {
                            wu ->
                            FilterChip(
                                // ✅ يقرأ line.displayWeightUnit المحدث من ViewModel
                                selected = line.displayWeightUnit == wu,
                                onClick = {
                                    onUpdateWeightUnit(wu)
                                },
                                label = {
                                    Text(wu.labelAr)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Cyan500.copy(0.15f),
                                    selectedLabelColor = Cyan500
                                )
                            )
                        }
                    }
                    // تلميح: الكيلو المكافئ
                    val enteredQty = qtyInput.toSafeDouble()
                    if (enteredQty != null && enteredQty > 0 && line.displayWeightUnit != SaleWeightUnit.KG) {
                        val kgEquiv = toKgQuantity(enteredQty, line.displayWeightUnit)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Cyan500.copy(0.1f)
                        ) {
                            Text(
                                "= ${kgEquiv.formatQty()} كيلو",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Cyan500,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // ── إدخال الكمية ──────────────────────────────────
                OutlinedTextField(
                    value = qtyInput,
                    onValueChange = {
                        qtyInput = it.toLatinDigits()
                    },
                    label = {
                        Text(
                            if (isWeightProduct) "الكمية (${line.displayWeightUnit.labelAr})"
                            else "الكمية"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── سعر مخصص ─────────────────────────────────────
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = {
                        priceInput = it.toLatinDigits()
                    },
                    label = {
                        Text("سعر مخصص (فارغ = افتراضي ₪${line.selectedUnit.price.formatAmount()})")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    qtyInput.toSafeDouble()?.let {
                        onUpdateQty(it)
                    }
                    onUpdatePrice(priceInput.toSafeDouble())
                    onDismiss()
                },
                enabled = (qtyInput.toSafeDouble() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Violet500)
            ) {
                Text("تطبيق")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    onRemove(); onDismiss()
                }) {
                    Text("حذف", color = DebtRed)
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )
}

@Composable
private fun rememberScrollState() = androidx.compose.foundation.rememberScrollState()