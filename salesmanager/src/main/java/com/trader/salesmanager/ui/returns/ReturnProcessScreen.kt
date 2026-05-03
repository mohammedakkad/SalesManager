package com.trader.salesmanager.ui.returns

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.salesmanager.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnProcessScreen(
    transactionId: Long,
    onNavigateUp: () -> Unit,
    onReturnSuccess: () -> Unit,
    viewModel: ReturnViewModel = koinViewModel { parametersOf(transactionId, Unit) }
) {
    val state by viewModel.state.collectAsState()

    // ✅ لا حاجة لـ LaunchedEffect لاستدعاء load() — يحدث في init{}
    LaunchedEffect(state.processingState) {
        if (state.processingState is com.trader.core.domain.model.ReturnUiState.Success) {
            onReturnSuccess()
        }
    }

    // ── حوار القفل (خطة مجانية) ─────────────────────────────────
    if (state.processingState is com.trader.core.domain.model.ReturnUiState.PartialReturnLocked) {
        PartialReturnLockedDialog(
            onDismiss = viewModel::dismissConfirmSheet,
            onUpgrade = { /* TODO: navigate to subscription */ }
        )
    }

    Scaffold(containerColor = appColors.screenBackground) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {

            // ── Header gradient ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Emerald700, PaidGreen)
                        )
                    )
                    .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "إرجاع بضاعة",
                                fontWeight = FontWeight.Bold, color = Color.White,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                "اختر الأصناف والكميات المُرجَعة",
                                color = Color.White.copy(0.75f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // ملخص سريع
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryPill("${state.selectedLines.size} أصناف", Icons.Rounded.Inventory2)
                        SummaryPill(
                            "₪${String.format(Locale.US, "%.2f", state.totalRefund)}",
                            Icons.Rounded.Undo
                        )
                        if (state.totalLostProfit > 0) {
                            SummaryPill(
                                "خسارة ₪${String.format(Locale.US, "%.2f", state.totalLostProfit)}",
                                Icons.Rounded.TrendingDown
                            )
                        }
                    }
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Violet500)
                }
                return@Scaffold
            }

            // ── قائمة الأصناف ────────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "الأصناف القابلة للإرجاع",
                        style = MaterialTheme.typography.labelLarge,
                        color = appColors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                itemsIndexed(state.lines, key = { _, l -> l.invoiceItem.id }) { index, line ->
                    ReturnLineCard(
                        line = line,
                        isPartialEnabled = state.isPartialEnabled,
                        onToggle = { viewModel.toggleLine(index) },
                        onQtyChange = { viewModel.updateQty(index, it) }
                    )
                }

                // ── حقل الملاحظة ──────────────────────────────
                item {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = viewModel::updateNote,
                        label = { Text("سبب الإرجاع (اختياري)") },
                        leadingIcon = { Icon(Icons.Rounded.Notes, null, tint = Violet500) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violet500,
                            focusedContainerColor = appColors.cardBackground,
                            unfocusedContainerColor = appColors.cardBackground
                        )
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }
            }

            // ── زر التأكيد ───────────────────────────────────────
            AnimatedVisibility(
                visible = state.canConfirm,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Surface(
                    shadowElevation = 12.dp,
                    color = appColors.cardBackground
                ) {
                    Button(
                        onClick = viewModel::showConfirmSheet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet500)
                    ) {
                        Icon(Icons.Rounded.Undo, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "تأكيد الإرجاع · ₪${
                                String.format(
                                    Locale.US,
                                    "%.2f",
                                    state.totalRefund
                                )
                            }",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    // ── Bottom Sheet تأكيد ───────────────────────────────────────
    if (state.showConfirmSheet) {
        ConfirmReturnBottomSheet(
            state = state,
            onConfirm = viewModel::confirmReturn,
            onDismiss = viewModel::dismissConfirmSheet
        )
    }
}

// ── بطاقة صنف واحد (Swipe-to-select + Stepper) ──────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReturnLineCard(
    line: ReturnLineState,
    isPartialEnabled: Boolean,
    onToggle: () -> Unit,
    onQtyChange: (Double) -> Unit
) {
    val isEnabled  = line.canReturn
    val isSelected = line.isSelected

    val animatedElevation by animateDpAsState(
        if (isSelected) 10.dp else 0.dp,
        spring(Spring.DampingRatioMediumBouncy), label = "elev"
    )
    val animatedAlpha by animateFloatAsState(
        if (isEnabled) 1f else 0.45f, tween(300), label = "alpha"
    )
    val animatedScale by animateFloatAsState(
        if (isSelected) 1.01f else 1f,
        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium), label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer { alpha = animatedAlpha; scaleX = animatedScale; scaleY = animatedScale }
    ) {
        // ── ظل ملون خلف البطاقة عند الاختيار ──────────────────
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Violet500.copy(0.25f), Cyan500.copy(0.2f)))
                    )
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable(enabled = isEnabled) { onToggle() }
                .then(
                    if (isSelected) Modifier.border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(listOf(Violet500, Cyan500)),
                        shape = RoundedCornerShape(24.dp)
                    ) else Modifier.border(
                        1.dp, appColors.border.copy(0.4f), RoundedCornerShape(24.dp)
                    )
                ),
            color = if (isSelected) Violet500.copy(0.05f) else appColors.cardBackground,
            shadowElevation = animatedElevation,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ── أيقونة الحالة مع Animation ───────────────
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected)
                                    Brush.linearGradient(listOf(Violet500, Cyan500))
                                else
                                    Brush.linearGradient(listOf(appColors.border.copy(0.2f), appColors.border.copy(0.3f)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(targetState = isSelected, animationSpec = tween(250), label = "icon") { selected ->
                            Icon(
                                if (selected) Icons.Rounded.Check else Icons.Rounded.Undo,
                                null,
                                tint = if (selected) Color.White else appColors.textSubtle,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // ── تفاصيل الصنف ─────────────────────────────
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            line.productName,
                            style     = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color      = if (isSelected) Violet500 else appColors.textPrimary,
                            maxLines   = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Badge وحدة
                            Surface(
                                color = if (isSelected) Violet500.copy(0.1f) else appColors.border.copy(0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    line.unitLabel,
                                    modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style      = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = if (isSelected) Violet500 else appColors.textSubtle
                                )
                            }
                            Text("·", color = appColors.textSubtle)
                            Text(
                                "₪${String.format(java.util.Locale.US, "%.2f", line.invoiceItem.pricePerUnit)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = appColors.textSubtle
                            )
                            // الكمية المتبقية
                            if (line.maxReturnable < line.invoiceItem.quantity) {
                                Text("·", color = appColors.textSubtle)
                                Text(
                                    "متبقي ${line.maxReturnable.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = UnpaidAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ── مبلغ الإرجاع بانتقال أنيق ───────────────
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn(tween(200)) + slideInHorizontally { it / 2 },
                        exit  = fadeOut(tween(150)) + slideOutHorizontally { it / 2 }
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "₪${String.format(java.util.Locale.US, "%.2f", line.refundAmount)}",
                                color      = Violet500,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            if (line.lostProfit > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(Icons.Rounded.TrendingDown, null,
                                        tint = DebtRed, modifier = Modifier.size(10.dp))
                                    Text(
                                        "₪${String.format(java.util.Locale.US, "%.2f", line.lostProfit)}",
                                        color = DebtRed,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Stepper كبسولة عائمة ─────────────────────────
                AnimatedVisibility(
                    visible = isSelected && line.maxReturnable > 1 &&
                            (isPartialEnabled || line.maxReturnable == line.invoiceItem.quantity),
                    enter = expandVertically(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit  = shrinkVertically(tween(200)) + fadeOut()
                ) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = appColors.divider.copy(0.5f))
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "الكمية المُرجَعة",
                                style      = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = appColors.textSecondary
                            )
                            // ── Stepper ──────────────────────────
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StepperButton(
                                    icon    = Icons.Rounded.Remove,
                                    enabled = line.returnQty > 1,
                                    onClick = { onQtyChange(line.returnQty - 1) }
                                )
                                // عداد مع animation
                                Box(
                                    modifier = Modifier
                                        .widthIn(min = 72.dp)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Violet500.copy(0.08f), Cyan500.copy(0.06f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${line.returnQty.toInt()} / ${line.maxReturnable.toInt()}",
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Black,
                                        color      = Violet500
                                    )
                                }
                                StepperButton(
                                    icon    = Icons.Rounded.Add,
                                    enabled = line.returnQty < line.maxReturnable,
                                    onClick = { onQtyChange(line.returnQty + 1) }
                                )
                            }
                        }
                    }
                }

                // ── حالة مكتمل الإرجاع ───────────────────────────
                if (!isEnabled) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(appColors.textSubtle.copy(0.06f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null,
                            modifier = Modifier.size(14.dp), tint = PaidGreen.copy(0.7f))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "تم إرجاع كامل الكمية مسبقاً",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = appColors.textSubtle,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (enabled) 1f else 0.85f, label = "btn")
    Box(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(
                if (enabled) Brush.linearGradient(listOf(Violet500, Cyan500))
                else Brush.linearGradient(listOf(appColors.border.copy(0.2f), appColors.border.copy(0.2f)))
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, null,
            modifier = Modifier.size(18.dp),
            tint = if (enabled) Color.White else appColors.textSubtle.copy(0.4f)
        )
    }
}

// ── Bottom Sheet التأكيد النهائي ─────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmReturnBottomSheet(
    state: ReturnScreenState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = appColors.cardBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Violet500.copy(0.1f))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Undo, null, tint = Violet500, modifier = Modifier.size(32.dp)) }

            Text(
                "تأكيد الإرجاع",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // ملخص الأصناف
            state.selectedLines.forEach { line ->
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(
                        "${line.productName} × ${line.returnQty.toInt()} ${line.unitLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = appColors.textPrimary
                    )
                    Text(
                        "₪${String.format(Locale.US, "%.2f", line.refundAmount)}",
                        color = Violet500, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            HorizontalDivider(color = appColors.divider)

            // الإجمالي
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    "إجمالي الاسترداد",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                )
                Text(
                    "₪${String.format(Locale.US, "%.2f", state.totalRefund)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = Violet500
                )
            }

            // تحذير الخسارة
            if (state.totalLostProfit > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DebtRed.copy(0.08f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        null,
                        tint = DebtRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "سيتم تسجيل خسارة ₪${
                            String.format(
                                Locale.US,
                                "%.2f",
                                state.totalLostProfit
                            )
                        } من هامش الربح",
                        color = DebtRed, style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // أزرار
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("إلغاء") }

                Button(
                    onClick = onConfirm,
                    enabled = state.processingState !is com.trader.core.domain.model.ReturnUiState.Loading,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Violet500)
                ) {
                    if (state.processingState is com.trader.core.domain.model.ReturnUiState.Loading) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("تأكيد الإرجاع", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── حوار الميزة المقفولة ─────────────────────────────────────────
@Composable
private fun PartialReturnLockedDialog(onDismiss: () -> Unit, onUpgrade: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.cardBackground,
        icon = {
            Icon(
                Icons.Rounded.Lock,
                null,
                tint = Violet500,
                modifier = Modifier.size(36.dp)
            )
        },
        title = { Text("ميزة مدفوعة", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "الإرجاع الجزئي (اختيار أصناف محددة) متاح في الخطة المتقدمة والبريميوم.\nالخطة المجانية تدعم الإرجاع الكامل فقط.",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(containerColor = Violet500)
            ) {
                Text("ترقية الاشتراك")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

// ── Helper composables ────────────────────────────────────────────
@Composable
private fun SummaryPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(0.15f)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text(
                text, color = Color.White, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
