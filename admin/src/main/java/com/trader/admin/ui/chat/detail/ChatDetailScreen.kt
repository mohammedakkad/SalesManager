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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
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
val uiState by viewModel.uiState.collectAsState()
val listState = rememberLazyListState()
val clipboard = LocalClipboardManager.current

LaunchedEffect(uiState.visibleMessages.size) {
    if (uiState.visibleMessages.isNotEmpty())
        listState.animateScrollToItem(uiState.visibleMessages.size - 1)
}

androidx.activity.compose.BackHandler(uiState.isSelectionMode) {
    viewModel.cancelSelection()
}

if (uiState.showDeleteDialog) {
    AdminDeleteDialog(
        count = uiState.pendingOwnDeleteIds.size,
        onForMe = viewModel::confirmDeleteForMe,
        onForAll = viewModel::confirmDeleteForEveryone,
        onDismiss = viewModel::dismissDeleteDialog
    )
}

Scaffold(
    containerColor = Navy950,
    bottomBar = {
        AdminInputBar(uiState = uiState, onInput = viewModel::updateText,
            onSend = viewModel::send, onCancel = viewModel::cancelEdit)
    }
) {
    padding ->
    Column(Modifier.fillMaxSize().padding(padding)) {

        // ── App Bar ───────────────────────────────────────────
        AnimatedContent(uiState.isSelectionMode, label = "header",
            transitionSpec = {
                fadeIn(tween(180)) togetherWith fadeOut(tween(180))
            }
        ) {
            selMode ->
            if (selMode) {
                AdminSelectionAppBar(
                    count = uiState.selectedIds.size,
                    canEdit = uiState.canEdit,
                    canCopy = uiState.canCopy,
                    onClose = viewModel::cancelSelection,
                    onCopy = {
                        clipboard.setText(AnnotatedString(viewModel.buildCopyText()))
                        viewModel.cancelSelection()
                    },
                    onEdit = viewModel::startEditSelected,
                    onDelete = viewModel::requestDeleteSelected
                )
            } else {
                Box(
                    Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Cyan500, Indigo500)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(0.2f)), Alignment.Center) {
                            Text(merchantName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                color = Color.White, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall)
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

        // ── الرسائل ───────────────────────────────────────────
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            val grouped = groupByDate(uiState.visibleMessages)
            grouped.forEach {
                (label, msgs) ->
                item(key = "date_$label") {
                    AdminDateDivider(label)
                }
                items(msgs, key = {
                    it.id
                }) {
                    msg ->
                    AdminChatBubble(
                        msg = msg,
                        isSelected = msg.id in uiState.selectedIds,
                        isSelectionMode = uiState.isSelectionMode,
                        onLongPress = {
                            viewModel.onLongPress(msg)
                        },
                        onTap = {
                            viewModel.onTap(msg)
                        }
                    )
                }
            }
        }
    }
}
}

// ── Selection App Bar للأدمن ──────────────────────────────────
@Composable
private fun AdminSelectionAppBar(
count: Int,
canEdit: Boolean,
canCopy: Boolean,
onClose: () -> Unit,
onCopy: () -> Unit,
onEdit: () -> Unit,
onDelete: () -> Unit
) {
Box(
Modifier.fillMaxWidth().background(Navy800).statusBarsPadding()
.padding(horizontal = 4.dp, vertical = 6.dp)
) {
Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
IconButton(onClick = onClose) {
Icon(Icons.Rounded.Close, null, tint = Slate100)
}
// عداد متحرك
Box(Modifier.weight(1f).padding(start = 4.dp)) {
AnimatedContent(count,
transitionSpec = {
val up = targetState > initialState
(slideInVertically(tween(220)) {
if (up) -it else it
} + fadeIn(tween(180))) togetherWith
(slideOutVertically(tween(220)) {
if (up) it else -it
} + fadeOut(tween(160)))
}, label = "admin_counter"
) {
c ->
Row(verticalAlignment = Alignment.CenterVertically) {
Text("$c", color = Slate100, fontWeight = FontWeight.Bold,
style = MaterialTheme.typography.titleLarge, fontSize = 20.sp)
Spacer(Modifier.width(6.dp))
Text(if (c == 1) "محددة" else "محددات", color = Slate400,
style = MaterialTheme.typography.bodyMedium)
}
}
}
AnimatedVisibility(canCopy, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
IconButton(onClick = onCopy) {
Icon(Icons.Rounded.ContentCopy, null, tint = Slate100)
}
}
AnimatedVisibility(canEdit, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
IconButton(onClick = onEdit) {
Icon(Icons.Rounded.Edit, null, tint = Cyan400)
}
}
IconButton(onClick = onDelete, enabled = count > 0) {
Icon(Icons.Rounded.Delete, null,
tint = if (count > 0) Color(0xFFEF4444) else Slate600)
}
}
}
}

