package com.trader.salesmanager.ui.inventory.list

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import com.trader.core.data.local.appDataStore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.core.domain.model.ProductWithUnits
import com.trader.core.domain.model.SyncStatus
import com.trader.core.domain.model.UnitType
import com.trader.salesmanager.ui.scanner.BarcodeScannerScreen
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors
import com.trader.salesmanager.util.export.ExportTarget
import com.trader.salesmanager.util.export.ExportViewModel
import com.trader.salesmanager.util.export.ExportActionButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    onNavigateUp: () -> Unit,
    onProductClick: (String) -> Unit,
    onAddProduct: (barcode: String?) -> Unit,
    onInventorySession: () -> Unit,
    onStockReports: () -> Unit = {},
    viewModel: InventoryListViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showScanner by remember {
        mutableStateOf(false)
    }
    var showNewProduct by remember {
        mutableStateOf<String?>(null)
    }
    
    val storeName by context.appDataStore.data
    .map {
        it[com.trader.salesmanager.ui.settings.STORE_NAME_KEY] ?: ""
    }
    .collectAsState(initial = "")
    
    val exportVm: ExportViewModel = koinViewModel()
    val exportState by exportVm.state.collectAsState()

    // Dialog: باركود غير موجود
    showNewProduct?.let { barcode ->
        AlertDialog(
            onDismissRequest = {
                showNewProduct = null
            },
            icon = {
                Icon(Icons.Rounded.QrCodeScanner, null, tint = Emerald500)
            },
            title = {
                Text("باركود غير موجود", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("الباركود \"$barcode\" غير موجود في المخزن.\nهل تريد إضافة صنف جديد بهذا الباركود؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNewProduct = null; onAddProduct(barcode)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Text("إضافة صنف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showNewProduct = null
                }) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showScanner) {
        BarcodeScannerScreen(
            onBarcodeDetected = { barcode ->
                showScanner = false
                viewModel.onBarcodeScanned(
                    barcode,
                    onFound = { productId ->
                        onProductClick(productId)
                    },
                    onNotFound = {
                        showNewProduct = barcode
                    }
                )
            },
            onDismiss = {
                showScanner = false
            }
        )
        return
    }

    Scaffold(
        containerColor = appColors.screenBackground,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        onInventorySession()
                    },
                    containerColor = Cyan500,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Inventory2, null, modifier = Modifier.size(18.dp))
                }

                SmallFloatingActionButton(
                    onClick = {
                        onStockReports()
                    },
                    containerColor = Violet500,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.BarChart, null, modifier = Modifier.size(18.dp))
                }

                FloatingActionButton(
                    onClick = {
                        onAddProduct(null)
                    },
                    containerColor = Emerald500,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Add, null)
                }
            }
        }
    ) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(bottom = padding.calculateBottomPadding())) {

            // ── Header ────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Emerald700, Emerald500)))
                    .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Text(
                            "المخزن", fontWeight = FontWeight.Bold, color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f)
                        )
                        com.trader.salesmanager.util.export.ExportActionButton(
                            target         = com.trader.salesmanager.util.export.ExportTarget.INVENTORY_EXCEL,
                            state          = exportState,
                            onExport       = {
                                exportVm.exportInventoryExcel(
                                    products  = state.products,
                                    storeName = storeName,
                                    cacheDir  = context.cacheDir
                                )
                            },
                            onDismissError = exportVm::dismissError
                        )
                        IconButton(
                            onClick = {
                                showScanner = true
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(0.15f))
                        ) {
                            Icon(Icons.Rounded.QrCodeScanner, null, tint = Color.White)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ✅ إضافة شريط حالة المزامنة (من النسخة الأولى)
                    OfflineSyncBanner(
                        isOnline = state.isOnline,
                        pendingSyncCount = state.pendingSyncCount
                    )

                    Spacer(Modifier.height(12.dp))

                    // بحث
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        placeholder = {
                            Text("بحث بالاسم أو الباركود...", color = Color.White.copy(0.5f))
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.7f))
                        },
                        trailingIcon = {
                            AnimatedVisibility(state.query.isNotEmpty()) {
                                IconButton(onClick = {
                                    viewModel.setQuery("")
                                }) {
                                    Icon(Icons.Rounded.Close, null, tint = Color.White.copy(0.7f))
                                }
                            }
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
                }
            }

            // ── إحصائيات سريعة ────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(appColors.cardBackground)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChip(
                    Modifier.weight(1f),
                    "${state.totalProducts}",
                    "صنف",
                    Emerald500,
                    Icons.Rounded.Inventory
                )
                StatChip(
                    Modifier.weight(1f),
                    "${state.lowStockCount}",
                    "نقص",
                    UnpaidAmber,
                    Icons.Rounded.Warning
                )
                StatChip(
                    Modifier.weight(1f),
                    "${state.outOfStockCount}",
                    "نفد",
                    DebtRed,
                    Icons.Rounded.RemoveShoppingCart
                )
            }

            // ── فلاتر ─────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StockFilter.entries.forEach { f ->
                    val label = when (f) {
                        StockFilter.ALL -> "الكل"
                        StockFilter.LOW -> "نقص"
                        StockFilter.OUT -> "نفد"
                    }
                    val sel = state.filter == f
                    FilterChip(
                        selected = sel,
                        onClick = {
                            viewModel.setFilter(f)
                        },
                        label = {
                            Text(
                                label,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (f) {
                                StockFilter.ALL -> Emerald500.copy(0.15f)
                                StockFilter.LOW -> UnpaidAmber.copy(0.15f)
                                StockFilter.OUT -> DebtRed.copy(0.15f)
                            },
                            selectedLabelColor = when (f) {
                                StockFilter.ALL -> Emerald500
                                StockFilter.LOW -> UnpaidAmber
                                StockFilter.OUT -> DebtRed
                            },
                            containerColor = appColors.cardBackgroundVariant
                        )
                    )
                }
            }

            // ── قائمة الأصناف ─────────────────────────────────────
            AnimatedContent(
                targetState = state.isLoading to state.filtered.isEmpty(),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "list"
            ) { (loading, empty) ->
                when {
                    loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = Emerald500)
                    }

                    empty -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.Inventory2, null,
                                Modifier.size(64.dp), tint = appColors.divider
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (state.query.isNotEmpty()) "لا توجد نتائج" else "المخزن فارغ\nأضف أصنافك الآن",
                                color = appColors.textSubtle, textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                "${state.filtered.size} صنف",
                                style = MaterialTheme.typography.labelMedium,
                                color = appColors.textSubtle,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(state.filtered, key = {
                            it.product.id
                        }) { item ->
                            ProductCard(item = item, onClick = {
                                onProductClick(item.product.id)
                            })
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

// ✅ دالة شريط المزامنة (من النسخة الأولى)
@Composable
private fun OfflineSyncBanner(isOnline: Boolean, pendingSyncCount: Int) {
    AnimatedVisibility(
        visible = !isOnline || pendingSyncCount > 0,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val (bgColor, icon, message, color) = when {
            !isOnline -> arrayOf(
                Color(0xFFEF4444).copy(0.15f),
                Icons.Rounded.WifiOff,
                "بدون إنترنت — بياناتك محفوظة محلياً",
                Color(0xFFEF4444)
            )

            pendingSyncCount > 0 -> arrayOf(
                UnpaidAmber.copy(0.15f),
                Icons.Rounded.Sync,
                "جارٍ مزامنة $pendingSyncCount صنف...",
                UnpaidAmber
            )

            else -> arrayOf(Color.Transparent, Icons.Rounded.Check, "", Color.Transparent)
        }

        val rotation by rememberInfiniteTransition(label = "").animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)), label = ""
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = bgColor as Color,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon as ImageVector,
                    contentDescription = null,
                    tint = color as Color,
                    modifier = Modifier.size(16.dp).let {
                        if (icon == Icons.Rounded.Sync) it.graphicsLayer {
                            rotationZ = rotation
                        } else it
                    }
                )
                Text(
                    message as String,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier,
    value: String,
    label: String,
    color: Color,
    icon: ImageVector
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = color.copy(0.08f)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Column {
                Text(
                    value, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = color
                )
                Text(
                    label, style = MaterialTheme.typography.labelSmall,
                    color = color.copy(0.7f)
                )
            }
        }
    }
}

