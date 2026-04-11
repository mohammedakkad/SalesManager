package com.trader.salesmanager.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.core.data.local.entity.PendingMessageEntity
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.model.MessageStatus
import com.trader.core.domain.model.SENDER_ADMIN
import com.trader.salesmanager.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(onNavigateUp: () -> Unit, viewModel: ChatViewModel = koinViewModel()) {
    val state     by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size + state.pendingMessages.size) {
        val total = state.messages.size + state.pendingMessages.size
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    // ── قائمة السياق (تعديل / حذف / تحديد) ─────────────────────
    state.contextMessage?.let { ctx ->
        MessageContextMenu(
            isOwn    = ctx.isOwn,
            onEdit   = {
                val msg = state.messages.find { it.id == ctx.messageId }
                if (msg != null) viewModel.startEdit(msg)
            },
            onDelete = { viewModel.deleteMessage(ctx.messageId) },
            onSelect = { viewModel.enterSelectionMode(ctx.messageId) },
            onDismiss = viewModel::dismissContext
        )
    }

    Scaffold(
        bottomBar = {
            ChatInputBar(
                state     = state,
                onInput   = viewModel::updateInput,
                onSend    = viewModel::sendOrSaveMessage,
                onCancel  = viewModel::cancelEdit
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Header ───────────────────────────────────────────
            AnimatedContent(
                targetState = state.isSelectionMode,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "header"
            ) { selMode ->
                if (selMode) {
                    // وضع التحديد المتعدد
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B))
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = viewModel::cancelSelection) {
                                Icon(Icons.Rounded.Close, null, tint = Color.White)
                            }
                            Text(
                                "${state.selectedForDelete.size} محدد",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = viewModel::deleteSelected,
                                enabled = state.selectedForDelete.isNotEmpty()
                            ) {
                                Icon(Icons.Rounded.Delete, null, tint = Color(0xFFEF4444))
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Emerald700, Cyan500)))
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(Color.White.copy(0.2f)),
                                Alignment.Center
                            ) { Icon(Icons.Rounded.SupportAgent, null, tint = Color.White) }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "دعم فني",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "الإدارة متاحة للمساعدة",
                                    color = Color.White.copy(0.7f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // ── قائمة الرسائل ────────────────────────────────────
            if (state.messages.isEmpty() && state.pendingMessages.isEmpty() && !state.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Forum, null, Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.25f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "ابدأ محادثة مع الإدارة",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // جمّع الرسائل مع فواصل التاريخ
                    val grouped = groupMessagesByDate(state.messages)
                    grouped.forEach { (dateLabel, msgs) ->
                        item(key = "date_$dateLabel") {
                            DateDivider(dateLabel)
                        }
                        items(msgs, key = { it.id }) { msg ->
                            ChatBubble(
                                msg            = msg,
                                myId           = state.merchantId,
                                isSelected     = msg.id in state.selectedForDelete,
                                isSelectionMode = state.isSelectionMode,
                                onLongPress    = { viewModel.onLongPress(msg) },
                                onTap          = {
                                    if (state.isSelectionMode) viewModel.toggleSelection(msg.id)
                                }
                            )
                        }
                    }
                    // رسائل pending
                    items(state.pendingMessages, key = { "p_${it.tempId}" }) { pending ->
                        PendingBubble(
                            pending   = pending,
                            onRetry   = { viewModel.retryMessage(pending.tempId) },
                            onDismiss = { viewModel.dismissFailedMessage(pending.tempId) }
                        )
                    }
                }
            }
        }
    }
}

// ── Input Bar ─────────────────────────────────────────────────
@Composable
private fun ChatInputBar(
    state: ChatUiState,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        // شريط التعديل
        AnimatedVisibility(visible = state.editingMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Emerald500.copy(0.1f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Edit, null, tint = Emerald500, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "تعديل الرسالة",
                    style = MaterialTheme.typography.labelMedium,
                    color = Emerald500,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, null, tint = Emerald500, modifier = Modifier.size(18.dp))
                }
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.inputText,
                onValueChange = onInput,
                placeholder = {
                    Text(
                        if (state.editingMessage != null) "تعديل الرسالة..."
                        else "اكتب رسالة للإدارة...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Emerald500,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            FloatingActionButton(
                onClick = onSend,
                containerColor = if (state.inputText.isNotBlank()) Emerald500
                                 else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    if (state.editingMessage != null) Icons.Rounded.Check else Icons.Rounded.Send,
                    null
                )
            }
        }
    }
}

// ── فاصل التاريخ ──────────────────────────────────────────────
@Composable
private fun DateDivider(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(0.3f))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(0.3f))
    }
}

