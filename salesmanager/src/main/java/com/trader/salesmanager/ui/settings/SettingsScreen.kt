package com.trader.salesmanager.ui.settings

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.core.data.local.appDataStore
import com.trader.core.domain.repository.LowStockAlertSettings
import com.trader.core.domain.repository.LowStockSettingsRepository
import com.trader.salesmanager.R
import com.trader.salesmanager.ui.settings.backup.LAST_BACKUP_AT_KEY
import com.trader.salesmanager.ui.settings.backup.formatLastBackupRelative
import com.trader.salesmanager.ui.theme.Cyan500
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.Slate400
import com.trader.salesmanager.ui.theme.Slate600
import com.trader.salesmanager.ui.theme.Slate800
import com.trader.salesmanager.ui.theme.UnpaidAmber
import com.trader.salesmanager.ui.theme.Violet400
import com.trader.salesmanager.ui.theme.Violet500
import com.trader.salesmanager.ui.theme.isDarkTheme
import com.trader.salesmanager.ui.theme.toggleTheme
import com.trader.salesmanager.update.AppUpdateDownloader
import com.trader.salesmanager.update.AppUpdateUiState
import com.trader.salesmanager.update.AppUpdateViewModel
import com.trader.salesmanager.worker.LowStockCheckWorker
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

val STORE_NAME_KEY = stringPreferencesKey("store_name")
val MERCHANT_CODE_KEY = stringPreferencesKey("merchant_code")

@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToPaymentMethods: () -> Unit,
    onNavigateToCashBoxes: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    updateViewModel: AppUpdateViewModel = koinViewModel(),
    lowStockSettingsRepository: LowStockSettingsRepository = koinInject()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val storeName by context.appDataStore.data
        .map { it[STORE_NAME_KEY] ?: "" }
        .collectAsState(initial = "")
    val merchantCode by context.appDataStore.data
        .map { it[MERCHANT_CODE_KEY] ?: "" }
        .collectAsState(initial = "")
    val lastBackupAt by context.appDataStore.data
        .map { it[LAST_BACKUP_AT_KEY] }
        .collectAsState(initial = null)
    val backupSubtitle = stringResource(
        R.string.settings_backup_last,
        formatLastBackupRelative(lastBackupAt)
    )
    val lowStockSettings by lowStockSettingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = LowStockAlertSettings())

    var showStoreNameDialog by remember { mutableStateOf(false) }

    val currentVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }

    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val canInstall = AppUpdateDownloader.canInstallUnknownApps(context)

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
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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

                DarkModeSettingItem()

                LowStockAlertsSettingItem(
                    settings = lowStockSettings,
                    onEnabledChange = { enabled ->
                        scope.launch {
                            lowStockSettingsRepository.setEnabled(enabled)
                            if (enabled) {
                                LowStockCheckWorker.schedule(
                                    context.applicationContext,
                                    lowStockSettings.preferredHour,
                                    replaceExisting = true
                                )
                            } else {
                                LowStockCheckWorker.cancel(context.applicationContext)
                            }
                        }
                    },
                    onHourChange = { hour ->
                        scope.launch {
                            lowStockSettingsRepository.setPreferredHour(hour)
                            if (lowStockSettings.enabled) {
                                LowStockCheckWorker.schedule(
                                    context.applicationContext,
                                    hour,
                                    replaceExisting = true
                                )
                            }
                        }
                    }
                )

                SettingItem(
                    icon = Icons.Rounded.Payment,
                    title = "طرق الدفع",
                    subtitle = "إدارة طرق دفع التاجر",
                    color = Cyan500,
                    onClick = onNavigateToPaymentMethods
                )

                SettingItem(
                    icon = Icons.Rounded.Savings,
                    title = "الصناديق",
                    subtitle = "رصيد كل طريقة دفع لحظياً",
                    color = Emerald500,
                    onClick = onNavigateToCashBoxes
                )

                SettingItem(
                    icon = Icons.Rounded.Backup,
                    title = stringResource(R.string.settings_backup_title),
                    subtitle = backupSubtitle,
                    color = Cyan500,
                    onClick = onNavigateToBackup
                )

                SettingItem(
                    icon = Icons.Rounded.SupportAgent,
                    title = "الدعم الفني",
                    subtitle = "تواصل مع الإدارة مباشرة",
                    color = Violet500,
                    onClick = onNavigateToChat
                )

                UpdateSettingItem(
                    currentVersion = currentVersion,
                    updateState = updateState,
                    canInstallUnknownApps = canInstall,
                    onCheck = { updateViewModel.checkForUpdate() },
                    onDownload = { updateViewModel.startDownload() },
                    onInstall = { updateViewModel.installUpdate() },
                    onOpenInstallSettings = { updateViewModel.openInstallSettings() }
                )
            }
        }
    }
}