// ── Dialog الحذف ──────────────────────────────────────────────
@Composable
private fun AdminDeleteDialog(count: Int, onForMe: () -> Unit, onForAll: () -> Unit, onDismiss: () -> Unit) {
AlertDialog(
onDismissRequest = onDismiss,
containerColor = Navy900,
shape = RoundedCornerShape(24.dp),
icon = {
Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEF4444).copy(0.12f)), Alignment.Center) {
Icon(Icons.Rounded.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
}
},
title = {
Text(if (count == 1) "حذف رسالة" else "حذف $count رسائل",
fontWeight = FontWeight.Bold, color = Slate100)
},
text = {
Text("كيف تريد الحذف؟", color = Slate400)
},
confirmButton = {
Button(onClick = onForAll,
colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
Icon(Icons.Rounded.DeleteForever, null, modifier = Modifier.size(18.dp))
Spacer(Modifier.width(6.dp))
Text("حذف عند الجميع", fontWeight = FontWeight.SemiBold)
}
},
dismissButton = {
OutlinedButton(onClick = onForMe, shape = RoundedCornerShape(12.dp),
modifier = Modifier.fillMaxWidth(),
border = ButtonDefaults.outlinedButtonBorder(true).copy(
brush = androidx.compose.ui.graphics.SolidColor(Slate600))) {
Icon(Icons.Rounded.PersonRemove, null, modifier = Modifier.size(18.dp), tint = Slate400)
Spacer(Modifier.width(6.dp))
Text("حذف عندي فقط", color = Slate400)
}
}
)
}

