package com.trader.salesmanager.ui.chat

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
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.model.SENDER_ADMIN
import com.trader.salesmanager.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChatScreen(
    onNavigateUp: () -> Unit,
    viewModel: ChatViewModel = koinViewModel()
) {
    val messages  by viewModel.messages.collectAsState()
    val text      by viewModel.text.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = viewModel::updateText,
                    placeholder = { Text("اكتب رسالة للإدارة...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald500,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                FloatingActionButton(
                    onClick = viewModel::send,
                    containerColor = if (text.isNotBlank()) Emerald500 else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(52.dp)
                ) {
                    AnimatedContent(isSending, label = "send") { sending ->
                        if (sending) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.Rounded.Send, null)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Emerald700, Cyan500)))
                    .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Color.White.copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.SupportAgent, null, tint = Color.White) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("دعم فني", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("الإدارة متاحة للمساعدة",
                            color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Messages
            if (messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Forum, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                        Spacer(Modifier.height(12.dp))
                        Text("لا توجد رسائل بعد",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium)
                        Text("ابدأ محادثة مع الإدارة",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
                        ) {
                            ChatBubble(msg)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isAdmin = msg.senderId == SENDER_ADMIN
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAdmin) Arrangement.Start else Arrangement.End
    ) {
        if (isAdmin) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(Cyan500.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.SupportAgent, null, tint = Cyan500, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isAdmin) 4.dp else 16.dp,
                topEnd = if (isAdmin) 16.dp else 4.dp,
                bottomStart = 16.dp, bottomEnd = 16.dp
            ),
            color = if (isAdmin) MaterialTheme.colorScheme.surfaceVariant else Emerald500,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp, 8.dp)) {
                Text(
                    msg.text,
                    color = if (isAdmin) MaterialTheme.colorScheme.onSurface else Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (msg.senderName.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        msg.senderName,
                        color = if (isAdmin) MaterialTheme.colorScheme.onSurfaceVariant
                                else Color.White.copy(0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}