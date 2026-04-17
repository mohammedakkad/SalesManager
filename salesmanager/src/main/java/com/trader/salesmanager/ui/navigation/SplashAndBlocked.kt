package com.trader.salesmanager.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors

// ── Splash / Checking Screen ──────────────────────────────────────────
@Composable
fun SplashCheckScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val pulse by infiniteTransition.animateFloat(
        0.9f, 1.1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Emerald700, Cyan500))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.TrendingUp, null,
                    tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("مدير المبيعات",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(
                color = Color.White.copy(0.8f),
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp
            )
            Spacer(Modifier.height(8.dp))
            Text("جاري التحقق من الحساب...",
                color = Color.White.copy(0.7f),
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ── Blocked Screen ────────────────────────────────────────────────────
@Composable
fun BlockedScreen(message: String, canRetry: Boolean, onRetry: () -> Unit) {
    val lines = message.split("\n")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF1A2C5B), Color(0xFF0F172A)))),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = appColors.textPrimary),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(DebtRed.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Lock, null,
                        tint = DebtRed, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text("الوصول محظور",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(12.dp))
                lines.forEach { line ->
                    Text(line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = appColors.textSubtle,
                        textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(28.dp))
                // Contact admin hint
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.SupportAgent, null,
                            tint = Cyan500, modifier = Modifier.size(20.dp))
                        Text("تواصل مع الإدارة عبر تطبيق الدعم",
                            style = MaterialTheme.typography.bodySmall,
                            color = appColors.textSubtle)
                    }
                }
                if (canRetry) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إعادة المحاولة")
                    }
                }
            }
        }
    }
}