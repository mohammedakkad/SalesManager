package com.trader.salesmanager.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.salesmanager.ui.theme.*

@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToPaymentMethods: () -> Unit,
    onNavigateToChat: () -> Unit = {}
) {
    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
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
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }

            Column(
                modifier  = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingItem(
                    icon     = Icons.Rounded.Payment,
                    title    = "طرق الدفع",
                    subtitle = "إدارة طرق دفع التاجر",
                    color    = Cyan500,
                    onClick  = onNavigateToPaymentMethods
                )
                SettingItem(
                    icon     = Icons.Rounded.SupportAgent,
                    title    = "الدعم الفني",
                    subtitle = "تواصل مع الإدارة مباشرة",
                    color    = Emerald500,
                    onClick  = onNavigateToChat
                )
                SettingItem(
                    icon     = Icons.Rounded.Info,
                    title    = "الإصدار",
                    subtitle = "1.0.0",
                    color    = Violet500,
                    onClick  = {}
                )
            }
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector, title: String, subtitle: String,
    color: Color, onClick: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = color) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
