package com.trader.salesmanager.ui.employees

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.salesmanager.ui.theme.Cyan500
import com.trader.salesmanager.ui.theme.DebtRed
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.Violet500
import com.trader.salesmanager.ui.theme.appColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.cos
import kotlin.math.sin

// ════════════════════════════════════════════════════════════════════
//  PIN LOCK SCREEN — "2026 Premium" Look
//  Animated mesh-gradient background, fluid PIN dots with spring +
//  shake-on-error physics, oversized custom numpad with staggered
//  entrance, scale-on-press, corner-morph and haptic feedback.
// ════════════════════════════════════════════════════════════════════

private const val MESH_PERIOD_MS = 18_000          // 18 s mesh loop
private const val SHAKE_DISTANCE = 28f             // px
private const val NUMPAD_STAGGER_MS = 45L          // per-key delay
private const val NUMPAD_KEYS = 12                 // 0..9 + empty + backspace

@Composable
fun PinLockScreen(
    onUnlocked: (isAdmin: Boolean, employeeName: String) -> Unit,
    viewModel: PinLockViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Horizontal shake driven from a single Animatable so the keyframes
    // can express a real physical decay (right → left → right → settle).
    val shakeOffset = remember { Animatable(0f) }

    // One-shot events from the VM — drive haptics, navigation, shake.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PinLockEvent.Success -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUnlocked(event.isAdmin, event.employeeName)
                }
                PinLockEvent.WrongPin -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch { runShake(shakeOffset) }
                    delay(650)
                    viewModel.onClear()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .meshGradientBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Header ────────────────────────────────────────
            HeaderBlock()

            // ── PIN dots ──────────────────────────────────────
            PinDotsRow(
                length = state.pin.length,
                total = state.maxLen,
                hasError = state.hasError,
                shakeOffset = shakeOffset.value,
                isLoading = state.loading
            )

            // ── Numpad ────────────────────────────────────────
            NumPad(
                enabled = !state.loading,
                onDigit = { digit ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onDigit(digit)
                },
                onBackspace = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onBackspace()
                }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  HEADER — Lock badge + greeting
