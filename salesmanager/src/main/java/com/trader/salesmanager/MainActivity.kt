package com.trader.salesmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.trader.salesmanager.service.NotificationService
import com.trader.salesmanager.ui.navigation.AppNavigation
import com.trader.salesmanager.ui.theme.SalesManagerTheme
import com.trader.salesmanager.update.AppUpdateDialog
import com.trader.salesmanager.update.AppUpdateViewModel
import com.trader.salesmanager.update.UpdateUiState

class MainActivity : ComponentActivity() {

    private val updateViewModel: AppUpdateViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationService.createChannels(this)
        requestNotificationPermission()

        // Check for update on startup
        val currentVersionCode = packageManager
            .getPackageInfo(packageName, 0)
            .let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    it.longVersionCode.toInt()
                else @Suppress("DEPRECATION") it.versionCode
            }
        updateViewModel.checkForUpdate(currentVersionCode)

        setContent {
            SalesManagerTheme {
                val updateState by updateViewModel.state.collectAsState()

                // Main app
                AppNavigation()

                // Update dialog — overlays everything, non-dismissible
                AppUpdateDialog(
                    state            = updateState,
                    onStartDownload  = { ctx -> updateViewModel.startDownload(ctx) },
                    onInstall        = { ctx -> updateViewModel.install(ctx) },
                    onRetry          = { ctx -> updateViewModel.retryDownload(ctx) },
                    onOpenPermission = { ctx ->
                        updateViewModel.openInstallPermission(ctx)
                        // Re-check permission after returning
                        updateViewModel.startDownload(ctx)
                    }
                )
            }
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
