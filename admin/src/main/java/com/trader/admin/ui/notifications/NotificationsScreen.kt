package com.trader.admin.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.admin.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun NotificationsScreen(
    onNavigateUp: () -> Unit,
    viewModel: NotificationsViewModel = koinViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(containerColor = Navy950) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Indigo500, Violet500)))
                    .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("الإشعارات",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${notifications.size} إشعار",
                            color = Color.White.copy(0.7f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (notifications.isEmpty()) {
                // حالة فارغة
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.NotificationsNone, null,
                            tint = Slate600, modifier = Modifier.size(64.dp))
                        Text("لا توجد إشعارات", color = Slate400,
                            style = MaterialTheme.typography.bodyLarge)
                        Text("جميع البائعين في حالة جيدة",
                            color = Slate600, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.merchantId + it.type }) { notif ->
                        NotificationCard(notif)
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notif: MerchantNotification) {
    val (color, icon) = when (notif.type) {
        NotifType.EXPIRING_SOON -> ExpiredAmber to Icons.Rounded.Warning
        NotifType.EXPIRED       -> DisabledRose to Icons.Rounded.Error
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy900),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(notif.merchantName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = Slate100)
                Spacer(Modifier.height(2.dp))
                Text(notif.message,
                    style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
    }
}