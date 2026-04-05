package com.trader.admin.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.trader.admin.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardScreen(
    onNavigateToMerchants: () -> Unit,
    onNavigateToChat: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel()
) {
    val stats by viewModel.stats.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(Navy950).verticalScroll(rememberScrollState())
    ) {
        // ── Header ─────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Indigo500, Violet500)))
                .padding(top = 52.dp, bottom = 80.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Admin Panel", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("لوحة إدارة البائعين", color = Color.White.copy(0.8f), style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.15f))
                ) { Icon(Icons.Rounded.Logout, null, tint = Color.White) }
            }
        }

        Column(modifier = Modifier.offset(y = (-56).dp).padding(horizontal = 16.dp)) {
            // ── Stats Grid ──────────────────────────────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("إحصائيات البائعين", style = MaterialTheme.typography.titleMedium, color = Slate300)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatPill(Modifier.weight(1f), "الكل", stats.total, Indigo400)
                        StatPill(Modifier.weight(1f), "نشط", stats.active, ActiveGreen)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatPill(Modifier.weight(1f), "منتهي", stats.expired, ExpiredAmber)
                        StatPill(Modifier.weight(1f), "معطل", stats.disabled, DisabledRose)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Quick Actions ────────────────────────────────────
            Text("الإجراءات السريعة", style = MaterialTheme.typography.titleMedium, color = Slate300, modifier = Modifier.padding(bottom = 12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionCard(
                    icon = Icons.Rounded.People, title = "إدارة البائعين",
                    subtitle = "${stats.total} بائع مسجل", color = Indigo500,
                    onClick = onNavigateToMerchants
                )
                ActionCard(
                    icon = Icons.Rounded.Forum, title = "الدردشة والدعم",
                    subtitle = "تواصل مع البائعين", color = Cyan500,
                    onClick = onNavigateToChat
                )
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StatPill(modifier: Modifier, label: String, value: Int, color: Color) {
    val animatedValue by animateIntAsState(value, animationSpec = spring(Spring.DampingRatioMediumBouncy), label = "stat")
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = color.copy(0.1f)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Spacer(Modifier.height(4.dp))
            Text("$animatedValue", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun ActionCard(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        onClick = onClick, colors = CardDefaults.cardColors(containerColor = Navy900),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = color, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Slate100)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Slate400)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Slate600)
        }
    }
}
