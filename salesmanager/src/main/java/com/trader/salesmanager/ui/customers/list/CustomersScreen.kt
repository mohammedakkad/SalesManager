package com.trader.salesmanager.ui.customers.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.SyncStatus
import com.trader.salesmanager.ui.components.EmptyState
import com.trader.salesmanager.ui.components.ModernSearchBar
import com.trader.salesmanager.ui.theme.Cyan500
import com.trader.salesmanager.ui.theme.DebtRed
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.Emerald700
import com.trader.salesmanager.ui.theme.UnpaidAmber
import com.trader.salesmanager.ui.theme.Violet500
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    onNavigateUp: () -> Unit,
    onCustomerClick: (Long) -> Unit,
    onAddCustomer: () -> Unit,
    showNavigateUp: Boolean = true,
    viewModel: CustomersViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deleteConfirm by viewModel.deleteConfirm.collectAsStateWithLifecycle()

    // ✅ dialog حذف ذكي — يعرض عدد العمليات المرتبطة
    deleteConfirm?.let { state ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            icon = {
                Icon(Icons.Rounded.DeleteForever, null, tint = DebtRed)
            },
            title = {
                Text("حذف الزبون", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.transactionCount > 0) {
                        // ⚠️ تحذير واضح عند وجود عمليات
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DebtRed.copy(alpha = 0.1f)
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Warning, null,
                                    tint = DebtRed, modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "يملك ${state.transactionCount} عملية مرتبطة",
                                    color = DebtRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Text(
                        if (state.transactionCount > 0)
                            "حذف \"${state.customer.name}\" سيجعل عملياته تظهر بدون اسم زبون."
                        else
                            "هل تريد حذف \"${state.customer.name}\" نهائياً؟"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = DebtRed)
                ) {
                    Text(if (state.transactionCount > 0) "حذف رغم ذلك" else "حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::dismissDelete) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = padding.calculateBottomPadding())) {
            // ── Header ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Emerald700, Cyan500)))
                    .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showNavigateUp) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "الزبائن", style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = Color.White
                            )
                            Text(
                                "${uiState.customers.size} زبون", color = Color.White.copy(0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        // ✅ badge المزامنة
                        androidx.compose.animation.AnimatedVisibility(uiState.pendingSyncCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(0.2f)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = Color.White,
                                        strokeWidth = 1.5.dp
                                    )
                                    Text(
                                        "جاري رفع ${uiState.pendingSyncCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
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
                targetState = Triple(uiState.isLoading, uiState.customers.isEmpty(), uiState.searchQuery),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "content"
            ) { (loading, empty, _) ->
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Emerald500)
                    }
                } else if (empty) {
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
                        itemsIndexed(uiState.customers, key = { _, c ->
                            c.id
                        }) { index, customer ->
                            val visible = remember {
                                MutableTransitionState(false).apply {
                                    targetState = true
                                }
                            }
                            AnimatedVisibility(
                                visibleState = visible,
                                enter = slideInVertically(initialOffsetY = {
                                    it / 2
                                }, animationSpec = tween(300, delayMillis = index * 40)) + fadeIn()
                            ) {
                                CustomerCard(
                                    customer = customer,
                                    onClick = {
                                        onCustomerClick(customer.id)
                                    },
                                    onDelete = {
                                        viewModel.requestDelete(customer)
                                    }
                                )
                            }
                        }
                        item {
                            Spacer(Modifier.height(72.dp))
                        }
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
                Text(
                    initial,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        customer.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // ✅ نفس badge المزامنة في شاشة العمليات
                    if (customer.syncStatus == SyncStatus.PENDING) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = UnpaidAmber.copy(alpha = 0.15f)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.CloudOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = UnpaidAmber
                                )
                                Text(
                                    "جاري المزامنة",
                                    fontSize = 9.sp,
                                    color = UnpaidAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Text(
                    if (customer.phone.isNotEmpty()) customer.phone else "اضغط لعرض التفاصيل",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // ✅ إخفاء زر الحذف للزبون الزائر (id = -1)
            if (customer.id != -1L) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = DebtRed.copy(alpha = 0.7f))
                }
            }
            Icon(
                Icons.Rounded.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)
            )
        }
    }
}