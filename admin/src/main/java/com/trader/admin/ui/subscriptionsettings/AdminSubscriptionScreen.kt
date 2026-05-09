package com.trader.admin.ui.subscriptionsettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.admin.ui.theme.Cyan500
import com.trader.admin.ui.theme.Emerald400
import com.trader.admin.ui.theme.Indigo400
import com.trader.admin.ui.theme.Indigo500
import com.trader.admin.ui.theme.Navy900
import com.trader.admin.ui.theme.Navy950
import com.trader.admin.ui.theme.Rose400
import com.trader.admin.ui.theme.Rose500
import com.trader.admin.ui.theme.Slate100
import com.trader.admin.ui.theme.Slate300
import com.trader.admin.ui.theme.Slate400
import com.trader.admin.ui.theme.Slate600
import com.trader.admin.ui.theme.Slate700
import com.trader.admin.ui.theme.Violet400
import com.trader.admin.ui.theme.Violet500
import com.trader.core.domain.model.SubscriptionPaymentMethod
import com.trader.core.domain.model.SubscriptionPlan
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

private sealed interface SheetContent {
    data class PlanForm(val existing: SubscriptionPlan? = null) : SheetContent
    data class MethodForm(val existing: SubscriptionPaymentMethod? = null) : SheetContent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSubscriptionScreen(
    onNavigateUp: () -> Unit,
    viewModel: AdminSubscriptionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sheetContent by remember { mutableStateOf<SheetContent?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "إعدادات الاشتراك",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowForward,
                                contentDescription = "رجوع",
                                tint = Slate300
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Navy950
                    )
                )
                AnimatedVisibility(
                    visible = uiState.isSaving,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200))
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Indigo500,
                        trackColor = Navy900
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Indigo500,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "إضافة")
            }
        },
        snackbarHost = { SnackbarHost(snackState) },
        containerColor = Navy950
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Indigo500)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SectionHeader(title = "خطط الاشتراك", count = uiState.plans.size)
                }

                if (uiState.plans.isEmpty()) {
                    item { EmptyStateCard(message = "لا توجد خطط اشتراك بعد") }
                } else {
                    items(uiState.plans, key = { it.id }) { plan ->
                        PlanItemCard(
                            plan = plan,
                            canDelete = uiState.plans.size > 1,
                            onEdit = { sheetContent = SheetContent.PlanForm(plan) },
                            onDelete = { viewModel.deletePlan(plan.id) }
                        )
                    }
                }

                item { Spacer(Modifier.height(12.dp)) }

                item {
                    SectionHeader(title = "طرق الدفع", count = uiState.methods.size)
                }

                if (uiState.methods.isEmpty()) {
                    item { EmptyStateCard(message = "لا توجد طرق دفع بعد") }
                } else {
                    items(uiState.methods, key = { it.id }) { method ->
                        MethodItemCard(
                            method = method,
                            canDelete = uiState.methods.size > 1,
                            onEdit = { sheetContent = SheetContent.MethodForm(method) },
                            onDelete = { viewModel.deletePaymentMethod(method.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTypeDialog(
            onAddPlan = { showAddDialog = false; sheetContent = SheetContent.PlanForm() },
            onAddMethod = { showAddDialog = false; sheetContent = SheetContent.MethodForm() },
            onDismiss = { showAddDialog = false }
        )
    }

    sheetContent?.let { content ->
        ModalBottomSheet(
            onDismissRequest = { sheetContent = null },
            sheetState = sheetState,
            containerColor = Navy900,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = Slate600)
            }
        ) {
            when (content) {
                is SheetContent.PlanForm -> PlanFormSheet(
                    existing = content.existing,
                    onSave = { plan -> viewModel.savePlan(plan); sheetContent = null },
                    onDismiss = { sheetContent = null }
                )

                is SheetContent.MethodForm -> MethodFormSheet(
                    existing = content.existing,
                    onSave = { method -> viewModel.savePaymentMethod(method); sheetContent = null },
                    onDismiss = { sheetContent = null }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(Indigo500.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = Indigo400,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Slate300
        )
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy900.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Slate600,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlanItemCard(
    plan: SubscriptionPlan,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy900),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Indigo500.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Layers,
                    contentDescription = null,
                    tint = Indigo400,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    plan.savingBadge?.let { badge ->
                        Box(
                            modifier = Modifier
                                .background(
                                    Emerald400.copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald400
                            )
                        }
                    }
                    if (plan.isRecommended) {
                        Box(
                            modifier = Modifier
                                .background(Violet500.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "موصى به",
                                style = MaterialTheme.typography.labelSmall,
                                color = Violet400
                            )
                        }
                    }
                    Text(
                        text = plan.arabicLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate100
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        plan.periodLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Slate400
                    )
                    Text("·", color = Slate600, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = plan.priceLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Indigo400
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "تعديل",
                        tint = Indigo400,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = canDelete
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "حذف",
                        tint = if (canDelete) Rose400 else Slate600,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            itemName = plan.arabicLabel,
            onConfirm = { showDeleteConfirm = false; onDelete() },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun MethodItemCard(
    method: SubscriptionPaymentMethod,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy900),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Cyan500.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountBalance,
                    contentDescription = null,
                    tint = Cyan500,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = method.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate100
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = method.accountNumber,
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate400
                )
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "تعديل",
                        tint = Indigo400,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = canDelete
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "حذف",
                        tint = if (canDelete) Rose400 else Slate600,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            itemName = method.name,
            onConfirm = { showDeleteConfirm = false; onDelete() },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy900,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteForever,
                contentDescription = null,
                tint = Rose400,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "تأكيد الحذف",
                style = MaterialTheme.typography.titleMedium,
                color = Slate100,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "هل تريد حذف \"$itemName\"؟ لا يمكن التراجع عن هذا الإجراء.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حذف", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Slate400)
            }
        }
    )
}

@Composable
private fun AddTypeDialog(
    onAddPlan: () -> Unit,
    onAddMethod: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy900,
        title = {
            Text(
                text = "إضافة عنصر جديد",
                style = MaterialTheme.typography.titleMedium,
                color = Slate100,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AddTypeOption(
                    icon = Icons.Rounded.Layers,
                    label = "خطة اشتراك جديدة",
                    tint = Indigo400,
                    onClick = onAddPlan
                )
                HorizontalDivider(color = Slate700, thickness = 0.5.dp)
                AddTypeOption(
                    icon = Icons.Rounded.Payment,
                    label = "طريقة دفع جديدة",
                    tint = Cyan500,
                    onClick = onAddMethod
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Slate400)
            }
        }
    )
}

