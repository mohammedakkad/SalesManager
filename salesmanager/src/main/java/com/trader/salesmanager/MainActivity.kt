package com.trader.salesmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.salesmanager.service.NotificationService
import com.trader.salesmanager.ui.navigation.AppNavigation
import com.trader.salesmanager.ui.theme.SalesManagerTheme
import com.trader.salesmanager.update.AppUpdateViewModel
import com.trader.salesmanager.update.BackgroundUpdateWorker
import com.trader.salesmanager.update.UpdateUiState
import org.koin.androidx.viewmodel.ext.android.viewModel

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        NotificationService.createChannels(this)
        requestNotificationPermission()

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

                // ✅ Fix 2: Surface جذرية تضمن خلفية صحيحة في كل الأوقات
                // تحمي من الشفافية التي تُظهر الـ Window background الخام
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val updateState by updateViewModel.state.collectAsStateWithLifecycle()

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