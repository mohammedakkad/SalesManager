package com.trader.salesmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.trader.salesmanager.service.NotificationService
import com.trader.salesmanager.ui.navigation.AppNavigation
import com.trader.salesmanager.ui.theme.SalesManagerTheme
import com.trader.salesmanager.update.AppUpdateViewModel
import com.trader.salesmanager.update.BackgroundUpdateWorker
import com.trader.salesmanager.update.UpdateUiState
import org.koin.androidx.viewmodel.ext.android.viewModel

import androidx.lifecycle.lifecycleScope // حل مشكلة Unresolved reference 'lifecycleScope'
import kotlinx.coroutines.launch
import com.trader.core.data.migration.FirestoreMigrationHelper // تأكد أن هذا المسار يطابق ملف الهيلبر
class MainActivity : ComponentActivity() {

    private val updateViewModel: AppUpdateViewModel by viewModel()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        /* granted or denied */
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationService.createChannels(this)
        requestNotificationPermission()
        lifecycleScope.launch {
            FirestoreMigrationHelper().migrateAll()
        }
        val currentVersionCode = packageManager
        .getPackageInfo(packageName, 0)
        .let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                it.longVersionCode.toInt()
            else @Suppress("DEPRECATION") it.versionCode
        }

        // التحقق من التحديث — يبدأ التحميل تلقائياً في الخلفية إذا وُجد
        updateViewModel.checkForUpdate(currentVersionCode)

        setContent {
            SalesManagerTheme(context = this@MainActivity) {
                val updateState by updateViewModel.state.collectAsState()

                // عند وجود تحديث — نبدأ التحميل في الخلفية تلقائياً (بدون Dialog إجباري)
                LaunchedEffect(updateState) {
                    if (updateState is UpdateUiState.UpdateAvailable) {
                        val info = (updateState as UpdateUiState.UpdateAvailable).info
                        BackgroundUpdateWorker.schedule(
                            this@MainActivity,
                            info.downloadUrl,
                            info.versionName
                        )
                    }
                }

                // التطبيق الرئيسي — يعمل بشكل طبيعي دون Dialog إجباري
                AppNavigation()
            }
        }
    }

    // يُستدعى عند الضغط على إشعار "جاهز للتثبيت"
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("install_update", false)) {
            updateViewModel.install(this)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}