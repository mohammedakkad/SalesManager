package com.trader.salesmanager.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.core.data.local.appDataStore
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.isDarkTheme
import com.trader.salesmanager.ui.theme.toggleTheme
import com.trader.salesmanager.ui.theme.Violet400
import com.trader.salesmanager.update.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

val STORE_NAME_KEY = stringPreferencesKey("store_name")
val MERCHANT_CODE_KEY = stringPreferencesKey("merchant_code")

@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToPaymentMethods: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToSessions: () -> Unit = {},
    onNavigateToEmployeeManagement: () -> Unit = {},
    onLogout: () -> Unit = {},
    updateViewModel: AppUpdateViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // اسم المحل من DataStore
    val storeName by context.appDataStore.data
        .map { it[STORE_NAME_KEY] ?: "" }
        .collectAsState(initial = "")
    val merchantCode by context.appDataStore.data
        .map { it[MERCHANT_CODE_KEY] ?: "" }
        .collectAsState(initial = "")

    var showStoreNameDialog by remember { mutableStateOf(false) }

    // إصدار التطبيق الحالي
    val currentVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }

    // حالة التحديث
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    val isChecking = updateState is UpdateUiState.Checking
    val latestVersion = when (val s = updateState) {
        is UpdateUiState.UpdateAvailable -> s.info.versionName
        is UpdateUiState.BackgroundDownloading -> "يتم التحميل..."
        else -> null
    }

    // Dialog تغيير اسم المحل
    if (showStoreNameDialog) {
        StoreNameDialog(
            currentName = storeName,
            onSave = { name ->
                scope.launch {
                    context.appDataStore.edit { it[STORE_NAME_KEY] = name }
                }
                showStoreNameDialog = false
            },
            onDismiss = { showStoreNameDialog = false }
        )
    }

    // Feedback for the Update/Refresh action so the user always knows the result
    // of tapping the button (no more silent "nothing happened" experience).
    LaunchedEffect(updateState) {
        when (val s = updateState) {
            is UpdateUiState.UpdateAvailable -> {
                BackgroundUpdateWorker.schedule(context, s.info.downloadUrl, s.info.versionName)
                Toast.makeText(
                    context,
                    "يتوفر تحديث: v${s.info.versionName}",
                    Toast.LENGTH_LONG
                ).show()
            }
            is UpdateUiState.UpToDate -> {
                Toast.makeText(
                    context,
                    "أنت تستخدم أحدث إصدار",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is UpdateUiState.DownloadError -> {
                Toast.makeText(
                    context,
                    "تعذر التحقق من التحديث: ${s.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> Unit
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Slate800, Slate600)))
                    .padding(top = 48.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "الإعدادات",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
            }

            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── اسم المحل ──────────────────────────────────────
                SettingItem(
                    icon = Icons.Rounded.Store,
                    title = "اسم المحل",
                    subtitle = storeName.ifEmpty { "غير محدد — اضغط لتحديده" },
                    color = Emerald500,
                    onClick = { showStoreNameDialog = true }
                )
                MerchantCodeCard(
                    merchantCode = merchantCode,
                    onCopy = {
                        if (merchantCode.isBlank()) return@MerchantCodeCard
                        clipboardManager.setText(AnnotatedString(merchantCode))
                        Toast.makeText(context, "تم نسخ الكود", Toast.LENGTH_SHORT).show()
                    }
                )

                // ── الوضع الليلي ───────────────────────────────────
                DarkModeSettingItem()

                // ── طرق الدفع ──────────────────────────────────────
                SettingItem(
                    icon = Icons.Rounded.Payment,
                    title = "طرق الدفع",
                    subtitle = "إدارة طرق دفع التاجر",
                    color = Cyan500,
                    onClick = onNavigateToPaymentMethods
                )

                // ── الدعم الفني ────────────────────────────────────
                SettingItem(
                    icon = Icons.Rounded.SupportAgent,
                    title = "الدعم الفني",
                    subtitle = "تواصل مع الإدارة مباشرة",
                    color = Violet500,
                    onClick = onNavigateToChat
                )
                SettingItem(
                    icon = Icons.Rounded.Devices,
                    title = "الأجهزة النشطة",
                    subtitle = "إدارة جلسات تسجيل الدخول للأجهزة",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToSessions
                )

                // ── إدارة الموظفين (Phase 4.3 — RBAC) ──────────────
                SettingItem(
                    icon = Icons.Rounded.AdminPanelSettings,
                    title = "إدارة الموظفين",
                    subtitle = "إضافة وحذف الموظفين وتعيين الصلاحيات",
                    color = Violet500,
                    onClick = onNavigateToEmployeeManagement
                )

                // ── تسجيل الخروج ───────────────────────────────────
                SettingItem(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    title = "تسجيل الخروج",
                    subtitle = "إنهاء الجلسة الحالية والعودة لشاشة القفل",
                    color = DebtRed,
                    onClick = onLogout
                )

                // ── التحديثات ──────────────────────────────────────
                UpdateSettingItem(
                    currentVersion = currentVersion,
                    latestVersion = latestVersion,
                    isChecking = isChecking,
                    updateState = updateState,
                    onCheck = {
                        val versionCode = try {
                            context.packageManager.getPackageInfo(
                                context.packageName,
                                0
                            ).longVersionCode.toInt()
                        } catch (_: Exception) {
                            0
                        }
                        updateViewModel.checkForUpdate(versionCode)
                    },
                    onInstall = { updateViewModel.install(context) }
                )
            }
        }
    }
}

