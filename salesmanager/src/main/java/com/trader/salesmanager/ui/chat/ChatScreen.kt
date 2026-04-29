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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.core.data.local.entity.PendingMessageEntity
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.model.MessageStatus
import com.trader.core.domain.model.SENDER_ADMIN
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(onNavigateUp: () -> Unit, viewModel: ChatViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    // تمرير تلقائي للأسفل عند رسالة جديدة
    LaunchedEffect(state.visibleMessages.size + state.pendingMessages.size) {
        val total = state.visibleMessages.size + state.pendingMessages.size
        if (total > 0) listState.animateScrollToItem(0)
    }

    // ── Back Handler: إلغاء وضع التحديد ──────────────────────────
    androidx.activity.compose.BackHandler(state.isSelectionMode) {
        viewModel.cancelSelection()
    }

    // ── Dialog حذف رسائلي ─────────────────────────────────────────
    if (state.showDeleteDialog) {
        DeleteOwnMessagesDialog(
            count = state.pendingOwnDeleteIds.size,
            onForMe = viewModel::confirmDeleteForMe,
            onForAll = viewModel::confirmDeleteForEveryone,
            onDismiss = viewModel::dismissDeleteDialog
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ChatInputBar(
                state = state,
                onInput = viewModel::updateInput,
                onSend = viewModel::sendOrSaveMessage,
                onCancel = viewModel::cancelEdit,
                modifier = Modifier.imePadding()
            )
        }
    ) {
        padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── App Bar ───────────────────────────────────────────
            AnimatedContent(
                targetState = state.isSelectionMode,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(180))
                },
                label = "appbar"
            ) {
                selMode ->
                if (selMode) {
                    SelectionAppBar(
                        count = state.selectedIds.size,
                        canEdit = state.canEdit,
                        canCopy = state.canCopy,
                        onClose = viewModel::cancelSelection,
                        onCopy = {
                            clipboard.setText(AnnotatedString(viewModel.buildCopyText()))
                            viewModel.cancelSelection()
                        },
                        onEdit = viewModel::startEditSelected,
                        onDelete = viewModel::requestDeleteSelected
                    )
                } else {
                    NormalChatAppBar(onNavigateUp = onNavigateUp)
                }
            }

            // ── قائمة الرسائل ─────────────────────────────────────
            if (state.visibleMessages.isEmpty() && state.pendingMessages.isEmpty() && !state.isLoading) {
                EmptyChatState(Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.pendingMessages, key = {
                        "p_${it.tempId}"
                    }) {
                        pending ->
                        PendingBubble(
                            pending = pending,
                            onRetry = {
                                viewModel.retryMessage(pending.tempId)
                            },
                            onDismiss = {
                                viewModel.dismissFailedMessage(pending.tempId)
                            }
                        )
                    }
                    val grouped = groupMessagesByDate(state.visibleMessages)
                    grouped.forEach {
                        (dateLabel, msgs) ->

                        items(msgs, key = {
                            it.id
                        }) {
                            msg ->
                            ChatBubble(
                                msg = msg,
                                myId = state.merchantId,
                                isSelected = msg.id in state.selectedIds,
                                isSelectionMode = state.isSelectionMode,
                                onLongPress = {
                                    viewModel.onLongPress(msg)
                                },
                                onTap = {
                                    viewModel.onTap(msg)
                                }
                            )
                        }
                        item(key = "date_$dateLabel") {
                            DateDivider(dateLabel)
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ── App Bar العادي ────────────────────────────────────────────
@Composable
private fun NormalChatAppBar(onNavigateUp: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
        .background(Brush.horizontalGradient(listOf(Emerald700, Cyan500)))
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateUp) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(0.2f)),
                Alignment.Center
            ) {
                Icon(Icons.Rounded.SupportAgent, null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("دعم فني", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text("الإدارة متاحة للمساعدة", color = Color.White.copy(0.7f),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ── App Bar وضع التحديد — مع عداد متحرك ───────────────────────
@Composable
private fun SelectionAppBar(
    count: Int,
    canEdit: Boolean,
    canCopy: Boolean,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth()
        .background(
            Brush.horizontalGradient(listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant
            ))
        )
        .background(Emerald900.copy(0.97f))
        .statusBarsPadding()
        .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {

            // زر إغلاق
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, null, tint = Color.White)
            }

            // ✅ عداد متحرك — يرتفع عند الزيادة وينزل عند النقص
            AnimatedSelectionCounter(count = count, modifier = Modifier.weight(1f))

            // نسخ
            AnimatedVisibility(canCopy, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Rounded.ContentCopy, null, tint = Color.White.copy(0.9f))
                }
            }

            // تعديل — فقط عند محددة واحدة هي رسالتي
            AnimatedVisibility(canEdit, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, null, tint = Emerald400)
                }
            }

            // حذف
            IconButton(onClick = onDelete, enabled = count > 0) {
                Icon(Icons.Rounded.Delete, null,
                    tint = if (count > 0) DebtRed else Color.White.copy(0.3f))
            }
        }
    }
}

