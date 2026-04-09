package com.trader.admin.ui.chat.detail

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
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.model.MessageStatus
import com.trader.core.domain.model.SENDER_ADMIN
import com.trader.admin.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatDetailScreen(
    merchantId: String,
    merchantName: String,
    onNavigateUp: () -> Unit,
    viewModel: ChatDetailViewModel = koinViewModel(parameters = { parametersOf(merchantId) })
) {
    val uiState   by viewModel.uiState.collectAsState()
    val listState  = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size - 1)
    }

    // قائمة السياق
    uiState.contextMessageId?.let { ctxId ->
        val msg = uiState.messages.find { it.id == ctxId }
        if (msg != null) {
            AdminMessageContextMenu(
                isOwn     = msg.senderId == SENDER_ADMIN,
                onEdit    = { viewModel.startEdit(msg) },
                onDelete  = { viewModel.deleteMessage(ctxId) },
                onSelect  = { viewModel.enterSelectionMode(ctxId) },
                onDismiss = viewModel::dismissContext
            )
        }
    }

    Scaffold(
        containerColor = Navy950,
        bottomBar = {
            AdminChatInputBar(
                uiState   = uiState,
                onInput   = viewModel::updateText,
                onSend    = viewModel::sendOrSave,
                onCancel  = viewModel::cancelEdit
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Header ───────────────────────────────────────────
            AnimatedContent(uiState.isSelectionMode, label = "header",
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }
            ) { selMode ->
                if (selMode) {
                    Box(
                        Modifier.fillMaxWidth().background(Navy900).statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = viewModel::cancelSelection) {
                                Icon(Icons.Rounded.Close, null, tint = Slate100)
                            }
                            Text("${uiState.selectedForDelete.size} محدد",
                                color = Slate100, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = viewModel::deleteSelected,
                                enabled = uiState.selectedForDelete.isNotEmpty()
                            ) {
                                Icon(Icons.Rounded.Delete, null, tint = Color(0xFFEF4444))
                            }
                        }
                    }
                } else {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Cyan500, Indigo500)))
                            .statusBarsPadding()
                            .padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier.size(38.dp).clip(CircleShape)
                                    .background(Color.White.copy(0.2f)),
                                Alignment.Center
                            ) {
                                Text(
                                    merchantName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    color = Color.White, fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(merchantName, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold, color = Color.White)
                                Text("دردشة الدعم الفني", color = Color.White.copy(0.7f),
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // ── الرسائل ──────────────────────────────────────────
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                val grouped = groupByDate(uiState.messages)
                grouped.forEach { (label, msgs) ->
                    item(key = "date_$label") { AdminDateDivider(label) }
                    items(msgs, key = { it.id }) { msg ->
                        AdminChatBubble(
                            msg             = msg,
                            isSelected      = msg.id in uiState.selectedForDelete,
                            isSelectionMode = uiState.isSelectionMode,
                            onLongPress     = {
                                if (uiState.isSelectionMode) viewModel.toggleSelection(msg.id)
                                else viewModel.showContext(msg.id)
                            },
                            onTap           = {
                                if (uiState.isSelectionMode) viewModel.toggleSelection(msg.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Input Bar ─────────────────────────────────────────────────
@Composable
private fun AdminChatInputBar(
    uiState: AdminChatUiState,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().background(Navy900).navigationBarsPadding()
    ) {
        AnimatedVisibility(uiState.editingMessage != null) {
            Row(
                Modifier.fillMaxWidth().background(Indigo500.copy(0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Edit, null, tint = Indigo500, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("تعديل الرسالة", style = MaterialTheme.typography.labelMedium,
                    color = Indigo500, modifier = Modifier.weight(1f))
                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, null, tint = Indigo500, modifier = Modifier.size(18.dp))
                }
            }
        }
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = onInput,
                placeholder = {
                    Text(
                        if (uiState.editingMessage != null) "تعديل الرسالة..." else "اكتب رسالة...",
                        color = Slate600
                    )
                },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor     = Indigo500, unfocusedBorderColor = Slate700,
                    focusedTextColor       = Slate100,  unfocusedTextColor   = Slate100,
                    focusedContainerColor  = Slate800,  unfocusedContainerColor = Slate800
                ),
                maxLines = 3
            )
            FloatingActionButton(
                onClick        = onSend,
                containerColor = Indigo500,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(14.dp),
                modifier       = Modifier.size(52.dp)
            ) {
                Icon(if (uiState.editingMessage != null) Icons.Rounded.Check else Icons.Rounded.Send, null)
            }
        }
    }
}

// ── فقاعة الرسالة ────────────────────────────────────────────
@Composable
private fun AdminChatBubble(
    msg: ChatMessage,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onLongPress: () -> Unit,
    onTap: () -> Unit
) {
    val isAdmin = msg.senderId == SENDER_ADMIN
    val selectedBg by animateColorAsState(
        if (isSelected) Indigo500.copy(0.15f) else Color.Transparent, label = "sel"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(selectedBg)
            .pointerInput(isSelectionMode) {
                detectTapGestures(onLongPress = { onLongPress() }, onTap = { onTap() })
            }
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isAdmin) Arrangement.End else Arrangement.Start,
        verticalAlignment     = Alignment.Bottom
    ) {
        if (!isAdmin) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(Cyan500.copy(0.2f)),
                Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, null, tint = Cyan500, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(6.dp))
        }

        if (isSelectionMode && isAdmin) {
            Box(
                Modifier.size(20.dp).clip(CircleShape)
                    .background(if (isSelected) Indigo500 else Slate700)
                    .border(1.dp, Indigo500, CircleShape),
                Alignment.Center
            ) {
                if (isSelected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (isAdmin) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isAdmin) 18.dp else 4.dp,
                    topEnd   = if (isAdmin) 4.dp else 18.dp,
                    bottomStart = 18.dp, bottomEnd = 18.dp
                ),
                color    = if (isAdmin) Indigo500 else Navy900,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (msg.isDeleted) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Block, null, tint = Slate600, modifier = Modifier.size(14.dp))
                            Text("تم حذف هذه الرسالة",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic, color = Slate600)
                        }
                    } else {
                        Text(msg.text,
                            color = if (isAdmin) Color.White else Slate100,
                            style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                    }
                }
            }
            // وقت + حالة
            Row(
                Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (msg.isEdited && !msg.isDeleted) {
                    Text("معدّل", style = MaterialTheme.typography.labelSmall,
                        color = Slate600, fontSize = 9.sp)
                }
                msg.timestamp?.toDate()?.let {
                    Text(SimpleDateFormat("hh:mm a", Locale("ar")).format(it),
                        style = MaterialTheme.typography.labelSmall, color = Slate600, fontSize = 10.sp)
                }
                if (isAdmin && !msg.isDeleted) {
                    AdminReadReceipt(msg.status)
                }
            }
        }
    }
}

