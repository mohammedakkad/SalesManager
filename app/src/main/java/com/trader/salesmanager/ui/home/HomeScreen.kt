package com.trader.salesmanager.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.salesmanager.ui.components.AnimatedCounter
import com.trader.salesmanager.ui.components.GradientCard
import com.trader.salesmanager.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCustomers: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("عملية جديدة", fontWeight = FontWeight.SemiBold) },
                containerColor = Emerald500,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // ── Header Gradient ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Emerald700, Cyan500)))
                    .padding(top = 48.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "مرحباً 👋",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "مدير المبيعات",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Rounded.Settings, null, tint = Color.White)
                    }
                }
            }

            // ── Stats Cards ─────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height((-20).dp))

                // Big total card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "إجمالي اليوم",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        AnimatedCounter(
                            value = uiState.todayTotal,
                            style = MaterialTheme.typography.displayMedium,
                            color = Emerald500
                        )
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MiniStat(
                                modifier = Modifier.weight(1f),
                                label = "مدفوع",
                                value = uiState.todayPaid,
                                color = PaidGreen
                            )
                            VerticalDivider(modifier = Modifier.height(40.dp), color = MaterialTheme.colorScheme.outline.copy(0.3f))
                            MiniStat(
                                modifier = Modifier.weight(1f),
                                label = "غير مدفوع",
                                value = uiState.todayUnpaid,
                                color = UnpaidAmber
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Quick Navigation ──────────────────────────────
                Text(
                    "القوائم",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val navItems = listOf(
                    NavItem("الزبائن",   Icons.Rounded.People,      Emerald500, onNavigateToCustomers),
                    NavItem("العمليات",  Icons.Rounded.Receipt,     Cyan500,    onNavigateToTransactions),
                    NavItem("التقارير",  Icons.Rounded.BarChart,    Violet500,  onNavigateToReports),
                    NavItem("الديون",    Icons.Rounded.Warning,     DebtRed,    onNavigateToDebts),
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    navItems.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { item ->
                                NavCard(
                                    modifier = Modifier.weight(1f),
                                    item = item
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun MiniStat(modifier: Modifier = Modifier, label: String, value: Double, color: Color) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        AnimatedCounter(value = value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

private data class NavItem(val label: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

@Composable
private fun NavCard(modifier: Modifier = Modifier, item: NavItem) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "scale")

    Card(
        modifier = modifier
            .clickable {
                pressed = true
                item.onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = item.color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, null, tint = item.color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(item.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Icon(Icons.Default.ArrowForward, null, tint = item.color, modifier = Modifier.size(14.dp))
        }
    }
}
