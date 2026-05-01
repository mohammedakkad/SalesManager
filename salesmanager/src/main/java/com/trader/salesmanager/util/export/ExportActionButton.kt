package com.trader.salesmanager.util.export

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors
import kotlinx.coroutines.launch
import java.io.File

/**
 * زر التصدير + Sheet الإجراءات.
 *
 * Context يُستخدم هنا فقط (Composable = UI layer) — وليس في ViewModel.
 * كل Intent/FileProvider/MediaStore ينطلق من هنا.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportActionButton(
    target: ExportTarget,
    state: ExportState,
    onExport: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current      // ← Context في الـ Composable ✅
    val haptic  = LocalHapticFeedback.current
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is ExportState.Success) showSheet = true
    }

    // ── زر التصدير ────────────────────────────────────────────
    val isLoading = state is ExportState.Loading
    val shimmer by rememberInfiniteTransition(label = "shimmer")
        .animateFloat(0.4f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")

    OutlinedButton(
        onClick = { if (!isLoading) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onExport() } },
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isLoading) appColors.divider else Violet500),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet500)
    ) {
        AnimatedContent(isLoading, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "btn") { loading ->
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Violet500.copy(shimmer))
                    Text((state as? ExportState.Loading)?.message ?: "جارٍ...",
                        color = appColors.textSubtle, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.FileDownload, null, Modifier.size(18.dp))
                    Text("${target.icon} تصدير ${target.label}",
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    // ── Sheet الإجراءات ───────────────────────────────────────
    if (showSheet && state is ExportState.Success) {
        val file = File(state.filePath)
        ExportSuccessSheet(
            state     = state,
            onShare   = { ExportManager.shareFile(context, file, state.type.mimeType); showSheet = false },
            onWhatsApp = { ExportManager.shareToWhatsApp(context, file, state.type.mimeType); showSheet = false },
            onDownload = {
                ExportManager.saveToDownloads(context, file, state.fileName)
                showSheet = false
            },
            onDismiss = { showSheet = false }
        )
    }

    // ── خطأ ───────────────────────────────────────────────────
    if (state is ExportState.Error) {
        ExportErrorBar(message = state.message, onRetry = onExport, onDismiss = onDismissError)
    }
}

// ── Bottom Sheet ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSuccessSheet(
    state: ExportState.Success,
    onShare: () -> Unit,
    onWhatsApp: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss = { scope.launch { sheetState.hide(); onDismiss() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = appColors.cardBackground,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 8.dp).size(40.dp, 4.dp)
                .clip(RoundedCornerShape(50)).background(appColors.divider))
        }
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // رأس
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Emerald500.copy(0.12f)), Alignment.Center) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Emerald500, modifier = Modifier.size(26.dp))
                }
                Column {
                    Text("جاهز للمشاركة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(state.fileName, style = MaterialTheme.typography.bodySmall, color = appColors.textSubtle)
                }
            }

            HorizontalDivider(color = appColors.divider)

            SheetAction(Icons.Rounded.Share,    Cyan500,              "مشاركة",   "عبر أي تطبيق",       onShare)
            SheetAction(Icons.Rounded.Chat,     Color(0xFF25D366),    "واتساب",   "إرسال مباشر",         onWhatsApp)
            SheetAction(Icons.Rounded.Download, Violet500,            "تنزيل",    "حفظ في التنزيلات",    onDownload)

            TextButton(onClick = { dismiss() }, Modifier.fillMaxWidth()) {
                Text("إغلاق", color = appColors.textSubtle)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        shape = RoundedCornerShape(14.dp), color = color.copy(0.06f), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(0.12f)), Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = appColors.textSubtle)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = appColors.textSubtle, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ExportErrorBar(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(16.dp), RoundedCornerShape(14.dp),
        color = DebtRed.copy(0.08f), border = BorderStroke(1.dp, DebtRed.copy(0.3f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.ErrorOutline, null, tint = DebtRed, modifier = Modifier.size(18.dp))
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = DebtRed)
            TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text("إعادة", color = DebtRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onDismiss, Modifier.size(28.dp)) {
                Icon(Icons.Rounded.Close, null, tint = DebtRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}