@Composable
private fun MerchantCodeCard(
    merchantCode: String,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "كود التاجر",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = merchantCode.ifEmpty { "سيظهر الكود بعد تفعيل الحساب" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCopy, enabled = merchantCode.isNotBlank()) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "نسخ كود التاجر"
                )
            }
        }
    }
}

@Composable
private fun StoreNameDialog(
    currentName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Store, null, tint = Emerald500) },
        title = { Text("اسم المحل", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("أدخل اسم محلك") },
                placeholder = { Text("مثال: سوبر ماركت النور") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(input.trim()) },
                enabled = input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
            ) { Text("حفظ") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("إلغاء") } },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun DarkModeSettingItem() {
    val isDark = isDarkTheme
    val toggle = toggleTheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDark) Color(0xFF312E81).copy(0.3f)
                        else Color(0xFFFEF9C3)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    null,
                    tint = if (isDark) Violet400 else UnpaidAmber
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "المظهر",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (isDark) "الوضع الليلي مفعّل" else "الوضع النهاري مفعّل",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isDark,
                onCheckedChange = { toggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Violet500,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Slate400
                )
            )
        }
    }
}

@Composable
private fun UpdateSettingItem(
    currentVersion: String,
    latestVersion: String?,
    isChecking: Boolean,
    updateState: UpdateUiState,
    onCheck: () -> Unit,
    onInstall: () -> Unit
) {
    val hasUpdate = updateState is UpdateUiState.UpdateAvailable
    val isReady = updateState is UpdateUiState.ReadyToInstall
    val isUpToDate = updateState is UpdateUiState.UpToDate

    // Disable the row while checking so a fast double-tap doesn't fire the action twice.
    val clickHandler: () -> Unit = when {
        isChecking -> { -> Unit }
        isReady -> onInstall
        else -> onCheck
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isChecking, onClick = clickHandler),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                hasUpdate -> UnpaidAmber.copy(0.06f)
                isUpToDate -> Emerald500.copy(0.06f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            hasUpdate -> UnpaidAmber.copy(0.12f)
                            isUpToDate -> Emerald500.copy(0.12f)
                            else -> Slate600.copy(0.12f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isChecking)
                    CircularProgressIndicator(
                        Modifier.size(22.dp),
                        color = Emerald500,
                        strokeWidth = 2.dp
                    )
                else
                    Icon(
                        when {
                            isReady -> Icons.Rounded.InstallMobile
                            hasUpdate -> Icons.Rounded.SystemUpdate
                            else -> Icons.Rounded.Refresh
                        },
                        null,
                        tint = when {
                            hasUpdate || isReady -> UnpaidAmber
                            isUpToDate -> Emerald500
                            else -> Slate600
                        }
                    )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "الإصدار والتحديث",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
                )
                Text(
                    when {
                        isReady -> "جاهز للتثبيت — اضغط للتثبيت"
                        hasUpdate -> "تحديث متاح: v${latestVersion}  (حالي: v$currentVersion)"
                        isChecking -> "جاري التحقق من التحديثات..."
                        isUpToDate -> "أنت تستخدم أحدث إصدار (v$currentVersion)"
                        else -> "v$currentVersion  •  اضغط للتحقق من وجود تحديث"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        hasUpdate -> UnpaidAmber
                        isUpToDate -> Emerald500
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Icon(
                Icons.Rounded.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector, title: String, subtitle: String,
    color: Color, onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = color) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
