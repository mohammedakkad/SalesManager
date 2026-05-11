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

// ── Expired Subscription Screen ───────────────────────────────────────────
// Shown when StartupStatus.EXPIRED or DELETED — the merchant's subscription
// lapsed but the account was never admin-blocked.  Offers a clear upgrade CTA
// instead of a dead-end "blocked" message.
@Composable
fun ExpiredSubscriptionScreen(onRenew: () -> Unit, onRetry: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "expGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "expGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1A2035)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Animated gold badge
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFFFD700).copy(glowAlpha * 0.35f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFD700), Color(0xFFF59E0B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.WorkspacePremium, null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                "انتهى اشتراكك",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "جدّد اشتراكك للاستمرار في استخدام جميع المميزات المتميزة",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )

            // Features reminder
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.06f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Icons.Rounded.Inventory     to "إدارة المخزون الكاملة",
                        Icons.Rounded.QueryStats    to "تقارير المبيعات والأرباح",
                        Icons.Rounded.FileDownload  to "تصدير Excel/PDF",
                        Icons.Rounded.Undo          to "نظام المرتجعات",
                        Icons.Rounded.CalendarMonth to "تقارير شهرية"
                    ).forEach { (icon, text) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFD700).copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon, null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Renew CTA
            Button(
                onClick = onRenew,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFFD700), Color(0xFFF59E0B))
                            ),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Stars, null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "تجديد الاشتراك",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(0.7f)
                )
            ) {
                Icon(
                    Icons.Rounded.Refresh, null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("إعادة التحقق")
            }
        }
    }
}