// ✅ عداد متحرك: يرتفع عند زيادة الرقم، ينزل عند نقصانه
@Composable
private fun AnimatedSelectionCounter(count: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(start = 4.dp)) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically(tween(220)) {
                    if (up) -it else it
                } + fadeIn(tween(180))) togetherWith
                (slideOutVertically(tween(220)) {
                    if (up) it else -it
                } + fadeOut(tween(160)))
            },
            label = "sel_counter"
        ) {
            c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$c",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 20.sp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (c == 1) "محددة" else "محددات",
                    color = Color.White.copy(0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ── Dialog حذف رسائلي ─────────────────────────────────────────
@Composable
private fun DeleteOwnMessagesDialog(
    count: Int,
    onForMe: () -> Unit,
    onForAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(DebtRed.copy(0.1f)),
                Alignment.Center
            ) {
                Icon(Icons.Rounded.Delete, null, tint = DebtRed, modifier = Modifier.size(24.dp))
            }
        },
        title = {
            Text(
                if (count == 1) "حذف رسالة" else "حذف $count رسائل",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "كيف تريد حذف ${if (count == 1) "هذه الرسالة" else "هذه الرسائل"}؟",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            // حذف عند الجميع
            Button(
                onClick = onForAll,
                colors = ButtonDefaults.buttonColors(containerColor = DebtRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.DeleteForever, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("حذف عند الجميع", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            // حذف عندي فقط
            OutlinedButton(
                onClick = onForMe,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.PersonRemove, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("حذف عندي فقط")
            }
        }
    )
}

// ── فقاعة الرسالة ──────────────────────────────────────────────
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
    val haptic = LocalHapticFeedback.current

    // ✅ خلفية التحديد مع انيميشن
    val selectedBg by animateColorAsState(
        targetValue = if (isSelected) Emerald500.copy(0.12f) else Color.Transparent,
        animationSpec = tween(200),
        label = "bubble_bg"
    )

    Row(
        modifier = Modifier
        .fillMaxWidth()
        .background(selectedBg)
        .pointerInput(isSelectionMode, msg.id) {
            detectTapGestures(
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
                onTap = {
                    onTap()
                }
            )
        }
        .padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // أيقونة الطرف الآخر
        if (!isMe) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(Cyan500.copy(0.2f)),
                Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, null, tint = Cyan500, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(6.dp))
        }

        // ✅ Checkbox لكل الرسائل في وضع التحديد (ليس فقط رسائلي)
        if (isSelectionMode) {
            val checkBg by animateColorAsState(
                if (isSelected) Emerald500 else Slate700.copy(0.6f), tween(150), label = "check_bg"
            )
            if (!isMe) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape).background(checkBg)
                    .border(1.5.dp, if (isSelected) Emerald500 else Slate400, CircleShape),
                    Alignment.Center
                ) {
                    if (isSelected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
                Spacer(Modifier.width(6.dp))
            }
        }

        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            // فقاعة النص
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isMe) 18.dp else 4.dp,
                    topEnd = if (isMe) 4.dp else 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                ),
                color = if (isMe) Emerald700 else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                if (msg.isDeleted) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Block, null, tint = appColors.textSubtle, modifier = Modifier.size(14.dp))
                        Text("تم حذف هذه الرسالة",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic, color = appColors.textSubtle)
                    }
                } else {
                    Text(
                        msg.text,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
            // وقت + معدّل + علامات قراءة
            Row(
                Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (msg.isEdited && !msg.isDeleted) {
                    Text("معدّل", style = MaterialTheme.typography.labelSmall,
                        color = appColors.textSubtle, fontSize = 9.sp)
                }
                msg.timestamp?.toDate()?.let {
                    Text(formatTime(it), style = MaterialTheme.typography.labelSmall,
                        color = appColors.textSubtle, fontSize = 10.sp)
                }
                if (isMe && !msg.isDeleted) ReadReceipt(msg.status)
            }
        }

        // Checkbox لرسائلي في وضع التحديد
        if (isSelectionMode && isMe) {
            Spacer(Modifier.width(6.dp))
            val checkBg by animateColorAsState(
                if (isSelected) Emerald500 else Slate700.copy(0.6f), tween(150), label = "check_me_bg"
            )
            Box(
                Modifier.size(20.dp).clip(CircleShape).background(checkBg)
                .border(1.5.dp, if (isSelected) Emerald500 else Slate400, CircleShape),
                Alignment.Center
            ) {
                if (isSelected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

// ── Input Bar ──────────────────────────────────────────────────
@Composable
private fun ChatInputBar(
    state: ChatUiState,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .navigationBarsPadding()
    ) {
        AnimatedVisibility(state.editingMessage != null) {
            Row(
                Modifier.fillMaxWidth()
                .background(Emerald500.copy(0.1f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Edit, null, tint = Emerald500, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("تعديل الرسالة", style = MaterialTheme.typography.labelMedium,
                    color = Emerald500, modifier = Modifier.weight(1f))
                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, null, tint = appColors.textSubtle, modifier = Modifier.size(18.dp))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.inputText,
                onValueChange = onInput,
                placeholder = {
                    Text("اكتب رسالة...", color = appColors.textSubtle)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald500,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.5f)
                )
            )
            FloatingActionButton(
                onClick = onSend,
                containerColor = if (state.editingMessage != null) Emerald400 else Emerald500,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(if (state.editingMessage != null) Icons.Rounded.Check else Icons.Rounded.Send, null)
            }
        }
    }
}

// ── شاشة فارغة ────────────────────────────────────────────────
@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Forum, null, Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2f))
            Spacer(Modifier.height(12.dp))
            Text("ابدأ محادثة مع الإدارة",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
        }
    }
}

