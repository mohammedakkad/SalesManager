package com.trader.salesmanager.ui.inventory.reports

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.core.domain.model.ProductWithUnits
import com.trader.core.domain.model.UnitType
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun StockReportsScreen(
    onNavigateUp: () -> Unit,
    viewModel: StockReportsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LazyColumn(
        Modifier.fillMaxSize().background(appColors.screenBackground)
    ) {
        // ── Header ────────────────────────────────────────────────
        item {
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0F766E), Emerald500)))
                    .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Text("تقارير المخزن والكاش",
                            fontWeight = FontWeight.Bold, color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    // تبويبات
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.15f))
                    ) {
                        listOf("المخزن", "الكاش", "تنبيهات").forEachIndexed { i, label ->
                            val sel = selectedTab == i
                            Box(
                                Modifier.weight(1f).padding(4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) Color.White else Color.Transparent)
                                    .clickable { selectedTab = i }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label,
                                    color = if (sel) Emerald500 else Color.White,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> stockTab(state)
            1 -> cashTab(state)
            2 -> alertsTab(state)
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

private fun LazyListScope.stockTab(state: StockReportsUiState) {
    // قيمة المخزن الكلية
    item {
        BigValueCard(
            label = "قيمة المخزن الإجمالية",
            value = "₪${String.format("%,.2f", state.totalProductsValue)}",
            icon = Icons.Rounded.Inventory2,
            gradient = listOf(Emerald500, Color(0xFF059669)),
            modifier = Modifier.padding(16.dp)
        )
    }

    // إحصائيات سريعة
    item {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniStatCard(
                Modifier.weight(1f),
                "${state.stockItems.size}",
                "وحدة مخزنة",
                Emerald500, Icons.Rounded.Category
            )
            MiniStatCard(
                Modifier.weight(1f),
                "${state.lowStockItems.size}",
                "نقص مخزون",
                UnpaidAmber, Icons.Rounded.Warning
            )
            MiniStatCard(
                Modifier.weight(1f),
                "${state.outOfStockItems.size}",
                "نفد المخزون",
                DebtRed, Icons.Rounded.RemoveShoppingCart
            )
        }
    }

    if (state.stockItems.isNotEmpty()) {
        item {
            Text("تفاصيل المخزن",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = appColors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        items(state.stockItems, key = { it.unitId }) { item ->
            StockItemRow(item)
        }
    }
}

private fun LazyListScope.cashTab(state: StockReportsUiState) {
    item {
        BigValueCard(
            label = "إجمالي الكاش المحصّل",
            value = "₪${String.format("%,.2f", state.cashSummary.totalCash)}",
            icon = Icons.Rounded.Payments,
            gradient = listOf(PaidGreen, Color(0xFF16A34A)),
            modifier = Modifier.padding(16.dp)
        )
    }

    item {
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تفصيل الكاش", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)

                CashRow("اليوم", state.cashSummary.todayCash, Emerald500)
                CashRow("الأسبوع الماضي", state.cashSummary.weekCash, Cyan500)
                CashRow("آخر 30 يوم", state.cashSummary.monthCash, Violet500)

                HorizontalDivider(color = appColors.divider)

                CashRow("إجمالي الديون غير المسددة",
                    state.cashSummary.totalDebt, DebtRed)
            }
        }
    }
}

private fun LazyListScope.alertsTab(state: StockReportsUiState) {
    if (state.outOfStockItems.isEmpty() && state.lowStockItems.isEmpty()) {
        item {
            Box(
                Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CheckCircle, null,
                        Modifier.size(64.dp), tint = PaidGreen)
                    Spacer(Modifier.height(12.dp))
                    Text("كل الأصناف بكميات كافية ✅",
                        fontWeight = FontWeight.Bold, color = PaidGreen)
                }
            }
        }
        return
    }

    if (state.outOfStockItems.isNotEmpty()) {
        item {
            AlertSectionHeader("نفد من المخزون", "${state.outOfStockItems.size} صنف", DebtRed)
        }
        items(state.outOfStockItems, key = { "out_${it.product.id}" }) { p ->
            AlertProductCard(p, DebtRed)
        }
    }

    if (state.lowStockItems.isNotEmpty()) {
        item {
            AlertSectionHeader("كمية منخفضة", "${state.lowStockItems.size} صنف", UnpaidAmber)
        }
        items(state.lowStockItems, key = { "low_${it.product.id}" }) { p ->
            AlertProductCard(p, UnpaidAmber)
        }
    }
}

// ── Composables مساعدة ────────────────────────────────────────

@Composable
private fun BigValueCard(
    label: String, value: String,
    icon: ImageVector, gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    val anim by animateFloatAsState(1f, spring(Spring.DampingRatioMediumBouncy), label = "v")
    Card(modifier, shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(gradient))
                .padding(20.dp)
        ) {
            Column {
                Icon(icon, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(8.dp))
                Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineMedium)
                Text(label, color = Color.White.copy(0.8f),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MiniStatCard(
    modifier: Modifier, value: String, label: String,
    color: Color, icon: ImageVector
) {
    Card(modifier, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = color.copy(0.7f), textAlign = TextAlign.Center, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StockItemRow(item: StockReportItem) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                    .background(Emerald500.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (item.unitType) {
                        UnitType.WEIGHT -> Icons.Rounded.Scale
                        UnitType.CARTON -> Icons.Rounded.Inventory2
                        else            -> Icons.Rounded.Category
                    },
                    null, tint = Emerald500, modifier = Modifier.size(18.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(item.productName, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.unitLabel, style = MaterialTheme.typography.labelSmall,
                    color = appColors.textSubtle)
            }
            val qtyStr = when (item.unitType) {
                UnitType.WEIGHT -> "${String.format("%.3f", item.currentQty).trimEnd('0').trimEnd('.')} كجم"
                else            -> "${item.currentQty.toInt()}"
            }
            Text(qtyStr, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    item.currentQty <= 0 -> DebtRed
                    else                 -> appColors.textPrimary
                })
        }
    }
}

@Composable
private fun CashRow(label: String, amount: Double, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = appColors.textSecondary)
        Text("₪${String.format("%,.2f", amount)}",
            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium,
            color = color)
    }
}

@Composable
private fun AlertSectionHeader(title: String, count: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.Warning, null, tint = color, modifier = Modifier.size(18.dp))
        Text(title, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall, color = color)
        Spacer(Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(20.dp), color = color.copy(0.12f)) {
            Text(count, Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall, color = color,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AlertProductCard(product: ProductWithUnits, color: Color) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.06f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(color.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(product.product.name.firstOrNull()?.toString() ?: "؟",
                    color = color, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall)
            }
            Column(Modifier.weight(1f)) {
                Text(product.product.name, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall)
                val qtyText = product.units.joinToString(" • ") { u ->
                    "${u.unitLabel}: ${u.quantityInStock.let {
                        if (it == it.toLong().toDouble()) it.toLong().toString()
                        else String.format("%.2f", it)
                    }}"
                }
                Text(qtyText, style = MaterialTheme.typography.labelSmall, color = color)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = color.copy(0.4f),
                modifier = Modifier.size(16.dp))
        }
    }
}
