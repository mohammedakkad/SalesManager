package com.trader.salesmanager.ui.reports

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.TransactionRepository
import com.trader.salesmanager.ui.components.StatusChip
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.*

// ── ViewModel ────────────────────────────────────────────────────
data class DayTxUiState(
    val all: List<Transaction> = emptyList(),
    val query: String = "",
    val filterPaid: Boolean? = null,   // null=all, true=paid, false=unpaid
    val filterPaymentMethod: String = "",
    val paymentMethods: List<String> = emptyList(),
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val unpaidAmount: Double = 0.0,
    val isLoading: Boolean = true
) {
    val filtered: List<Transaction>
        get() = all.filter { tx ->
            val matchQuery = query.isEmpty() ||
                    tx.customerName.contains(query, ignoreCase = true)
            val matchPaid = filterPaid == null || tx.isPaid == filterPaid
            val matchMethod = filterPaymentMethod.isEmpty() ||
                    tx.paymentMethodName == filterPaymentMethod
            matchQuery && matchPaid && matchMethod
        }
}

class DayTransactionsViewModel(
    private val repo: TransactionRepository,
    private val dateMillis: Long
) : ViewModel() {

    private val startOfDay: Long
    private val endOfDay: Long

    init {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(
            Calendar.SECOND,
            0
        ); cal.set(Calendar.MILLISECOND, 0)
        startOfDay = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(
            Calendar.SECOND,
            59
        ); cal.set(Calendar.MILLISECOND, 999)
        endOfDay = cal.timeInMillis
    }

    private val _query = MutableStateFlow("")
    private val _paid = MutableStateFlow<Boolean?>(null)
    private val _method = MutableStateFlow("")

    val uiState: StateFlow<DayTxUiState> = combine(
        repo.getTransactionsByDate(startOfDay, endOfDay),
        _query, _paid, _method
    ) { txs, q, paid, method ->
        val methods =
            txs.map { it.paymentMethodName }.filter { it.isNotEmpty() }.distinct().sorted()
        DayTxUiState(
            all = txs,
            query = q,
            filterPaid = paid,
            filterPaymentMethod = method,
            paymentMethods = methods,
            totalAmount = txs.sumOf { it.amount },
            paidAmount = txs.filter { it.isPaid }.sumOf { it.amount },
            unpaidAmount = txs.filter { !it.isPaid }.sumOf { it.amount },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayTxUiState())

    fun setQuery(q: String) {
        _query.value = q
    }

    fun setFilterPaid(v: Boolean?) {
        _paid.value = v
    }

    fun setMethod(m: String) {
        _method.value = m
    }
}

// ── Screen ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayTransactionsScreen(
    dateMillis: Long,
    onNavigateUp: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: DayTransactionsViewModel = koinViewModel(parameters = { parametersOf(dateMillis) })
) {
    val state by viewModel.uiState.collectAsState()
    val dateLabel = remember(dateMillis) {
        SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(Date(dateMillis))
    }
    var showFilterSheet by remember { mutableStateOf(false) }

    if (showFilterSheet) {
        FilterBottomSheet(
            state = state,
            onDismiss = { showFilterSheet = false },
            onSetPaid = viewModel::setFilterPaid,
            onSetMethod = viewModel::setMethod
        )
    }

    Scaffold(
        containerColor = appColors.screenBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {

            // ── Gradient Header ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Violet500, Cyan500)))
                    .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "عمليات اليوم", fontWeight = FontWeight.Bold,
                                color = Color.White, style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                dateLabel, color = Color.White.copy(0.75f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        // Filter button with badge
                        val hasFilter =
                            state.filterPaid != null || state.filterPaymentMethod.isNotEmpty()
                        BadgedBox(badge = {
                            if (hasFilter) Badge(containerColor = Color(0xFFF59E0B))
                        }) {
                            IconButton(
                                onClick = { showFilterSheet = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.15f))
                            ) {
                                Icon(Icons.Rounded.Tune, null, tint = Color.White)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Search bar
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        placeholder = {
                            Text(
                                "بحث بالاسم أو رقم الهاتف...",
                                color = Color.White.copy(0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Search,
                                null,
                                tint = Color.White.copy(0.7f)
                            )
                        },
                        trailingIcon = {
                            AnimatedVisibility(state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setQuery("") }) {
                                    Icon(Icons.Rounded.Close, null, tint = Color.White.copy(0.7f))
                                }
                            }
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
                }
            }

            // ── Summary Strip ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryChip(Modifier.weight(1f), "الإجمالي", state.totalAmount, Violet500)
                SummaryChip(Modifier.weight(1f), "مدفوع", state.paidAmount, PaidGreen)
                SummaryChip(Modifier.weight(1f), "غير مدفوع", state.unpaidAmount, DebtRed)
            }

            // ── Transactions List ────────────────────────────────
            AnimatedContent(
                targetState = state.filtered.isEmpty() to state.isLoading,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "list"
            ) { (empty, loading) ->
                when {
                    loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = Violet500)
                    }

                    empty -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.SearchOff, null,
                                modifier = Modifier.size(56.dp), tint = appColors.divider
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (state.query.isNotEmpty()) "لا توجد نتائج للبحث"
                                else "لا توجد عمليات في هذا اليوم",
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
                                "${state.filtered.size} عملية",
                                style = MaterialTheme.typography.labelMedium,
                                color = appColors.textSubtle,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        itemsIndexed(state.filtered, key = { _, t -> t.id }) { index, tx ->
                            val visible = remember {
                                MutableTransitionState(false).apply {
                                    targetState = true
                                }
                            }
                            AnimatedVisibility(
                                visibleState = visible,
                                enter = slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = tween(250, index * 30)
                                ) + fadeIn()
                            ) {
                                DayTxCard(tx = tx, onClick = { onTransactionClick(tx.id) })
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(modifier: Modifier, label: String, value: Double, color: Color) {
    val animated by animateFloatAsState(
        value.toFloat(),
        spring(Spring.DampingRatioMediumBouncy),
        label = "v"
    )
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = appColors.textSubtle)
        Spacer(Modifier.height(2.dp))
        Text(
            "₪${String.format("%,.0f", animated)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun DayTxCard(tx: Transaction, onClick: () -> Unit) {
    val initial = tx.customerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val colors = listOf(Violet500, Emerald500, Cyan500, UnpaidAmber, DebtRed)
    val bg = colors[tx.customerName.length % colors.size]
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(bg.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initial, color = bg, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tx.customerName, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (tx.paymentMethodName.isNotEmpty())
                    Text(
                        tx.paymentMethodName, style = MaterialTheme.typography.bodySmall,
                        color = appColors.textSubtle
                    )
                if (tx.note.isNotEmpty())
                    Text(
                        tx.note, style = MaterialTheme.typography.labelSmall,
                        color = appColors.textSubtle, maxLines = 1
                    )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₪${String.format("%,.0f", tx.amount)}",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = if (tx.isPaid) PaidGreen else appColors.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                StatusChip(isPaid = tx.isPaid)
            }
        }
    }
}

// ── Filter Bottom Sheet ───────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    state: DayTxUiState,
    onDismiss: () -> Unit,
    onSetPaid: (Boolean?) -> Unit,
    onSetMethod: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = appColors.cardBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    "الفلاتر",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (state.filterPaid != null || state.filterPaymentMethod.isNotEmpty()) {
                    TextButton(onClick = { onSetPaid(null); onSetMethod("") }) {
                        Text(
                            "مسح الكل",
                            color = DebtRed,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Paid filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "حالة الدفع", style = MaterialTheme.typography.labelLarge,
                    color = appColors.textSecondary, fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "الكل", true to "مدفوع", false to "غير مدفوع")
                        .forEach { (value, label) ->
                            val sel = state.filterPaid == value
                            FilterChip(
                                selected = sel,
                                onClick = { onSetPaid(value) },
                                label = {
                                    Text(
                                        label,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (value == true) PaidGreen.copy(0.15f)
                                    else if (value == false) DebtRed.copy(0.15f)
                                    else Violet500.copy(0.12f),
                                    selectedLabelColor = if (value == true) PaidGreen
                                    else if (value == false) DebtRed
                                    else Violet500,
                                    containerColor = appColors.cardBackgroundVariant,
                                    labelColor = appColors.textSecondary
                                ),
                                leadingIcon = if (sel) ({
                                    Icon(
                                        Icons.Rounded.Check, null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (value == true) PaidGreen
                                        else if (value == false) DebtRed
                                        else Violet500
                                    )
                                }) else null
                            )
                        }
                }
            }

            // Payment method filter
            if (state.paymentMethods.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "طريقة الدفع", style = MaterialTheme.typography.labelLarge,
                        color = appColors.textSecondary, fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(
                            rememberScrollState()
                        )
                    ) {
                        // All
                        FilterChip(
                            selected = state.filterPaymentMethod.isEmpty(),
                            onClick = { onSetMethod("") },
                            label = { Text("الكل") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan500.copy(0.12f),
                                selectedLabelColor = Cyan500,
                                containerColor = appColors.cardBackgroundVariant
                            )
                        )
                        state.paymentMethods.forEach { method ->
                            val sel = state.filterPaymentMethod == method
                            FilterChip(
                                selected = sel,
                                onClick = { onSetMethod(method) },
                                label = {
                                    Text(
                                        method,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Cyan500.copy(0.12f),
                                    selectedLabelColor = Cyan500,
                                    containerColor = appColors.cardBackgroundVariant
                                )
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet500)
            ) { Text("تطبيق", fontWeight = FontWeight.Bold) }
        }
    }
}

// needed for horizontal scroll
@Composable
private fun rememberScrollState() = androidx.compose.foundation.rememberScrollState()
