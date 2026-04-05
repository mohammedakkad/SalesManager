package com.trader.admin.ui.chat.list

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
fun ChatListScreen(onNavigateUp: () -> Unit, onChatClick: (String) -> Unit, viewModel: ChatListViewModel = koinViewModel()) {
    val chats by viewModel.chats.collectAsState()

    Scaffold(containerColor = Navy950) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Cyan500, Indigo500)))
                .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateUp) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("الدردشة والدعم", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${chats.size} محادثة", color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(chats, key = { _, c -> c.merchant.id }) { index, item ->
                    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                    AnimatedVisibility(visible, enter = slideInVertically(animationSpec = tween(300, index * 40)) + fadeIn()) {
                        ChatListItem(item = item, onClick = { onChatClick(item.merchant.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatListItem(item: com.trader.admin.ui.chat.list.ChatListItem, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Navy900), elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Cyan500.copy(0.3f), Cyan500.copy(0.1f)))),
                contentAlignment = Alignment.Center) {
                Text(item.merchant.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Cyan500, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.merchant.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Slate100)
                Text(item.merchant.phone, style = MaterialTheme.typography.bodySmall, color = Slate400)
            }
            if (item.unreadCount > 0) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(Indigo500), contentAlignment = Alignment.Center) {
                    Text("${item.unreadCount}", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = Slate600)
        }
    }
}