// ── علامات القراءة ────────────────────────────────────────────
@Composable
private fun ReadReceipt(status: MessageStatus) {
    val color = when (status) {
        MessageStatus.READ -> Cyan500
        else -> appColors.textSubtle
    }
    when (status) {
        MessageStatus.READ -> {
            Box(Modifier.size(18.dp, 14.dp)) {
                Icon(Icons.Rounded.Done, null, tint = color, modifier = Modifier.size(14.dp).offset(x = 0.dp))
                Icon(Icons.Rounded.Done, null, tint = color, modifier = Modifier.size(14.dp).offset(x = 4.dp))
            }
        } else -> Icon(Icons.Rounded.Done, null, tint = color, modifier = Modifier.size(14.dp))
    }
}

// ── رسالة معلقة ───────────────────────────────────────────────
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                color = if (pending.isFailed) DebtRed.copy(0.12f) else Emerald500.copy(alpha),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(pending.text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (pending.isFailed) DebtRed else Color.White,
                    style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(3.dp))
            if (pending.isFailed) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = DebtRed, modifier = Modifier.size(12.dp))
                    Text("فشل الإرسال", style = MaterialTheme.typography.labelSmall, color = DebtRed, fontSize = 10.sp)
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                        Text("إعادة", style = MaterialTheme.typography.labelSmall, color = Emerald500, fontSize = 10.sp)
                    }
                    TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                        Text("حذف", style = MaterialTheme.typography.labelSmall, color = DebtRed, fontSize = 10.sp)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(10.dp), color = Emerald500, strokeWidth = 1.5.dp)
                    Text("جاري الإرسال...", style = MaterialTheme.typography.labelSmall,
                        color = appColors.textSubtle, fontSize = 10.sp)
                }
            }
        }
    }
}

// ── فاصل التاريخ ──────────────────────────────────────────────
@Composable
private fun DateDivider(label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(0.3f))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(label, Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(0.3f))
    }
}

// ── Helpers ───────────────────────────────────────────────────
private fun formatTime(date: Date): String =
SimpleDateFormat("hh:mm a", Locale("ar")).format(date)

private fun groupMessagesByDate(messages: List<ChatMessage>): Map<String, List<ChatMessage>> {
    val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    .time.let {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
    }
    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return messages.groupBy {
        msg ->
        val d = msg.timestamp?.toDate() ?: return@groupBy "—"
        when (val k = fmt.format(d)) {
            today -> "اليوم"; yesterday -> "أمس"; else -> k
        }
    }
}