package com.trader.salesmanager.ui.employees

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.core.domain.model.Employee
import com.trader.core.domain.model.EmployeeRole
import com.trader.salesmanager.ui.theme.Cyan500
import com.trader.salesmanager.ui.theme.DebtRed
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.Violet500
import com.trader.salesmanager.ui.theme.appColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

// ════════════════════════════════════════════════════════════════════
//  EMPLOYEE MANAGEMENT SCREEN — "2026 Premium" Look
//
//  • Glassmorphic employee cards with staggered slide-in entrance.
//  • Animated FAB (rotates + morphs) opens a smooth ModalBottomSheet
//    to add a new employee (Name, PIN, Role).
//  • Animated role badges (Admin = violet, Cashier = cyan).
// ════════════════════════════════════════════════════════════════════

private const val LIST_STAGGER_MS = 55L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeManagementScreen(
    onNavigateUp: () -> Unit,
    viewModel: EmployeeManagementViewModel = koinViewModel()
) {
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()

    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Employee?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EmployeeManagementEvent.EmployeeAdded -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showSheet = false
                    snackbarHostState.showSnackbar("تمت إضافة الموظف بنجاح")
                }
                EmployeeManagementEvent.EmployeeDeleted -> {
                    snackbarHostState.showSnackbar("تم حذف الموظف")
                }
                is EmployeeManagementEvent.Failure -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "إدارة الموظفين",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            AnimatedAddFab(
                expanded = showSheet,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showSheet = true
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (employees.isEmpty()) {
                EmptyState()
            } else {
                EmployeeList(
                    employees = employees,
                    onDelete = { pendingDelete = it }
                )
            }
        }
    }

    // ── Add-employee bottom sheet ────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                viewModel.resetForm()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            AddEmployeeSheetContent(
                form = form,
                onNameChange = viewModel::onNameChange,
                onPinChange = viewModel::onPinChange,
                onRoleChange = viewModel::onRoleChange,
                onSubmit = viewModel::submit,
                onCancel = {
                    scope.launch { sheetState.hide() }
                    showSheet = false
                    viewModel.resetForm()
                }
            )
        }
    }

    // ── Delete confirmation ──────────────────────────────────────
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("حذف الموظف") },
            text = {
                Text("هل أنت متأكد أنك تريد حذف \"${target.name}\"؟ لا يمكن التراجع.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(target)
                        pendingDelete = null
                    }
                ) { Text("حذف", color = DebtRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("إلغاء") }
            }
        )
    }
}

// ════════════════════════════════════════════════════════════════════
//  EMPTY STATE
// ════════════════════════════════════════════════════════════════════
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Emerald500.copy(alpha = 0.18f), Cyan500.copy(alpha = 0.18f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = Emerald500,
                modifier = Modifier.size(46.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "لا يوجد موظفون بعد",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "اضغط على زر الإضافة لإنشاء أول موظف",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 13.sp
        )
    }
}

// ════════════════════════════════════════════════════════════════════
//  EMPLOYEE LIST — staggered entrance
// ════════════════════════════════════════════════════════════════════
@Composable
private fun EmployeeList(
    employees: List<Employee>,
    onDelete: (Employee) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = employees,
            key = { _, item -> item.id }
        ) { index, employee ->
            EmployeeCard(
                employee = employee,
                staggerIndex = index,
                onDelete = { onDelete(employee) }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  EMPLOYEE CARD — glassmorphism + staggered slide-in
// ════════════════════════════════════════════════════════════════════
@Composable
private fun EmployeeCard(
    employee: Employee,
    staggerIndex: Int,
    onDelete: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(employee.id) {
        delay(staggerIndex * LIST_STAGGER_MS)
        visible = true
    }

    val isDark = appColors.isDark
    val glassBg = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.02f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color.White.copy(alpha = 0.80f)
            )
        )
    }
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) { it / 3 } + fadeIn(animationSpec = tween(380, easing = EaseOutCubic)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(glassBg)
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmployeeAvatar(name = employee.name, isAdmin = employee.role == EmployeeRole.ADMIN)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(6.dp))
                RoleBadge(role = employee.role)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "حذف",
                    tint = DebtRed
                )
            }
        }
    }
}

