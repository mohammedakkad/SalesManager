package com.trader.salesmanager.ui.boxes

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.core.domain.model.AdjustmentReason
import com.trader.core.domain.model.CashBox
import com.trader.core.domain.model.CashBoxMovement
import com.trader.core.domain.model.CashBoxMovementType
import com.trader.core.domain.model.PaymentType
import com.trader.salesmanager.R
import com.trader.salesmanager.ui.components.AnimatedCounter
import com.trader.salesmanager.ui.components.EmptyState
import com.trader.salesmanager.ui.theme.Cyan500
import com.trader.salesmanager.ui.theme.DebtRed
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.Emerald700
import com.trader.salesmanager.ui.theme.InfoBlue
import com.trader.salesmanager.ui.theme.PaidGreen
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

    var initDialogBox by remember { mutableStateOf<CashBox?>(null) }
    var adjustDialogBox by remember { mutableStateOf<CashBox?>(null) }

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

    adjustDialogBox?.let { box ->
        AdjustBalanceDialog(
            box = box,
            onConfirm = { amount, note, reason ->
                viewModel.adjustBalance(box.id, amount, note, reason)
                adjustDialogBox = null
            },
            onDismiss = { adjustDialogBox = null }
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(scrollState)
        ) {
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
                                stringResource(R.string.boxes_title),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.boxes_subtitle),
                                color = Color.White.copy(0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

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
                                stringResource(R.string.boxes_total_balance),
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
                                stringResource(
                                    R.string.boxes_initialized_count,
                                    uiState.initializedCount,
                                    uiState.boxes.size
                                ),
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
                        title = stringResource(R.string.boxes_empty_title),
                        subtitle = stringResource(R.string.boxes_empty_subtitle)
                    )
                } else {
                    Text(
                        stringResource(R.string.boxes_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.boxes.forEachIndexed { index, box ->
                            BoxCard(
                                box = box,
                                type = uiState.paymentTypes[box.paymentMethodId] ?: PaymentType.OTHER,
                                index = index,
                                isExpanded = uiState.expandedBoxId == box.id,
                                boxMovements = uiState.movementsForBox(box.id),
                                onToggleExpand = { viewModel.toggleBoxExpanded(box.id) },
                                onSetInitialBalance = { initDialogBox = box },
                                onAdjustBalance = { adjustDialogBox = box }
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    Text(
                        stringResource(R.string.boxes_movements_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (uiState.movements.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            EmptyState(
                                icon = Icons.Rounded.Receipt,
                                title = stringResource(R.string.boxes_movements_empty_title),
                                subtitle = stringResource(R.string.boxes_movements_empty_subtitle)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.movements.forEachIndexed { index, movement ->
                                MovementRow(movement = movement, index = index)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun BoxCard(
    box: CashBox,
    type: PaymentType,
    index: Int,
    isExpanded: Boolean,
    boxMovements: List<CashBoxMovement>,
    onToggleExpand: () -> Unit,
    onSetInitialBalance: () -> Unit,
    onAdjustBalance: () -> Unit
) {
    val visible = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    val (icon, color) = boxVisual(box.paymentMethodName, type)

    AnimatedVisibility(
        visible,
        enter = slideInVertically(tween(300, index * 60)) { it / 2 } + fadeIn(tween(300, index * 60))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleExpand)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            box.paymentMethodName.ifEmpty { "—" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (box.isInitialized) {
                                stringResource(
                                    R.string.boxes_last_update,
                                    formatRelativeTime(box.updatedAt)
                                )
                            } else {
                                stringResource(R.string.boxes_awaiting_initial)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (box.isInitialized) {
                        AnimatedCounter(
                            value = box.currentBalance,
                            style = MaterialTheme.typography.headlineSmall,
                            color = color
                        )
                        IconButton(onClick = onAdjustBalance) {
                            Icon(
                                Icons.Rounded.Tune,
                                contentDescription = stringResource(R.string.boxes_adjust_balance),
                                tint = color
                            )
                        }
                        Icon(
                            if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Button(
                            onClick = onSetInitialBalance,
                            colors = ButtonDefaults.buttonColors(containerColor = color),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                stringResource(R.string.boxes_set_initial),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded && box.isInitialized,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.3f))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.boxes_box_movements_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        if (boxMovements.isEmpty()) {
                            Text(
                                stringResource(R.string.boxes_movements_empty_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                boxMovements.forEachIndexed { mIndex, movement ->
                                    MovementRow(movement = movement, index = mIndex, compact = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementRow(
    movement: CashBoxMovement,
    index: Int,
    compact: Boolean = false
) {
    val visible = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    val deltaColor = if (movement.amountDelta >= 0) PaidGreen else DebtRed
    val (typeIcon, typeColor) = movementVisual(movement)
    val signedAmount = formatSignedAmount(movement.amountDelta)

    AnimatedVisibility(
        visible,
        enter = slideInVertically(tween(280, index * 50)) { it / 3 } + fadeIn(tween(280, index * 50))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(if (compact) 12.dp else 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (compact) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(0.35f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(if (compact) 0.dp else 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(if (compact) 10.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 36.dp else 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(typeColor.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(if (compact) 18.dp else 20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        movement.paymentMethodName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        movementTypeLabel(movement.type),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (movement.note.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            movement.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        signedAmount,
                        style = if (compact) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.Bold,
                        color = deltaColor
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatRelativeTime(movement.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdjustBalanceDialog(
    box: CashBox,
    onConfirm: (String, String?, AdjustmentReason) -> Unit,
    onDismiss: () -> Unit
) {
    var amountInput by remember { mutableStateOf(box.currentBalance.toString()) }
    var noteInput by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf(AdjustmentReason.CORRECTION) }
    val reasons = AdjustmentReason.entries

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Edit, null, tint = Cyan500) },
        title = { Text(stringResource(R.string.boxes_adjust_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    stringResource(R.string.boxes_adjust_dialog_body, box.paymentMethodName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text(stringResource(R.string.boxes_amount_label)) },
                    placeholder = { Text(stringResource(R.string.boxes_amount_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text(stringResource(R.string.boxes_note_label)) },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.boxes_reason_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    reasons.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            label = { Text(reasonLabel(reason)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan500.copy(0.15f),
                                selectedLabelColor = Cyan500
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        amountInput.trim(),
                        noteInput.trim().ifBlank { null },
                        selectedReason
                    )
                },
                enabled = amountInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan500)
            ) { Text(stringResource(R.string.boxes_save)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.boxes_cancel))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

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
        title = { Text(stringResource(R.string.boxes_initial_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    stringResource(R.string.boxes_initial_dialog_body, box.paymentMethodName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.boxes_amount_label)) },
                    placeholder = { Text(stringResource(R.string.boxes_amount_placeholder)) },
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
            ) { Text(stringResource(R.string.boxes_save)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.boxes_cancel))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun movementTypeLabel(type: CashBoxMovementType): String = when (type) {
    CashBoxMovementType.INITIAL_BALANCE -> stringResource(R.string.boxes_movement_initial)
    CashBoxMovementType.TRANSACTION_EFFECT -> stringResource(R.string.boxes_movement_transaction)
    CashBoxMovementType.MANUAL_ADJUSTMENT -> stringResource(R.string.boxes_movement_adjustment)
}

@Composable
private fun reasonLabel(reason: AdjustmentReason): String = when (reason) {
    AdjustmentReason.CORRECTION -> stringResource(R.string.boxes_reason_correction)
    AdjustmentReason.COUNT -> stringResource(R.string.boxes_reason_count)
    AdjustmentReason.TRANSFER -> stringResource(R.string.boxes_reason_transfer)
    AdjustmentReason.OTHER -> stringResource(R.string.boxes_reason_other)
}

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

private fun movementVisual(movement: CashBoxMovement): Pair<ImageVector, Color> = when (movement.type) {
    CashBoxMovementType.INITIAL_BALANCE -> Icons.Rounded.Savings to Emerald500
    CashBoxMovementType.TRANSACTION_EFFECT -> Icons.Rounded.Receipt to InfoBlue
    CashBoxMovementType.MANUAL_ADJUSTMENT -> Icons.Rounded.Tune to Violet500
}

@Composable
private fun formatRelativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000L -> stringResource(R.string.boxes_time_now)
        diff < 3_600_000L -> stringResource(R.string.boxes_time_minutes, diff / 60_000)
        diff < 86_400_000L -> stringResource(R.string.boxes_time_hours, diff / 3_600_000)
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(millis))
    }
}

private fun formatSignedAmount(delta: Double): String {
    val prefix = if (delta >= 0) "+" else ""
    return prefix + String.format(Locale.US, "%.2f", delta)
}