@Composable
private fun AdminReadReceipt(status: MessageStatus) {
    val color = if (status == MessageStatus.READ) Cyan500 else Slate600
    when (status) {
        MessageStatus.READ -> {
            Box(Modifier.size(18.dp, 14.dp)) {
                Icon(Icons.Rounded.Done, null, tint = color, modifier = Modifier.size(14.dp).offset(x = 0.dp))
                Icon(Icons.Rounded.Done, null, tint = color, modifier = Modifier.size(14.dp).offset(x = 4.dp))
            }
        }
        else -> Icon(Icons.Rounded.Done, null, tint = color, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun AdminDateDivider(label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = Slate800)
        Surface(shape = RoundedCornerShape(20.dp), color = Slate800, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(label, Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall, color = Slate400)
        }
        HorizontalDivider(Modifier.weight(1f), color = Slate800)
    }
}

@Composable
private fun AdminMessageContextMenu(
    isOwn: Boolean, onEdit: () -> Unit, onDelete: () -> Unit,
    onSelect: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Navy900,
        shape            = RoundedCornerShape(24.dp),
        title = null,
        text = {
            Column {
                if (isOwn) {
                    AdminContextOption(Icons.Rounded.Edit, "تعديل الرسالة", Indigo500) { onEdit(); onDismiss() }
                }
                AdminContextOption(Icons.Rounded.SelectAll, "تحديد متعدد", Slate400) { onSelect(); onDismiss() }
                if (isOwn) {
                    AdminContextOption(Icons.Rounded.Delete, "حذف الرسالة", Color(0xFFEF4444)) { onDelete(); onDismiss() }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun AdminContextOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

private fun groupByDate(messages: List<ChatMessage>): Map<String, List<ChatMessage>> {
    val today     = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        .let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) }
    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return messages.groupBy { msg ->
        val d = msg.timestamp?.toDate() ?: return@groupBy "—"
        when (val k = fmt.format(d)) { today -> "اليوم"; yesterday -> "أمس"; else -> k }
    }
}
