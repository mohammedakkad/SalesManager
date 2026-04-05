package com.trader.admin.ui.merchants.detail

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.MerchantStatus
import com.trader.admin.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MerchantDetailScreen(
    merchantId: String, onNavigateUp: () -> Unit,
    viewModel: MerchantDetailViewModel = koinViewModel(parameters = { parametersOf(merchantId) })
) {
    val merchant by viewModel.merchant.collectAsState()
    var showDelete by remember { mutableStateOf(false) }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            icon = { Icon(Icons.Rounded.DeleteForever, null, tint = DisabledRose) },
            title = { Text("حذف البائع", fontWeight = FontWeight.Bold) },
            text = { Text("سيتم حذف البائع ${merchant?.name} نهائياً.") },
            containerColor = Navy900,
            confirmButton = { Button(onClick = { viewModel.delete { onNavigateUp() } },
                colors = ButtonDefaults.buttonColors(containerColor = DisabledRose)) { Text("حذف") } },
            dismissButton = { OutlinedButton(onClick = { showDelete = false }) { Text("إلغاء") } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(containerColor = Navy950) { padding ->
        merchant?.let { m ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Box(modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Indigo500, Violet500)))
                    .padding(top = 48.dp, bottom = 28.dp, start = 16.dp, end = 16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateUp) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { showDelete = true }) { Icon(Icons.Rounded.Delete, null, tint = Color.White.copy(0.8f)) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(0.2f)), contentAlignment = Alignment.Center) {
                                Text(m.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(m.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text(m.phone, color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailCard("كود التفعيل", m.activationCode, Icons.Rounded.Key, Indigo400)
                    DetailCard("الحالة", when(m.status) { MerchantStatus.ACTIVE -> "نشط"; MerchantStatus.EXPIRED -> "منتهي الصلاحية"; MerchantStatus.DISABLED -> "معطل" }, Icons.Rounded.Circle,
                        when(m.status) { MerchantStatus.ACTIVE -> ActiveGreen; MerchantStatus.EXPIRED -> ExpiredAmber; MerchantStatus.DISABLED -> DisabledRose })
                    DetailCard("نوع الاشتراك", if (m.isPermanent) "دائم" else "مؤقت", Icons.Rounded.CalendarToday, Cyan500)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (m.status != MerchantStatus.ACTIVE) {
                            Button(onClick = { viewModel.setStatus(MerchantStatus.ACTIVE) }, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = ActiveGreen)
                            ) { Text("تفعيل") }
                        }
                        if (m.status != MerchantStatus.DISABLED) {
                            OutlinedButton(onClick = { viewModel.setStatus(MerchantStatus.DISABLED) }, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp), border = ButtonDefaults.outlinedButtonBorder(true).copy(brush = androidx.compose.ui.graphics.SolidColor(DisabledRose))
                            ) { Text("تعطيل", color = DisabledRose) }
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Indigo500) }
    }
}

@Composable
private fun DetailCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Navy900), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Slate400)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Slate100)
            }
        }
    }
}
