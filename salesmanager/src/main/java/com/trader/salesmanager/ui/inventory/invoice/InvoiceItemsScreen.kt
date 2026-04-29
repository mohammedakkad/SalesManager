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
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    existingLinesJson: String? = null,
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

    LaunchedEffect(existingLinesJson) {
        if (!existingLinesJson.isNullOrEmpty()) {
            viewModel.loadExistingLines(existingLinesJson)
        }
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

            // ✅ Banner تحذير عام عند تجاوز أي صنف للمخزون
            AnimatedVisibility(visible = state.hasOverStockWarnings) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DebtRed.copy(0.1f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Warning, null,
                            tint = DebtRed, modifier = Modifier.size(18.dp))
                        Text(
                            "بعض الأصناف تتجاوز الكمية المتاحة في المخزون",
                            style = MaterialTheme.typography.bodySmall,
                            color = DebtRed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
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
                    // ✅ اللون يتغير: Violet عادي → Amber عند وجود تجاوز
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.hasOverStockWarnings) UnpaidAmber else Violet500
                    ),
                    enabled = state.lines.isNotEmpty()
                ) {
                    Icon(
                        if (state.hasOverStockWarnings) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                        null, modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            state.lines.isEmpty() -> "أضف أصناف أولاً"
                            state.hasOverStockWarnings -> "تأكيد رغم تجاوز المخزون  ₪${state.totalAmount.formatAmount()}"
                            else -> "تأكيد  ₪${state.totalAmount.formatAmount()}"
                        },
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
    // ── الألوان المتحركة حسب حالة المخزون ────────────────────────
    val accentColor by animateColorAsState(
        targetValue = if (line.isOverStock) DebtRed else Emerald500,
        animationSpec = tween(400),
        label = "accent"
    )
    val qtyColor by animateColorAsState(
        targetValue = if (line.isOverStock) DebtRed else appColors.textPrimary,
        animationSpec = tween(300),
        label = "qty_color"
    )

    // نسبة الاستخدام للـ progress bar (بين 0 و 1، يتجاوز 1 عند الـ overstock)
    val usageRatio = if (line.stockAvailable > 0 && line.stockAvailable != Double.MAX_VALUE)
        (line.quantity / line.stockAvailable).coerceAtMost(2.0).toFloat() else 0f

    val progressColor by animateColorAsState(
        targetValue = when {
            usageRatio >= 1f -> DebtRed
            usageRatio >= 0.75f -> UnpaidAmber
            else -> Emerald500
        },
        animationSpec = tween(400),
        label = "progress"
    )
    val progressWidth by animateFloatAsState(
        targetValue = usageRatio.coerceAtMost(1f),
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "progress_w"
    )

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (line.isOverStock) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground)
    ) {
        Column {
            Row(Modifier.fillMaxWidth()) {

                // ── شريط ملون رأسي على اليسار ────────────────────
                Box(
                    modifier = Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 0.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (line.isOverStock)
                                listOf(DebtRed, DebtRed.copy(0.5f))
                            else
                                listOf(Emerald500, Cyan500)
                        )
                    )
                )

                // ── محتوى البطاقة ─────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    // ── أيقونة الصنف ──────────────────────────────
                    Box(
                        modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = line.product.product.name.firstOrNull()?.toString() ?: "؟",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // ── اسم الصنف + badges ────────────────────────
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = line.product.product.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = appColors.textPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // وحدة
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = appColors.surfaceVariant
                            ) {
                                Text(
                                    line.selectedUnit.unitLabel,
                                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.textSecondary
                                )
                            }
                            // كيلو label
                            if (line.kgLabel.isNotEmpty()) {
                                Text(line.kgLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Cyan500)
                            }
                            // سعر معدّل badge
                            if (line.customPrice != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = UnpaidAmber.copy(0.12f)
                                ) {
                                    Text("معدّل",
                                        Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = UnpaidAmber,
                                        fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        // سعر الوحدة
                        Text(
                            "₪${line.effectivePrice.formatAmount()} / ${line.selectedUnit.unitLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = appColors.textSubtle
                        )
                    }

                    // ── عداد الكمية ───────────────────────────────
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // زر ناقص
                            Box(
                                Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DebtRed.copy(0.08f))
                                .clickable {
                                    onDecrement()
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Remove, null,
                                    tint = DebtRed,
                                    modifier = Modifier.size(16.dp))
                            }

                            // الكمية بـ AnimatedContent
                            AnimatedContent(
                                targetState = line.displayQtyLabel,
                                transitionSpec = {
                                    (slideInVertically {
                                        -it
                                    } + fadeIn()) togetherWith
                                    (slideOutVertically {
                                        it
                                    } + fadeOut())
                                },
                                modifier = Modifier.widthIn(min = 50.dp),
                                label = "qty"
                            ) {
                                label ->
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center,
                                    color = qtyColor
                                )
                            }

                            // زر زائد
                            Box(
                                Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Emerald500.copy(0.08f))
                                .clickable {
                                    onIncrement()
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Add, null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(16.dp))
                            }
                        }

                        // السعر الإجمالي + زر تعديل
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "₪${line.totalPrice.formatAmount()}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = Violet500
                            )
                            Box(
                                Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Violet500.copy(0.12f))
                                .clickable {
                                    onEdit()
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Edit, null,
                                    tint = Violet500,
                                    modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // ── شريط تقدم المخزون ─────────────────────────────────
            if (line.stockAvailable != Double.MAX_VALUE) {
                Column(
                    Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // شريط التقدم
                    Box(
                        Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(appColors.divider)
                    ) {
                        Box(
                            Modifier
                            .fillMaxWidth(progressWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(progressColor, progressColor.copy(0.7f))
                                )
                            )
                        )
                    }

                    // نص الحالة
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ✅ حالة overstock — chip أنيق بدل نص أحمر بارد
                        AnimatedVisibility(
                            visible = line.isOverStock,
                            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = DebtRed.copy(0.1f)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.TrendingUp, null,
                                        tint = DebtRed,
                                        modifier = Modifier.size(12.dp))
                                    Text(
                                        "نقص ${line.overStockAmount.formatQty()} ${line.selectedUnit.unitLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DebtRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // المتاح / المطلوب
                        Text(
                            if (line.isOverStock)
                                "${line.quantity.formatQty()} من ${line.stockAvailable.formatQty()} متاح"
                            else
                                "متاح: ${line.stockAvailable.formatQty()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (line.isOverStock) DebtRed.copy(0.8f) else appColors.textSubtle,
                            fontSize = 10.sp
                        )
                    }
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

    // ── حالة محلية لوحدة الوزن لتحديث UI فوري عند الضغط ────────
    var selectedWeightUnit by remember(line.tempId) {
        mutableStateOf(line.displayWeightUnit)
    }

    // مزامنة مع ViewModel في حال تغيير خارجي
    LaunchedEffect(line.displayWeightUnit) {
        if (selectedWeightUnit != line.displayWeightUnit) {
            selectedWeightUnit = line.displayWeightUnit
        }
        // تحديث حقل الكمية ليعكس الوحدة الجديدة
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
                            val isSelected = selectedWeightUnit == wu
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    // ① تحديث فوري للـ UI
                                    selectedWeightUnit = wu
                                    // ② تحديث حقل الكمية فوراً بالوحدة الجديدة
                                    val currentKg = line.quantity
                                    qtyInput = fromKgQuantity(currentKg, wu).formatQty()
                                    // ③ تحديث ViewModel
                                    onUpdateWeightUnit(wu)
                                },
                                label = {
                                    Text(wu.labelAr)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Cyan500.copy(0.15f),
                                    selectedLabelColor = Cyan500,
                                    containerColor = appColors.cardBackground,
                                    labelColor = appColors.textSecondary
                                )
                            )
                        }
                    }
                    // تلميح: الكيلو المكافئ
                    val enteredQty = qtyInput.toSafeDouble()
                    if (enteredQty != null && enteredQty > 0 && selectedWeightUnit != SaleWeightUnit.KG) {
                        val kgEquiv = toKgQuantity(enteredQty, selectedWeightUnit)
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
                            if (isWeightProduct) "الكمية (${selectedWeightUnit.labelAr})"
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