// ════════════════════════════════════════════════════════════════════
@Composable
private fun HeaderBlock() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Emerald500.copy(alpha = 0.95f),
                            Cyan500.copy(alpha = 0.95f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = "أدخل رمز الدخول",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Text(
            text = "أدخل رمز PIN المكوّن من 4 أرقام للمتابعة",
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}

// ════════════════════════════════════════════════════════════════════
//  PIN DOTS — spring fill + shake on error
// ════════════════════════════════════════════════════════════════════
@Composable
private fun PinDotsRow(
    length: Int,
    total: Int,
    hasError: Boolean,
    shakeOffset: Float,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .graphicsLayer { translationX = shakeOffset },
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            PinDot(
                filled = index < length,
                hasError = hasError
            )
        }
        if (isLoading) {
            Spacer(Modifier.width(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PinDot(filled: Boolean, hasError: Boolean) {
    // Fluid spring to express "fill" → grow then settle.
    val scale by animateFloatAsState(
        targetValue = if (filled) 1f else 0.55f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dot-scale"
    )
    val color by animateFloatAsState(
        targetValue = if (filled) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "dot-color"
    )

    val baseColor = if (hasError) DebtRed else Emerald500
    val outline = Color.White.copy(alpha = 0.55f)

    Box(
        modifier = Modifier
            .size(22.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                color = if (filled) baseColor else Color.Transparent
            )
            .drawBehind {
                if (!filled) {
                    drawCircle(
                        color = outline,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )
                }
            }
    ) {
        // Subtle inner glow when filled.
        if (filled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = color * 0.35f }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White, Color.Transparent),
                            radius = 22f
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  NUM PAD — staggered entrance + scale/corner morph on press
// ════════════════════════════════════════════════════════════════════
@Composable
private fun NumPad(
    enabled: Boolean,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit
) {
    // Trigger the staggered entrance once on first composition.
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(120)
        animateIn = true
    }

    val rows = listOf(
        listOf(NumKey.Digit(1), NumKey.Digit(2), NumKey.Digit(3)),
        listOf(NumKey.Digit(4), NumKey.Digit(5), NumKey.Digit(6)),
        listOf(NumKey.Digit(7), NumKey.Digit(8), NumKey.Digit(9)),
        listOf(NumKey.Empty, NumKey.Digit(0), NumKey.Backspace)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                row.forEachIndexed { colIndex, key ->
                    val flatIndex = rowIndex * 3 + colIndex
                    Box(modifier = Modifier.weight(1f)) {
                        when (key) {
                            is NumKey.Digit -> NumPadButton(
                                index = flatIndex,
                                animateIn = animateIn,
                                enabled = enabled,
                                onClick = { onDigit(key.value) }
                            ) {
                                Text(
                                    text = key.value.toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 30.sp
                                )
                            }
                            NumKey.Backspace -> NumPadButton(
                                index = flatIndex,
                                animateIn = animateIn,
                                enabled = enabled,
                                onClick = onBackspace
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                    contentDescription = "حذف",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            NumKey.Empty -> Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumPadButton(
    index: Int,
    animateIn: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Pressed state: scale down + tighten corner radius.
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "press-scale"
    )
    val corner by animateFloatAsState(
        targetValue = if (isPressed) 30f else 22f,
        animationSpec = tween(180),
        label = "press-corner"
    )

    // Per-key staggered entrance: alpha + slide up.
    val entry by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(
            durationMillis = 380,
            delayMillis = (index * NUMPAD_STAGGER_MS).toInt(),
            easing = EaseInOutCubic
        ),
        label = "key-entry-$index"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = entry
                translationY = (1f - entry) * 40f
            }
            .clip(RoundedCornerShape(corner.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.07f)
                    )
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private sealed interface NumKey {
    data class Digit(val value: Int) : NumKey
    object Backspace : NumKey
    object Empty : NumKey
}

// ════════════════════════════════════════════════════════════════════
//  ANIMATED MESH GRADIENT BACKGROUND
//  Slow, looping radial blobs over a deep base. No external libs.
// ════════════════════════════════════════════════════════════════════
@Composable
private fun Modifier.meshGradientBackground(): Modifier {
    val transition = rememberInfiniteTransition(label = "mesh")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MESH_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mesh-phase"
    )

    val isDark = appColors.isDark
    val base = if (isDark) Color(0xFF050B14) else Color(0xFF0F1923)

    return this.drawBehind {
        // 1) Deep solid base → guarantees contrast for the white glyphs.
        drawRect(base)

        val w = size.width
        val h = size.height

        // 2) Three slow-moving "blobs" — phases offset so they cross paths.
        val blobs = listOf(
            BlobSpec(Emerald500, 0f),
            BlobSpec(Cyan500, 0.33f),
            BlobSpec(Violet500, 0.66f)
        )

        blobs.forEach { blob ->
            val angle: Double = (phase + blob.phaseOffset).toDouble() * 2.0 * Math.PI
            val cx = w * (0.5f + 0.32f * cos(angle).toFloat())
            val cy = h * (0.5f + 0.35f * sin(angle * 1.3).toFloat())
            val radius = w * 0.85f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        blob.color.copy(alpha = 0.55f),
                        blob.color.copy(alpha = 0f)
                    ),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
        }

        // 3) Soft top-to-bottom dim to keep CTA legible.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    base.copy(alpha = 0.05f),
                    base.copy(alpha = 0.55f)
                )
            )
        )
    }
}

private data class BlobSpec(val color: Color, val phaseOffset: Float)

// ════════════════════════════════════════════════════════════════════
//  SHAKE PHYSICS — keyframed left/right decay driven by Animatable
// ════════════════════════════════════════════════════════════════════
private suspend fun runShake(animatable: Animatable<Float, *>) {
    val keyframes = listOf(
        -SHAKE_DISTANCE to 60,
        SHAKE_DISTANCE to 60,
        -SHAKE_DISTANCE * 0.7f to 60,
        SHAKE_DISTANCE * 0.7f to 60,
        -SHAKE_DISTANCE * 0.4f to 60,
        SHAKE_DISTANCE * 0.4f to 60,
        0f to 60
    )
    for ((target, duration) in keyframes) {
        animatable.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = duration, easing = LinearEasing)
        )
    }
}