@Composable
private fun EmployeeAvatar(name: String, isAdmin: Boolean) {
    val initial = remember(name) {
        name.trim().firstOrNull()?.uppercase()?.toString() ?: "?"
    }
    val colors = if (isAdmin) {
        listOf(Violet500, Cyan500)
    } else {
        listOf(Emerald500, Cyan500)
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun RoleBadge(role: EmployeeRole) {
    val (label, color, icon) = when (role) {
        EmployeeRole.ADMIN -> Triple(
            "مدير",
            Violet500,
            Icons.Rounded.AdminPanelSettings
        )
        EmployeeRole.CASHIER -> Triple(
            "كاشير",
            Cyan500,
            Icons.Rounded.PointOfSale
        )
    }

    val pulse by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(400, easing = EaseOutBack),
        label = "badge-pulse"
    )

    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.45f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ════════════════════════════════════════════════════════════════════
//  ANIMATED ADD FAB — rotates + morphs when sheet opens
// ════════════════════════════════════════════════════════════════════
@Composable
private fun AnimatedAddFab(expanded: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 135f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab-rotation"
    )
    val scale by animateFloatAsState(
        targetValue = if (expanded) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fab-scale"
    )

    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        containerColor = Emerald500,
        contentColor = Color.White,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        },
        text = {
            Text("إضافة موظف", fontWeight = FontWeight.SemiBold)
        }
    )
}

// ════════════════════════════════════════════════════════════════════
//  ADD EMPLOYEE — bottom sheet content
// ════════════════════════════════════════════════════════════════════
@Composable
private fun AddEmployeeSheetContent(
    form: AddEmployeeForm,
    onNameChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onRoleChange: (EmployeeRole) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    val nameError by remember(form) {
        derivedStateOf { form.error == AddEmployeeError.NAME_BLANK }
    }
    val pinError by remember(form) {
        derivedStateOf {
            form.error == AddEmployeeError.PIN_INVALID ||
                form.error == AddEmployeeError.PIN_TAKEN
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "إضافة موظف جديد",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            label = { Text("اسم الموظف") },
            singleLine = true,
            isError = nameError,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
            value = form.pin,
            onValueChange = onPinChange,
            label = { Text("رمز PIN (4 أرقام)") },
            singleLine = true,
            isError = pinError,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        AnimatedVisibility(
            visible = form.error != AddEmployeeError.NONE,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = when (form.error) {
                    AddEmployeeError.NAME_BLANK -> "الاسم قصير جداً"
                    AddEmployeeError.PIN_INVALID -> "رمز PIN يجب أن يكون 4 أرقام"
                    AddEmployeeError.PIN_TAKEN -> "هذا الرمز مستخدم بالفعل"
                    AddEmployeeError.NONE -> ""
                },
                color = DebtRed,
                fontSize = 12.sp
            )
        }

        Text(
            text = "الدور",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoleChip(
                label = "مدير",
                icon = Icons.Rounded.AdminPanelSettings,
                color = Violet500,
                selected = form.role == EmployeeRole.ADMIN,
                onClick = { onRoleChange(EmployeeRole.ADMIN) }
            )
            RoleChip(
                label = "كاشير",
                icon = Icons.Rounded.PointOfSale,
                color = Cyan500,
                selected = form.role == EmployeeRole.CASHIER,
                onClick = { onRoleChange(EmployeeRole.CASHIER) }
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SubmitButton(
                enabled = form.canSubmit,
                loading = form.submitting,
                onClick = onSubmit,
                modifier = Modifier.weight(2f)
            )
        }
    }
}

@Composable
private fun RoleChip(
    label: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chip-scale"
    )

    FilterChip(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        selected = selected,
        onClick = onClick,
        leadingIcon = {
            Icon(icon, null, tint = if (selected) Color.White else color)
        },
        label = {
            Text(
                label,
                color = if (selected) Color.White else color,
                fontWeight = FontWeight.SemiBold
            )
        },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = color.copy(alpha = 0.14f),
            selectedContainerColor = color
        )
    )
}

@Composable
private fun SubmitButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        animationSpec = tween(200),
        label = "submit-alpha"
    )

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(listOf(Emerald500, Cyan500))
            )
            .graphicsLayer { this.alpha = alpha }
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(
                text = "حفظ",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
