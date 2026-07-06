package com.trader.salesmanager.ui.boxes

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.core.domain.model.CashBox
import com.trader.core.domain.model.PaymentType
import com.trader.salesmanager.ui.components.AnimatedCounter
import com.trader.salesmanager.ui.components.EmptyState
import com.trader.salesmanager.ui.theme.Cyan500
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.Emerald700
import com.trader.salesmanager.ui.theme.InfoBlue
import com.trader.salesmanager.ui.theme.UnpaidAmber
import com.trader.salesmanager.ui.theme.Violet500
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HEADER_CONTENT_HEIGHT = 140.dp
private val OVERLAP = 40.dp

@Composable
fun BoxesScreen(
    onNavigateUp: () -> Unit,
    viewModel: BoxesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    // الصندوق الذي يُحدَّد رصيده الابتدائي حالياً
    var initDialogBox by remember { mutableStateOf<CashBox?>(null) }

    initDialogBox?.let { box ->
        InitialBalanceDialog(
            box = box,
            onConfirm = { amount ->
                viewModel.setInitialBalance(box.id, amount)
                initDialogBox = null
            },
            onDismiss = { initDialogBox = null }
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(scrollState)
        ) {
            // ── Header + Total Card (نفس نمط HomeScreen) ─────────
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HEADER_CONTENT_HEIGHT + OVERLAP)
                        .background(
                            Brush.linearGradient(
                                listOf(Emerald700, Cyan500),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(
                                    Float.POSITIVE_INFINITY,
                                    Float.POSITIVE_INFINITY
                                )
                            )
                        )
                )
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 20.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateUp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(0.2f))
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "الصناديق",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "رصيد كل طريقة دفع لحظياً",
                                color = Color.White.copy(0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── بطاقة الرصيد الإجمالي ────────────────────
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .offset(y = OVERLAP / 2)
                            .shadow(12.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "الرصيد الإجمالي",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            AnimatedCounter(
                                value = uiState.totalBalance,
                                style = MaterialTheme.typography.displayMedium,
                                color = Emerald500
                            )
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.4f))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "${uiState.initializedCount} من ${uiState.boxes.size} صناديق مهيّأة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(OVERLAP / 2 + 24.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (!uiState.isLoading && uiState.boxes.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.Inbox,
                        title = "لا توجد صناديق بعد",
                        subtitle = "أضف طريقة دفع أولاً — سيُنشأ صندوقها تلقائياً"
                    )
                } else {
                    Text(
                        "صناديق طرق الدفع",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.boxes.forEachIndexed { index, box ->
                            BoxCard(
                                box = box,
                                type = uiState.paymentTypes[box.paymentMethodId]
                                    ?: PaymentType.OTHER,
                                index = index,
                                onSetInitialBalance = { initDialogBox = box }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ── بطاقة صندوق ───────────────────────────────────────────────
@Composable
private fun BoxCard(
    box: CashBox,
    type: PaymentType,
    index: Int,
    onSetInitialBalance: () -> Unit
) {
    val visible = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }
    val (icon, color) = boxVisual(box.paymentMethodName, type)

    AnimatedVisibility(
        visible,
        enter = slideInVertically(tween(300, index * 60)) {
            it / 2
        } + fadeIn(tween(300, index * 60))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // أيقونة طريقة الدفع
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                }
                // اسم الطريقة + آخر تحديث
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        box.paymentMethodName.ifEmpty {
                            "—"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (box.isInitialized) "آخر تحديث ${formatUpdatedAt(box.updatedAt)}"
                        else "بانتظار تحديد الرصيد",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // الرصيد — بطل البطاقة
                if (box.isInitialized) {
                    AnimatedCounter(
                        value = box.currentBalance,
                        style = MaterialTheme.typography.headlineSmall,
                        color = color
                    )
                } else {
                    Button(
                        onClick = onSetInitialBalance,
                        colors = ButtonDefaults.buttonColors(containerColor = color),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "حدد الرصيد الابتدائي",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ── حوار الرصيد الابتدائي (مرة واحدة لكل صندوق) ────────────────
@Composable
private fun InitialBalanceDialog(
    box: CashBox,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Savings, null, tint = Emerald500) },
        title = { Text("تحديد الرصيد الابتدائي", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "الرصيد الحالي في «${box.paymentMethodName}» الآن — يُحدَّد مرة واحدة فقط",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("المبلغ") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(input.trim()) },
                enabled = input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
            ) { Text("حفظ") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("إلغاء") } },
        shape = RoundedCornerShape(20.dp)
    )
}

// ── Helpers ───────────────────────────────────────────────────
/** أيقونة + لون حسب نوع طريقة الدفع، مع ترجيح بالاسم عندما يكون النوع عاماً */
private fun boxVisual(name: String, type: PaymentType): Pair<ImageVector, Color> {
    val effectiveType = if (type == PaymentType.OTHER) {
        when {
            name.contains("بنك") -> PaymentType.BANK
            name.contains("كاش") -> PaymentType.CASH
            name.contains("محفظة") || name.contains("pay", ignoreCase = true) -> PaymentType.WALLET
            else -> PaymentType.OTHER
        }
    } else type

    return when (effectiveType) {
        PaymentType.BANK -> Icons.Rounded.AccountBalance to InfoBlue
        PaymentType.CASH -> Icons.Rounded.Payments to Emerald500
        PaymentType.WALLET -> Icons.Rounded.AccountBalanceWallet to Violet500
        PaymentType.DEBT -> Icons.Rounded.Payment to UnpaidAmber
        PaymentType.OTHER -> Icons.Rounded.Payment to Cyan500
    }
}

private fun formatUpdatedAt(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000L -> "الآن"
        diff < 3_600_000L -> "قبل ${diff / 60_000} د"
        diff < 86_400_000L -> "قبل ${diff / 3_600_000} س"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(millis))
    }
}
