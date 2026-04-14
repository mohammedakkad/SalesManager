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
    var editingLine by remember {
        mutableStateOf<Pair<Int, InvoiceLineItem>?>(null)
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

    editingLine?.let {
        (index, line) ->
        EditLineDialog(
            line = line,
            onUpdateQty = {
                viewModel.updateQuantity(index, it)
            },
            onUpdatePrice = {
                viewModel.updatePrice(index, it)
            },
            onUpdateUnit = {
                viewModel.updateUnit(index, it)
            },
            onRemove = {
                viewModel.removeLine(index)
            },
            onDismiss = {
                editingLine = null
            }
        )
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

    Scaffold(containerColor = Color(0xFFF2F4F7)) {
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
                        IconButton(onClick = {
                            showScanner = true
                        },
                            modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.15f))
                        ) {
                            Icon(Icons.Rounded.QrCodeScanner, null, tint = Color.White)
                        }
                        IconButton(onClick = {
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
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.White.copy(0.1f),
                                    unfocusedContainerColor = Color.White.copy(0.07f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            AnimatedVisibility(state.searchResults.isNotEmpty()) {
                                Card(Modifier.fillMaxWidth().padding(top = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    Modifier.fillMaxWidth().background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                    Arrangement.SpaceBetween, Alignment.CenterVertically
                ) {
                    Text("${state.lines.size} صنف  •  ${state.totalItems} قطعة",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                    Text("الإجمالي: ₪${String.format("%.2f", state.totalAmount)}",
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall,
                        color = Violet500)
                }
            }

            // ── قائمة الأصناف ────────────────────────────────────
            if (state.lines.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.ShoppingCart, null,
                            Modifier.size(64.dp), tint = Color(0xFFCBD5E1))
                        Spacer(Modifier.height(12.dp))
                        Text("لا توجد أصناف بعد", color = Color(0xFF94A3B8))
                        Text("امسح باركود أو ابحث عن صنف",
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(state.lines, key = {
                        _, l -> l.tempId
                    }) {
                        index, line ->
                        InvoiceLineCard(
                            line = line,
                            onEdit = {
                                editingLine = index to line
                            },
                            onIncrement = {
                                viewModel.updateQuantity(index, line.quantity + 1)
                            },
                            onDecrement = {
                                viewModel.updateQuantity(index, line.quantity - 1)
                            }
                        )
                    }
                    item {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }

            // ── زر التأكيد ────────────────────────────────────────
            Surface(Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = Color.White) {
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
                        else "تأكيد  ₪${String.format("%.2f", state.totalAmount)}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(product: ProductWithUnits, onSelect: (ProductUnit) -> Unit) {
    Column {
        if (product.units.size == 1) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    onSelect(product.units.first())
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(product.product.name, Modifier.weight(1f), fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("₪${String.format("%.2f", product.units.first().price)}",
                    color = Violet500, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Text(product.product.name, Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            product.units.forEach {
                unit ->
                Row(Modifier.fillMaxWidth().clickable {
                    onSelect(unit)
                }
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(unit.unitLabel, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                    Text("₪${String.format("%.2f", unit.price)}", color = Violet500,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        HorizontalDivider(color = Color(0xFFF1F5F9))
    }
}

@Composable
private fun InvoiceLineCard(
    line: InvoiceLineItem, onEdit: () -> Unit,
    onIncrement: () -> Unit, onDecrement: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(line.product.product.name, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(line.selectedUnit.unitLabel, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                    if (line.customPrice != null)
                        Text("• سعر معدّل", style = MaterialTheme.typography.labelSmall, color = UnpaidAmber)
                }
                Text("₪${String.format("%.2f", line.effectivePrice)} / ${line.selectedUnit.unitLabel}",
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            }
            Row(Alignment.CenterVertically, Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Remove, null, tint = DebtRed, modifier = Modifier.size(18.dp))
                }
                Text(
                    if (line.quantity == line.quantity.toLong().toDouble()) line.quantity.toLong().toString()
                    else String.format("%.2f", line.quantity),
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.widthIn(min = 28.dp), textAlign = TextAlign.Center
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Add, null, tint = PaidGreen, modifier = Modifier.size(18.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₪${String.format("%.2f", line.totalPrice)}",
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Violet500)
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Edit, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun EditLineDialog(
    line: InvoiceLineItem,
    onUpdateQty: (Double) -> Unit, onUpdatePrice: (Double?) -> Unit,
    onUpdateUnit: (ProductUnit) -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit
) {
    var qtyInput by remember {
        mutableStateOf(
            if (line.quantity == line.quantity.toLong().toDouble()) line.quantity.toLong().toString()
            else String.format("%.3f", line.quantity)
        )}
    var priceInput by remember {
        mutableStateOf(line.customPrice?.let {
            String.format("%.2f", it)
        } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(line.product.product.name, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (line.product.units.size > 1) {
                    Text("الوحدة:", style = MaterialTheme.typography.labelLarge, color = Color(0xFF64748B))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        line.product.units.forEach {
                            unit ->
                            FilterChip(selected = line.selectedUnit.id == unit.id,
                                onClick = {
                                    onUpdateUnit(unit)
                                }, label = {
                                    Text(unit.unitLabel)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Violet500.copy(0.15f), selectedLabelColor = Violet500))
                        }
                    }
                }
                OutlinedTextField(value = qtyInput, onValueChange = {
                    qtyInput = it
                },
                    label = {
                        Text("الكمية")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = priceInput, onValueChange = {
                    priceInput = it
                },
                    label = {
                        Text("سعر مخصص (فارغ = افتراضي ₪${String.format("%.2f", line.selectedUnit.price)})")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                qtyInput.toDoubleOrNull()?.let {
                    onUpdateQty(it)
                }; onUpdatePrice(priceInput.toDoubleOrNull()); onDismiss()
            },
                enabled = qtyInput.toDoubleOrNull() != null,
                colors = ButtonDefaults.buttonColors(containerColor = Violet500)) {
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