// ── فقاعة رسالة ───────────────────────────────────────────────
@Composable
private fun ChatBubble(
    msg: ChatMessage,
    myId: String,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onLongPress: () -> Unit,
    onTap: () -> Unit
) {
    val isMe = msg.senderId == myId

    val bubbleColor = when {
        msg.isDeleted -> MaterialTheme.colorScheme.surfaceVariant
        isMe          -> Emerald500
        else          -> MaterialTheme.colorScheme.surfaceVariant
    }

    val selectedBg by animateColorAsState(
        if (isSelected) Emerald500.copy(0.15f) else Color.Transparent,
        label = "selBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(selectedBg)
            .pointerInput(isSelectionMode, isMe) {
                detectTapGestures(
                    onLongPress = {
                        // الضغط المطول يعمل فقط على رسائل المستخدم نفسه
                        if (isMe) onLongPress()
                    },
                    onTap = { onTap() }
                )
            }
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // أيقونة الطرف الثاني
        if (!isMe) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(Cyan500.copy(0.15f)),
                Alignment.Center
            ) { Icon(Icons.Rounded.SupportAgent, null, tint = Cyan500, modifier = Modifier.size(14.dp)) }
            Spacer(Modifier.width(6.dp))
        }

        // checkbox في وضع التحديد
        if (isSelectionMode && isMe) {
            Box(
                Modifier.size(20.dp).clip(CircleShape)
                    .background(if (isSelected) Emerald500 else MaterialTheme.colorScheme.outline.copy(0.3f))
                    .border(1.dp, Emerald500, CircleShape),
                Alignment.Center
            ) {
                if (isSelected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(
                    topStart    = if (isMe) 18.dp else 4.dp,
                    topEnd      = if (isMe) 4.dp  else 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd   = 18.dp
                ),
                color    = bubbleColor,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (msg.isDeleted) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Block, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                modifier = Modifier.size(14.dp))
                            Text(
                                "تم حذف هذه الرسالة",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                            )
                        }
                    } else {
                        Text(
                            msg.text,
                            color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // وقت + حالة الرسالة
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (msg.isEdited && !msg.isDeleted) {
                    Text("معدّل", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                        fontSize = 9.sp)
                }
                msg.timestamp?.toDate()?.let { date ->
                    Text(
                        formatTime(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                        fontSize = 10.sp
                    )
                }
                // علامات القراءة فقط لرسائلي
                if (isMe && !msg.isDeleted) {
                    ReadReceipt(status = msg.status)
                }
            }
        }
    }
}

// ── علامات القراءة ✓ / ✓✓ ────────────────────────────────────
@Composable
private fun ReadReceipt(status: MessageStatus) {
    val color = when (status) {
        MessageStatus.READ    -> Color(0xFF06B6D4)  // أزرق = مقروء
        MessageStatus.SENT    -> Color(0xFF94A3B8)  // رمادي = وصلت
        MessageStatus.SENDING -> Color(0xFF94A3B8)
    }
    when (status) {
        MessageStatus.SENT, MessageStatus.SENDING -> {
            Icon(Icons.Rounded.Done, null, tint = color, modifier = Modifier.size(14.dp))
        }
        MessageStatus.READ -> {
            // ✓✓ — أيقونتان متراكبتان
            Box(modifier = Modifier.size(18.dp, 14.dp)) {
                Icon(Icons.Rounded.Done, null, tint = color, modifier = Modifier.size(14.dp).offset(x = 0.dp))
                Icon(Icons.Rounded.Done, null, tint = color, modifier = Modifier.size(14.dp).offset(x = 4.dp))
            }
        }
    }
}

// ── رسالة معلقة ──────────────────────────────────────────────
@Composable
private fun PendingBubble(
    pending: PendingMessageEntity,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                color = if (pending.isFailed) DebtRed.copy(0.12f) else Emerald500.copy(alpha),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    pending.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color    = if (pending.isFailed) DebtRed else Color.White,
                    style    = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(3.dp))
            if (pending.isFailed) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.ErrorOutline, null,
                        tint = DebtRed, modifier = Modifier.size(12.dp))
                    Text("فشل الإرسال",
                        style = MaterialTheme.typography.labelSmall, color = DebtRed, fontSize = 10.sp)
                    TextButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("إعادة", style = MaterialTheme.typography.labelSmall, color = Emerald500, fontSize = 10.sp)
                    }
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("حذف", style = MaterialTheme.typography.labelSmall, color = DebtRed, fontSize = 10.sp)
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        color    = Emerald500,
                        strokeWidth = 1.5.dp
                    )
                    Text("جاري الإرسال...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp)
                }
            }
        }
    }
}

// ── قائمة السياق (Bottom Sheet بديل) ─────────────────────────
@Composable
private fun MessageContextMenu(
    isOwn: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(24.dp),
        title = null,
        text = {
            Column {
                if (isOwn) {
                    ContextOption(icon = Icons.Rounded.Edit, label = "تعديل الرسالة", color = Emerald500) {
                        onEdit(); onDismiss()
                    }
                }
                ContextOption(icon = Icons.Rounded.SelectAll, label = "تحديد متعدد", color = MaterialTheme.colorScheme.onSurface) {
                    onSelect(); onDismiss()
                }
                if (isOwn) {
                    ContextOption(icon = Icons.Rounded.Delete, label = "حذف الرسالة", color = DebtRed) {
                        onDelete(); onDismiss()
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ContextOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

// ── Helpers ───────────────────────────────────────────────────
private fun formatTime(date: Date): String =
    SimpleDateFormat("hh:mm a", Locale("ar")).format(date)

private fun groupMessagesByDate(messages: List<ChatMessage>): Map<String, List<ChatMessage>> {
    val today     = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    val yesterday = run {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
    }
    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return messages
        .groupBy { msg ->
            val d = msg.timestamp?.toDate() ?: return@groupBy "—"
            when (val key = fmt.format(d)) {
                today     -> "اليوم"
                yesterday -> "أمس"
                else      -> key
            }
        }
        .entries
        .associate { it.key to it.value }
}