// ── فقاعة الأدمن ──────────────────────────────────────────────
@Composable
private fun AdminChatBubble(
msg: ChatMessage,
isSelected: Boolean,
isSelectionMode: Boolean,
onLongPress: () -> Unit,
onTap: () -> Unit
) {
val isAdmin = msg.senderId == SENDER_ADMIN
val haptic = LocalHapticFeedback.current

val selectedBg by animateColorAsState(
if (isSelected) Indigo500.copy(0.15f) else Color.Transparent, tween(200), label = "sel_bg"
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
horizontalArrangement = if (isAdmin) Arrangement.End else Arrangement.Start,
verticalAlignment = Alignment.Bottom
) {
if (!isAdmin) {
Box(Modifier.size(28.dp).clip(CircleShape).background(Cyan500.copy(0.2f)), Alignment.Center) {
Icon(Icons.Rounded.Person, null, tint = Cyan500, modifier = Modifier.size(14.dp))
}
Spacer(Modifier.width(6.dp))
}

if (isSelectionMode && !isAdmin) {
val checkBg by animateColorAsState(
if (isSelected) Indigo500 else Slate700.copy(0.6f), tween(150), label = "check_other"
)
Box(Modifier.size(20.dp).clip(CircleShape).background(checkBg)
.border(1.5.dp, if (isSelected) Indigo500 else Slate600, CircleShape), Alignment.Center) {
if (isSelected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
}
Spacer(Modifier.width(6.dp))
}

Column(horizontalAlignment = if (isAdmin) Alignment.End else Alignment.Start) {
Surface(
shape = RoundedCornerShape(
topStart = if (isAdmin) 18.dp else 4.dp, topEnd = if (isAdmin) 4.dp else 18.dp,
bottomStart = 18.dp, bottomEnd = 18.dp
),
color = if (isAdmin) Indigo500 else Navy900,
modifier = Modifier.widthIn(max = 280.dp)
) {
if (msg.isDeleted) {
Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
horizontalArrangement = Arrangement.spacedBy(6.dp),
verticalAlignment = Alignment.CenterVertically) {
Icon(Icons.Rounded.Block, null, tint = Slate600, modifier = Modifier.size(14.dp))
Text("تم حذف هذه الرسالة", style = MaterialTheme.typography.bodySmall,
fontStyle = FontStyle.Italic, color = Slate600)
}
} else {
Text(msg.text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
color = if (isAdmin) Color.White else Slate100,
style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
}
}
Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
horizontalArrangement = Arrangement.spacedBy(4.dp),
verticalAlignment = Alignment.CenterVertically) {
if (msg.isEdited && !msg.isDeleted)
Text("معدّل", style = MaterialTheme.typography.labelSmall, color = Slate600, fontSize = 9.sp)
msg.timestamp?.toDate()?.let {
Text(SimpleDateFormat("hh:mm a", Locale("ar")).format(it),
style = MaterialTheme.typography.labelSmall, color = Slate600, fontSize = 10.sp)
}
if (isAdmin && !msg.isDeleted) AdminReadReceipt(msg.status)
}
}

if (isSelectionMode && isAdmin) {
Spacer(Modifier.width(6.dp))
val checkBg by animateColorAsState(
if (isSelected) Indigo500 else Slate700.copy(0.6f), tween(150), label = "check_admin"
)
Box(Modifier.size(20.dp).clip(CircleShape).background(checkBg)
.border(1.5.dp, if (isSelected) Indigo500 else Slate600, CircleShape), Alignment.Center) {
if (isSelected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
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
} else -> Icon(Icons.Rounded.Done, null, tint = color, modifier = Modifier.size(14.dp))
}
}

@Composable
private fun AdminInputBar(uiState: AdminChatUiState, onInput: (String) -> Unit, onSend: () -> Unit, onCancel: () -> Unit) {
Column(Modifier.fillMaxWidth().background(Navy900).navigationBarsPadding()) {
AnimatedVisibility(uiState.editingMessage != null) {
Row(Modifier.fillMaxWidth().background(Indigo500.copy(0.15f)).padding(horizontal = 16.dp, vertical = 8.dp),
verticalAlignment = Alignment.CenterVertically) {
Icon(Icons.Rounded.Edit, null, tint = Indigo500, modifier = Modifier.size(16.dp))
Spacer(Modifier.width(8.dp))
Text("تعديل الرسالة", style = MaterialTheme.typography.labelMedium,
color = Indigo500, modifier = Modifier.weight(1f))
IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
Icon(Icons.Rounded.Close, null, tint = Slate400, modifier = Modifier.size(18.dp))
}
}
}
Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
OutlinedTextField(
value = uiState.inputText, onValueChange = onInput,
placeholder = {
Text("اكتب رسالة...", color = Slate600)
},
modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), maxLines = 5,
colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Indigo500,
unfocusedBorderColor = Slate700, focusedTextColor = Slate100, unfocusedTextColor = Slate100)
)
FloatingActionButton(onClick = onSend,
containerColor = if (uiState.editingMessage != null) Cyan400 else Indigo500,
contentColor = Color.White, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(52.dp)) {
Icon(if (uiState.editingMessage != null) Icons.Rounded.Check else Icons.Rounded.Send, null)
}
}
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

private fun groupByDate(messages: List<ChatMessage>): Map<String, List<ChatMessage>> {
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