@Composable
private fun ProductCard(item: ProductWithUnits, onClick: () -> Unit) {
    val statusColor = when {
        item.isOutOfStock -> DebtRed
        item.isLowStock -> UnpaidAmber
        else -> PaidGreen
    }
    val statusLabel = when {
        item.isOutOfStock -> "نفد"
        item.isLowStock -> "نقص"
        else -> "متوفر"
    }

    // ✅ تحديد إذا الصنف لم يُزامن بعد (من النسخة الأولى)
    val isPending = item.units.isNotEmpty() && item.units.all {
        it.syncStatus == SyncStatus.PENDING
    }

    val initial = item.product.name.firstOrNull()?.toString() ?: "؟"
    val bgColors = listOf(Emerald500, Cyan500, Violet500, UnpaidAmber)
    val bg = bgColors[item.product.name.length % bgColors.size]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initial,
                    color = bg,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        item.product.name, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false)
                    )

                    // ✅ مؤشر "محلي" (من النسخة الأولى)
                    if (isPending) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = UnpaidAmber.copy(0.15f)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.CloudOff,
                                    null,
                                    modifier = Modifier.size(10.dp),
                                    tint = UnpaidAmber
                                )
                                Text(
                                    "محلي",
                                    fontSize = 9.sp,
                                    color = UnpaidAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                if (item.product.category.isNotEmpty()) {
                    Text(
                        item.product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.textSubtle
                    )
                }
                Spacer(Modifier.height(6.dp))

                // وحدات الصنف (تأخذ أول 3 فقط لمنع التكدس)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.units.take(3).forEach { unit ->
                        val qty = unit.quantityInStock
                        val qtyText = if (unit.unitType == UnitType.WEIGHT)
                            "${String.format("%.2f", qty)} ${unit.unitLabel}"
                        else "${qty.toInt()} ${unit.unitLabel}"
                        Surface(shape = RoundedCornerShape(6.dp), color = appColors.divider) {
                            Text(
                                qtyText,
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = appColors.textSecondary
                            )
                        }
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(0.12f)) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor))
                        Text(
                            statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                item.defaultUnit?.let { unit ->
                    Text(
                        "₪${String.format("%.2f", unit.price)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    null,
                    tint = appColors.divider,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}