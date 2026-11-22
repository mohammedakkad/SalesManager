package com.trader.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.trader.admin.ui.navigation.AdminNavigation
import com.trader.core.ui.WideScreenContainer
import com.trader.admin.ui.theme.AdminTheme

class AdminMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminAlertsWorker.startPeriodicChecks(this)
        enableEdgeToEdge()
        setContent {
            AdminTheme {
                // Background only (no Surface): content colours stay exactly as before.
                WideScreenContainer(Modifier.background(MaterialTheme.colorScheme.background)) {
                    AdminNavigation()
                }
            }
        }
    }
}
