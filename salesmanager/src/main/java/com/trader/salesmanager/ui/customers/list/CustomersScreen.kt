package com.trader.salesmanager.ui.customers.list

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.Customer
import com.trader.salesmanager.ui.components.*
import com.trader.salesmanager.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    onNavigateUp: () -> Unit,
    onCustomerClick: (Long) -> Unit,
    onAddCustomer: () -> Unit,
    viewModel: CustomersViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Customer?>(null) }

    showDeleteDialog?.let { customer ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Rounded.DeleteForever, null, tint = DebtRed) },
            title = { Text("حذف الزبون", fontWeight = FontWeight.Bold) },
            text = { Text("سيتم حذف ${customer.name} وجميع عملياته نهائياً.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteCustomer(customer); showDeleteDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DebtRed)
                ) { Text("حذف") }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteDialog = null }) { Text("إلغاء") } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomer,
                containerColor = Emerald500,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.PersonAdd, null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Header ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Emerald700, Cyan500)))
                    .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("الزبائن", style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${uiState.customers.size} زبون", color = Color.White.copy(0.7f),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    ModernSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::updateSearch,
                        placeholder = "بحث بالاسم..."
                    )
                }
            }

            // ── Content ──────────────────────────────────────────
            AnimatedContent(
                targetState = uiState.customers.isEmpty(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "content"
            ) { empty ->
                if (empty) {
                    EmptyState(
                        icon = Icons.Rounded.People,
                        title = "لا يوجد زبائن بعد",
                        subtitle = "اضغط + لإضافة أول زبون",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(uiState.customers, key = { _, c -> c.id }) { index, customer ->
                            val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                            AnimatedVisibility(
                                visibleState = visible,
                                enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300, delayMillis = index * 40)) + fadeIn()
                            ) {
                                CustomerCard(
                                    customer = customer,
                                    onClick = { onCustomerClick(customer.id) },
                                    onDelete = { showDeleteDialog = customer }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerCard(customer: Customer, onClick: () -> Unit, onDelete: () -> Unit) {
    val initial = customer.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val colors = listOf(Emerald500, Cyan500, Violet500, UnpaidAmber)
    val cardColor = colors[customer.name.length % colors.size]

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(cardColor, cardColor.copy(0.7f)))),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("اضغط لعرض التفاصيل", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = DebtRed.copy(alpha = 0.7f))
            }
            Icon(Icons.Rounded.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}
