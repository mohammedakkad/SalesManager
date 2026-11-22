package com.trader.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Widest content column; phones and portrait tablets (<= 840 dp) are unaffected. */
val MaxContentWidth: Dp = 840.dp

/**
 * Minimal wide-screen support: on landscape tablets, foldables and desktop windows the app is a
 * centred column at most [MaxContentWidth] wide, so lines and forms do not stretch edge to edge.
 * The caller's background still fills the whole window.
 */
@Composable
fun WideScreenContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = MaxContentWidth).fillMaxWidth().fillMaxHeight()) { content() }
    }
}
