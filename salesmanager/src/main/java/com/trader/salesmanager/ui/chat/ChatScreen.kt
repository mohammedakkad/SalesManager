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
fun ChatScreen(onNavigateUp: () -> Unit, viewModel: ChatViewModel = koinViewModel()) {
    val state     by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size + state.pendingMessages.size) {
        val total = state.messages.size + state.pendingMessages.size
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = viewModel::updateInput,
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
                        onClick = viewModel::sendMessage,
                        containerColor = if (state.inputText.isNotBlank()) Emerald500
                                         else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Rounded.Send, null)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Emerald700, Cyan500)))
                    .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(0.2f)),
                        Alignment.Center
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

            // Empty state
            if (state.messages.isEmpty() && state.pendingMessages.isEmpty() && !state.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Forum, null, Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
                        Spacer(Modifier.height(12.dp))
                        Text("ابدأ محادثة مع الإدارة",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Sent messages from Firestore
                    items(state.messages, key = { it.id }) { msg ->
                        ChatBubble(msg = msg, myId = state.merchantId)
                    }
                    // Pending / failed messages
                    items(state.pendingMessages, key = { it.tempId }) { pending ->
                        PendingBubble(
                            pending = pending,
                            onRetry = { viewModel.retryMessage(pending.tempId) },
                            onDismiss = { viewModel.dismissFailedMessage(pending.tempId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage, myId: String) {
    val isMe = msg.senderId == myId
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(Cyan500.copy(0.15f)),
                Alignment.Center
            ) { Icon(Icons.Rounded.SupportAgent, null, tint = Cyan500, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isMe) 16.dp else 4.dp,
                topEnd   = if (isMe) 4.dp  else 16.dp,
                bottomStart = 16.dp, bottomEnd = 16.dp
            ),
            color = if (isMe) Emerald500 else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                msg.text,
                modifier = Modifier.padding(10.dp, 8.dp),
                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PendingBubble(pending: PendingMessage, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        0.5f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha"
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = if (pending.isFailed) DebtRed.copy(0.15f) else Emerald500.copy(alpha),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    pending.text,
                    modifier = Modifier.padding(10.dp, 8.dp),
                    color = if (pending.isFailed) DebtRed else Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (pending.isFailed) {
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("فشل الإرسال", style = MaterialTheme.typography.labelSmall, color = DebtRed)
                    TextButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, null,
                            tint = Emerald500, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إعادة إرسال", style = MaterialTheme.typography.labelSmall, color = Emerald500)
                    }
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("حذف", style = MaterialTheme.typography.labelSmall, color = DebtRed)
                    }
                }
            } else {
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(10.dp), color = Emerald500, strokeWidth = 1.5.dp)
                    Text("جاري الإرسال...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}