@Composable
private fun LowStockAlertsSettingItem(
    settings: LowStockAlertSettings,
    onEnabledChange: (Boolean) -> Unit,
    onHourChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(UnpaidAmber.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = UnpaidAmber
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "تنبيهات نقص المخزون",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (settings.enabled) "مفعّلة يومياً مع منع التكرار" else "متوقفة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = UnpaidAmber,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Slate400
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "وقت التنبيه",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(
                    LowStockAlertSettings.MORNING_HOUR to "صباحاً",
                    LowStockAlertSettings.EVENING_HOUR to "مساءً"
                ).forEach { (hour, label) ->
                    FilterChip(
                        selected = settings.preferredHour == hour,
                        onClick = { onHourChange(hour) },
                        enabled = settings.enabled,
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = UnpaidAmber.copy(0.15f),
                            selectedLabelColor = UnpaidAmber
                        )
                    )
                }
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
    updateState: AppUpdateUiState,
    canInstallUnknownApps: Boolean,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenInstallSettings: () -> Unit
) {
    val isChecking = updateState.isChecking
    val isDownloading = updateState.downloadProgress != null
    val isReadyToInstall = updateState.isReadyToInstall
    val hasUpdate = updateState.updateAvailable
    val versionName = updateState.updateInfo?.versionName
    val downloadProgress = updateState.downloadProgress
    val needsInstallPermission = (hasUpdate || isReadyToInstall) && !canInstallUnknownApps

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isChecking && !isDownloading) {
                when {
                    isReadyToInstall -> onInstall()
                    needsInstallPermission -> onOpenInstallSettings()
                    else -> onCheck()
                }
            },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                hasUpdate || isReadyToInstall -> UnpaidAmber.copy(0.06f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            (if (hasUpdate || isReadyToInstall) UnpaidAmber else Slate600).copy(0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isChecking -> CircularProgressIndicator(
                            Modifier.size(22.dp),
                            color = Slate600,
                            strokeWidth = 2.dp
                        )
                        isReadyToInstall -> Icon(
                            Icons.Rounded.InstallMobile,
                            null,
                            tint = UnpaidAmber
                        )
                        hasUpdate -> Icon(
                            Icons.Rounded.SystemUpdate,
                            null,
                            tint = UnpaidAmber
                        )
                        else -> Icon(
                            Icons.Rounded.CheckCircle,
                            null,
                            tint = Slate600
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "تحديث التطبيق",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        when {
                            isChecking -> "جاري التحقق..."
                            needsInstallPermission -> "يلزم منح إذن التثبيت"
                            isReadyToInstall -> "اكتمل التحميل — اضغط للتثبيت"
                            isDownloading -> "جاري التحميل... ${downloadProgress ?: 0}%"
                            hasUpdate && versionName != null ->
                                "يوجد تحديث — الإصدار $versionName"
                            updateState.error != null -> updateState.error
                            else -> "الإصدار الحالي v$currentVersion — اضغط للتحقق"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            hasUpdate || isReadyToInstall -> UnpaidAmber
                            updateState.error != null -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                if (!isChecking && !isDownloading) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                isDownloading && downloadProgress != null -> {
                    Spacer(Modifier.size(12.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "$downloadProgress%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                needsInstallPermission -> {
                    Spacer(Modifier.size(12.dp))
                    OutlinedButton(
                        onClick = onOpenInstallSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("فتح إعدادات التثبيت")
                    }
                }
                hasUpdate && !isReadyToInstall && !isDownloading && canInstallUnknownApps -> {
                    Spacer(Modifier.size(12.dp))
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = UnpaidAmber)
                    ) {
                        Text("تحميل التحديث")
                    }
                }
                isReadyToInstall && canInstallUnknownApps -> {
                    Spacer(Modifier.size(12.dp))
                    Button(
                        onClick = onInstall,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = UnpaidAmber)
                    ) {
                        Text("تثبيت التحديث")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
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
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
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
