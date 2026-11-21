package com.trader.core.util

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import kotlinx.coroutines.launch

/**
 * Copies plain text through the suspending [LocalClipboard] API (replaces the
 * deprecated LocalClipboardManager.setText). The label matches the previous
 * Compose implementation so clipboard contents stay identical.
 */
@Composable
fun rememberPlainTextCopier(): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, scope) {
        { text ->
            scope.launch {
                clipboard.setClipEntry(ClipData.newPlainText("plain text", text).toClipEntry())
            }
        }
    }
}