@Composable
private fun AddTypeOption(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Slate100)
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun PlanFormSheet(
    existing: SubscriptionPlan?,
    onSave: (SubscriptionPlan) -> Unit,
    onDismiss: () -> Unit
) {
    val id = remember { existing?.id ?: UUID.randomUUID().toString().take(12) }
    var arabicLabel by remember { mutableStateOf(existing?.arabicLabel ?: "") }
    var priceLabel by remember { mutableStateOf(existing?.priceLabel ?: "") }
    var periodLabel by remember { mutableStateOf(existing?.periodLabel ?: "") }
    var savingBadge by remember { mutableStateOf(existing?.savingBadge ?: "") }
    var isRecommended by remember { mutableStateOf(existing?.isRecommended ?: false) }

    val isFormValid =
        arabicLabel.isNotBlank() && priceLabel.isNotBlank() && periodLabel.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (existing != null) "تعديل خطة الاشتراك" else "إضافة خطة اشتراك",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Slate100,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        AdminOutlinedField(
            value = arabicLabel,
            onValueChange = { arabicLabel = it },
            label = "اسم الخطة *"
        )
        AdminOutlinedField(
            value = priceLabel,
            onValueChange = { priceLabel = it },
            label = "السعر (مثال: 20₪) *"
        )
        AdminOutlinedField(
            value = periodLabel,
            onValueChange = { periodLabel = it },
            label = "الفترة (مثال: شهرياً) *"
        )
        AdminOutlinedField(
            value = savingBadge,
            onValueChange = { savingBadge = it },
            label = "شارة التوفير (اختياري)"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Navy950.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = isRecommended,
                onCheckedChange = { isRecommended = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Violet500,
                    uncheckedThumbColor = Slate400,
                    uncheckedTrackColor = Slate700
                )
            )
            Text(
                text = "خطة موصى بها",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isRecommended) Violet400 else Slate400
            )
        }

        Button(
            onClick = {
                onSave(
                    SubscriptionPlan(
                        id = id,
                        arabicLabel = arabicLabel.trim(),
                        priceLabel = priceLabel.trim(),
                        periodLabel = periodLabel.trim(),
                        savingBadge = savingBadge.trim().takeIf { it.isNotBlank() },
                        isRecommended = isRecommended
                    )
                )
            },
            enabled = isFormValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Indigo500,
                disabledContainerColor = Slate700
            )
        ) {
            Text(
                text = "حفظ الخطة",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isFormValid) Color.White else Slate600
            )
        }
    }
}

@Composable
private fun MethodFormSheet(
    existing: SubscriptionPaymentMethod?,
    onSave: (SubscriptionPaymentMethod) -> Unit,
    onDismiss: () -> Unit
) {
    val id = remember { existing?.id ?: UUID.randomUUID().toString().take(12) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var accountNumber by remember { mutableStateOf(existing?.accountNumber ?: "") }

    val isFormValid = name.isNotBlank() && accountNumber.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (existing != null) "تعديل طريقة الدفع" else "إضافة طريقة دفع",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Slate100,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        AdminOutlinedField(
            value = name,
            onValueChange = { name = it },
            label = "اسم الخدمة *"
        )
        AdminOutlinedField(
            value = accountNumber,
            onValueChange = { accountNumber = it },
            label = "رقم الحساب *"
        )

        Button(
            onClick = {
                onSave(
                    SubscriptionPaymentMethod(
                        id = id,
                        name = name.trim(),
                        accountNumber = accountNumber.trim()
                    )
                )
            },
            enabled = isFormValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Indigo500,
                disabledContainerColor = Slate700
            )
        ) {
            Text(
                text = "حفظ طريقة الدفع",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isFormValid) Color.White else Slate600
            )
        }
    }
}

@Composable
private fun AdminOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Slate100,
            unfocusedTextColor = Slate100,
            focusedBorderColor = Indigo500,
            unfocusedBorderColor = Slate700,
            focusedLabelColor = Indigo400,
            unfocusedLabelColor = Slate400,
            cursorColor = Indigo400,
            focusedContainerColor = Navy950.copy(alpha = 0.5f),
            unfocusedContainerColor = Navy950.copy(alpha = 0.5f)
        )
    )
}
