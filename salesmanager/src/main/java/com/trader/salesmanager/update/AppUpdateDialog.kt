package com.trader.salesmanager.update

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trader.salesmanager.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AppUpdateDialog(
    state: UpdateUiState,
    onStartDownload: (Context) -> Unit,
    onInstall: (Context) -> Unit,
    onRetry: (Context) -> Unit,
    onOpenPermission: (Context) -> Unit
) {
    val showDialog = state is UpdateUiState.UpdateAvailable
        || state is UpdateUiState.Downloading
        || state is UpdateUiState.ReadyToInstall
        || state is UpdateUiState.DownloadError
        || state is UpdateUiState.NeedInstallPermission

    if (!showDialog) return

    val context = LocalContext.current

    Dialog(
        onDismissRequest = { /* force — not dismissible */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            UpdateCard(
                state            = state,
                onStartDownload  = { onStartDownload(context) },
                onInstall        = { onInstall(context) },
                onRetry          = { onRetry(context) },
                onOpenPermission = { onOpenPermission(context) }
            )
        }
    }
}

@Composable
private fun UpdateCard(
    state: UpdateUiState,
    onStartDownload: () -> Unit,
    onInstall: () -> Unit,
    onRetry: () -> Unit,
    onOpenPermission: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAngle by infiniteTransition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "angle"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Gradient Header ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1A1F5E), Color(0xFF0D3B6E), Color(0xFF0A7EA4))
                        )
                    )
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Animated orbit rings
                Canvas(modifier = Modifier.size(160.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    for (i in 0..2) {
                        val radius = 50f + i * 22f
                        val alpha  = 0.15f - i * 0.04f
                        drawCircle(
                            color = Color(0xFF06B6D4).copy(alpha),
                            radius = radius,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.5f)
                        )
                    }
                    // Rotating dot
                    val dotAngleRad = Math.toRadians(glowAngle.toDouble())
                    val dotX = cx + 72f * cos(dotAngleRad).toFloat()
                    val dotY = cy + 72f * sin(dotAngleRad).toFloat()
                    drawCircle(color = Color(0xFF06B6D4), radius = 5f, center = Offset(dotX, dotY))

                    val dot2X = cx + 50f * cos(dotAngleRad + Math.PI).toFloat()
                    val dot2Y = cy + 50f * sin(dotAngleRad + Math.PI).toFloat()
                    drawCircle(color = Color(0xFF818CF8), radius = 3f, center = Offset(dot2X, dot2Y))
                }

                // Rocket / Update icon
                val scale by infiniteTransition.animateFloat(
                    0.9f, 1.1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "icon"
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // ── Content ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(targetState = state, label = "content",
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }
                ) { currentState ->
                    when (currentState) {

                        // ── Update Available ─────────────────────────
                        is UpdateUiState.UpdateAvailable -> {
                            UpdateAvailableContent(
                                info = currentState.info,
                                onDownload = onStartDownload
                            )
                        }

                        // ── Downloading ──────────────────────────────
                        is UpdateUiState.Downloading -> {
                            DownloadingContent(percent = currentState.percent)
                        }

                        // ── Ready to Install ─────────────────────────
                        is UpdateUiState.ReadyToInstall -> {
                            ReadyToInstallContent(onInstall = onInstall)
                        }

                        // ── Need Permission ──────────────────────────
                        is UpdateUiState.NeedInstallPermission -> {
                            NeedPermissionContent(onOpenSettings = onOpenPermission)
                        }

                        // ── Error ────────────────────────────────────
                        is UpdateUiState.DownloadError -> {
                            ErrorContent(
                                message = currentState.message,
                                onRetry = onRetry
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}

// ── Update Available ─────────────────────────────────────────────────
@Composable
private fun UpdateAvailableContent(info: AppUpdateInfo, onDownload: () -> Unit) {
    // طبقة حماية UI: نمنع الضغط المتعدد على مستوى الـ Composable أيضاً
    var isButtonClicked by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "تحديث جديد متاح! 🎉",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF06B6D4).copy(0.15f)) {
            Text(
                "الإصدار ${info.versionName}",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                color = Color(0xFF06B6D4),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (info.changelog.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null,
                            tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        Text("المميزات الجديدة",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    info.changelog.forEach { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF06B6D4))
                            )
                            Text(
                                feature,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Force update warning
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFEF4444).copy(0.1f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Warning, null,
                tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            Text(
                "هذا التحديث إجباري — يجب التحديث للمتابعة",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFEF4444)
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (!isButtonClicked) {
                    isButtonClicked = true
                    onDownload()
                }
            },
            enabled = !isButtonClicked,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            border = ButtonDefaults.outlinedButtonBorder(false).copy(
                brush = if (isButtonClicked)
                    Brush.horizontalGradient(listOf(Color(0xFF475569), Color(0xFF475569)))
                else
                    Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)))
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isButtonClicked)
                            Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF334155)))
                        else
                            Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isButtonClicked) {
                    // مؤشر تحميل بعد الضغط
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF06B6D4),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "جاري التحضير...",
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Download, null,
                            tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("تحميل التحديث الآن",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

// ── Downloading ──────────────────────────────────────────────────────
@Composable
private fun DownloadingContent(percent: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("جاري التحميل...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White)

        // Circular progress
        val animPercent by animateFloatAsState(
            percent / 100f,
            animationSpec = tween(300),
            label = "progress"
        )
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(100.dp)) {
                val strokeWidth = 8.dp.toPx()
                // Background circle
                drawArc(
                    color = Color(0xFF1E293B),
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth, size.height - strokeWidth
                    )
                )
                // Progress arc
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color(0xFF3B82F6), Color(0xFF06B6D4), Color(0xFF3B82F6))
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animPercent,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth, size.height - strokeWidth
                    )
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$percent%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp)
            }
        }

        // Linear progress bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animPercent)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF06B6D4))),
                            RoundedCornerShape(3.dp)
                        )
                )
            }
            Spacer(Modifier.height(6.dp))
            Text("$percent% مكتمل",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center)
        }

        Text("لا تغلق التطبيق أثناء التحميل",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center)
    }
}

// ── Ready to Install ─────────────────────────────────────────────────
@Composable
private fun ReadyToInstallContent(onInstall: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ready")
    val pulse by infiniteTransition.animateFloat(
        0.95f, 1.05f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CheckCircle, null,
                tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Text("اكتمل التحميل! ✅",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White)
        Text("اضغط للتثبيت الآن",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8))
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onInstall,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
        ) {
            Icon(Icons.Rounded.InstallMobile, null,
                tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("تثبيت التحديث",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ── Need Permission ──────────────────────────────────────────────────
@Composable
private fun NeedPermissionContent(onOpenSettings: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape)
                .background(Color(0xFFF59E0B).copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Security, null,
                tint = Color(0xFFF59E0B), modifier = Modifier.size(36.dp))
        }
        Text("مطلوب إذن التثبيت",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center)
        Text(
            "يحتاج التطبيق إذن تثبيت التطبيقات من مصادر غير معروفة للمتابعة",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
        ) {
            Icon(Icons.Rounded.OpenInNew, null,
                tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("فتح الإعدادات", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Error ────────────────────────────────────────────────────────────
@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape)
                .background(Color(0xFFEF4444).copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CloudOff, null,
                tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp))
        }
        Text("فشل التحميل",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White)
        Text(message,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
        ) {
            Icon(Icons.Rounded.Refresh, null,
                tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("إعادة المحاولة", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
