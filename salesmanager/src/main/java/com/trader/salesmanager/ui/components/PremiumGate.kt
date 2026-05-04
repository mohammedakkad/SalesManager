package com.trader.salesmanager.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.core.domain.model.FeatureFlags
import com.trader.salesmanager.ui.theme.*

// ── Gate composable — أساسي لكل شاشة مدفوعة ──────────────────────
@Composable
fun PremiumGate(
    feature: String,
    icon: ImageVector = Icons.Rounded.Stars,
    onUpgrade: () -> Unit,
    content: @Composable () -> Unit
) {
    val flags by FeatureFlags.flow.collectAsState()
    if (flags.isPremium) {
        content()
    } else {
        PremiumUpsellScreen(feature = feature, icon = icon, onUpgrade = onUpgrade)
    }
}

// ── Inline gate — للأزرار داخل الشاشة ────────────────────────────
@Composable
fun PremiumInlineGate(
    enabled: Boolean,
    feature: String,
    onUpgrade: () -> Unit,
    content: @Composable () -> Unit
) {
    if (enabled) {
        content()
    } else {
        PremiumLockChip(feature = feature, onUpgrade = onUpgrade)
    }
}

// ── شاشة كاملة (بدل الشاشة المقفولة) ────────────────────────────
@Composable
fun PremiumUpsellScreen(
    feature: String,
    icon: ImageVector,
    onUpgrade: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        0.5f, 1f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Animated icon ────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFFFD700).copy(glowAlpha * 0.3f),
                                Color(0xFFFFD700).copy(0.08f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }

            Text(
                "ميزة مدفوعة",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )
            Text(
                "«$feature» متاحة في الخطة المدفوعة فقط",
                style = MaterialTheme.typography.bodyMedium,
                color = appColors.textSecondary,
                textAlign = TextAlign.Center
            )

            // ── مميزات Premium ───────────────────────────────────
            PremiumBenefitsCard()

            // ── زر الترقية ───────────────────────────────────────
            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                            ),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Stars, null,
                            tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("ترقية إلى Premium",
                            color = Color.White, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // ── رابط خطط الاشتراك ────────────────────────────────
            TextButton(onClick = onUpgrade) {
                Text("عرض خطط الاشتراك",
                    color = Emerald500,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── مميزات Premium ────────────────────────────────────────────────
@Composable
private fun PremiumBenefitsCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                Icons.Rounded.Undo           to "نظام المرتجعات الكامل",
                Icons.Rounded.Inventory      to "جرد المخزون",
                Icons.Rounded.QueryStats     to "تقارير المخزون والأرباح",
                Icons.Rounded.QrCodeScanner  to "ماسح الباركود",
                Icons.Rounded.CalendarMonth  to "تقارير شهرية",
                Icons.Rounded.FileDownload   to "تصدير Excel/PDF",
                Icons.Rounded.SupportAgent   to "دعم فني مباشر"
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
                        Icon(icon, null,
                            tint = Color(0xFFFFA500),
                            modifier = Modifier.size(14.dp))
                    }
                    Text(text,
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.textPrimary,
                        fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── Chip صغير (للأزرار داخل شاشة) ────────────────────────────────
@Composable
fun PremiumLockChip(feature: String, onUpgrade: () -> Unit) {
    Surface(
        onClick = onUpgrade,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFD700).copy(0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Lock, null,
                tint = Color(0xFFFFA500),
                modifier = Modifier.size(12.dp))
            Text(feature,
                color = Color(0xFFFFA500),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold)
        }
    }
}
