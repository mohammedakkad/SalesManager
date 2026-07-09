package com.trader.salesmanager

import android.Manifest
import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.trader.salesmanager.service.NotificationService
import com.trader.salesmanager.ui.navigation.AppNavigation
import com.trader.salesmanager.ui.theme.SalesManagerTheme
import com.trader.salesmanager.update.AppUpdateViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val updateViewModel: AppUpdateViewModel by viewModel()

    private var pendingNavigation by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        NotificationService.createChannels(this)
        requestNotificationPermission()
        handleUpdateIntent(intent)

        setContent {
            SalesManagerTheme(context = this@MainActivity) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        pendingNavigation = pendingNavigation,
                        onNavigationHandled = { pendingNavigation = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
        pendingNavigation = intent.getStringExtra(EXTRA_NAVIGATE_TO)
    }

    private fun handleUpdateIntent(intent: Intent?) {
        if (intent == null) return

        pendingNavigation = intent.getStringExtra(EXTRA_NAVIGATE_TO)

        if (intent.getBooleanExtra(EXTRA_INSTALL_UPDATE, false)) {
            updateViewModel.markReadyToInstallFromBackground()
            updateViewModel.installUpdate()
        } else if (pendingNavigation == NAV_SETTINGS) {
            updateViewModel.checkForUpdate()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val EXTRA_INSTALL_UPDATE = "install_update"
        const val NAV_SETTINGS = "settings"
        const val NAV_REMIND_ALL = "remind_all"
    }
}
