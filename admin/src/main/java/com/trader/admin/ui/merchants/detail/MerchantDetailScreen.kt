package com.trader.admin.ui.merchants.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.model.MerchantStatus
import com.trader.admin.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun MerchantDetailScreen(
    merchantId: String, onNavigateUp: () -> Unit,
    viewModel: MerchantDetailViewModel = koinViewModel(parameters = { parametersOf(merchantId) })
) {
    val merchant   by viewModel.merchant.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    val context    = LocalContext.current

    var showDelete         by remember { mutableStateOf(false) }
    var showAdjustExpiry   by remember { mutableStateOf(false) }
    var copiedSnack        by remember { mutableStateOf(false) }

    // ── Snackbar for copy feedback ────────────────────────────────
    LaunchedEffect(copiedSnack) {
        if (copiedSnack) {
            kotlinx.coroutines.delay(2000)
            copiedSnack = false
        }
    }

    // ── Delete dialog ─────────────────────────────────────────────
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            icon = { Icon(Icons.Rounded.DeleteForever, null, tint = DisabledRose) },
            title = { Text("حذف البائع", fontWeight = FontWeight.Bold) },
            text = { Text("سيتم حذف البائع ${merchant?.name} نهائياً وإلغاء كود التفعيل.") },
            containerColor = Navy900,
            confirmButton = {
                Button(
                    onClick = { viewModel.delete { onNavigateUp() } },
                    colors = ButtonDefaults.buttonColors(containerColor = DisabledRose)
                ) { Text("حذف") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDelete = false }) { Text("إلغاء") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Adjust expiry dialog ──────────────────────────────────────
    if (showAdjustExpiry) {
        AdjustExpiryDialog(
            merchant = merchant,
            onAdjust = { days ->
                viewModel.adjustExpiry(days)
                showAdjustExpiry = false
            },
            onDismiss = { showAdjustExpiry = false }
        )
    }

    Scaffold(containerColor = Navy950) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            merchant?.let { m ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Header ────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Indigo500, Violet500)))
                            .padding(top = 48.dp, bottom = 28.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onNavigateUp) {
                                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { showDelete = true }) {
                                    Icon(Icons.Rounded.Delete, null, tint = Color.White.copy(0.8f))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        m.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(m.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    Text(m.phone, color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // ── Content ───────────────────────────────────────
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        // Activation code card (with copy button)
                        ActivationCodeCard(
                            code = m.activationCode,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("activation_code", m.activationCode))
                                copiedSnack = true
                            }
                        )

                        // Status card
                        DetailCard(
                            label = "الحالة",
                            value = when (m.status) {
                                MerchantStatus.ACTIVE   -> "نشط ✅"
                                MerchantStatus.EXPIRED  -> "منتهي الصلاحية ⚠️"
                                MerchantStatus.DISABLED -> "معطّل 🚫"
                            },
                            icon  = Icons.Rounded.Circle,
                            color = when (m.status) {
                                MerchantStatus.ACTIVE   -> ActiveGreen
                                MerchantStatus.EXPIRED  -> ExpiredAmber
                                MerchantStatus.DISABLED -> DisabledRose
                            }
                        )

                        // Subscription type + remaining time
                        if (m.isPermanent) {
                            DetailCard("نوع الاشتراك", "دائم ♾️", Icons.Rounded.AllInclusive, Cyan500)
                        } else {
                            ExpiryCard(merchant = m, onAdjust = { showAdjustExpiry = true })
                        }

                        Spacer(Modifier.height(4.dp))

                        // ── Action buttons ────────────────────────────
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (m.status != MerchantStatus.ACTIVE) {
                                Button(
                                    onClick = { viewModel.setStatus(MerchantStatus.ACTIVE) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ActiveGreen),
                                    enabled = !isLoading
                                ) { Text("تفعيل") }
                            }
                            if (m.status != MerchantStatus.DISABLED) {
                                OutlinedButton(
                                    onClick = { viewModel.setStatus(MerchantStatus.DISABLED) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(DisabledRose)
                                    ),
                                    enabled = !isLoading
                                ) { Text("تعطيل", color = DisabledRose) }
                            }
                        }
                    }
                }
            } ?: Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Indigo500)
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Indigo400) }
            }

            // Copy snackbar
            AnimatedVisibility(
                visible = copiedSnack,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                enter = slideInVertically { it } + fadeIn(),
                exit  = slideOutVertically { it } + fadeOut()
            ) {
                Surface(
                    shape  = RoundedCornerShape(24.dp),
                    color  = Indigo500,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("تم نسخ الكود", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// ── Activation code card with copy ───────────────────────────────────────────
@Composable
private fun ActivationCodeCard(code: String, onCopy: () -> Unit) {
    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Navy900),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Indigo400.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Key, null, tint = Indigo400, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("كود التفعيل", style = MaterialTheme.typography.labelSmall, color = Slate400)
                Text(code, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold,
                    color = Slate100, letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp))
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Rounded.ContentCopy, null, tint = Indigo400, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Expiry card with time remaining + adjust button ───────────────────────────
@Composable
private fun ExpiryCard(merchant: Merchant, onAdjust: () -> Unit) {
    val expiryDate  = merchant.expiryDate?.toDate()
    val now         = Date()
    val diffMs      = (expiryDate?.time ?: 0L) - now.time
    val daysLeft    = TimeUnit.MILLISECONDS.toDays(diffMs)
    val isExpired   = diffMs <= 0

    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Navy900),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background((if (isExpired) ExpiredAmber else Cyan500).copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CalendarToday, null,
                        tint = if (isExpired) ExpiredAmber else Cyan500,
                        modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("اشتراك مؤقت", style = MaterialTheme.typography.labelSmall, color = Slate400)
                    if (expiryDate != null) {
                        val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        Text("ينتهي ${fmt.format(expiryDate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate100)
                    }
                }
                IconButton(onClick = onAdjust) {
                    Icon(Icons.Rounded.EditCalendar, null, tint = Cyan500, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Slate700)
            Spacer(Modifier.height(12.dp))

            // Remaining time chip
            val chipColor = when {
                isExpired    -> DisabledRose
                daysLeft <= 7 -> ExpiredAmber
                else         -> ActiveGreen
            }
            val remainingText = when {
                isExpired    -> "منتهي منذ ${abs(daysLeft)} يوم"
                daysLeft == 0L -> "ينتهي اليوم!"
                daysLeft == 1L -> "يوم واحد متبقي"
                daysLeft <= 30 -> "متبقي $daysLeft يوم"
                else -> {
                    val months = daysLeft / 30
                    val rem    = daysLeft % 30
                    if (rem == 0L) "متبقي $months شهر" else "متبقي $months شهر و $rem يوم"
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isExpired) Icons.Rounded.Timer else Icons.Rounded.Timelapse,
                    null, tint = chipColor, modifier = Modifier.size(18.dp)
                )
                Text(remainingText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = chipColor)
            }
        }
    }
}

// ── Adjust expiry dialog ──────────────────────────────────────────────────────
@Composable
private fun AdjustExpiryDialog(
    merchant: Merchant?,
    onAdjust: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var customDays by remember { mutableStateOf("") }
    var isExtend   by remember { mutableStateOf(true) }

    val presets = listOf(7, 14, 30, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Navy900,
        shape = RoundedCornerShape(20.dp),
        icon = { Icon(Icons.Rounded.EditCalendar, null, tint = Cyan500) },
        title = {
            Text("تعديل مدة الاشتراك", fontWeight = FontWeight.Bold, color = Slate100)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Extend / Reduce toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isExtend,
                        onClick  = { isExtend = true },
                        label    = { Text("تمديد ➕") },
                        modifier = Modifier.weight(1f),
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ActiveGreen.copy(0.2f),
                            selectedLabelColor     = ActiveGreen
                        )
                    )
                    FilterChip(
                        selected = !isExtend,
                        onClick  = { isExtend = false },
                        label    = { Text("تقليص ➖") },
                        modifier = Modifier.weight(1f),
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DisabledRose.copy(0.2f),
                            selectedLabelColor     = DisabledRose
                        )
                    )
                }

                Text("اختر عدد الأيام:", style = MaterialTheme.typography.bodySmall, color = Slate400)

                // Preset buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { days ->
                        OutlinedButton(
                            onClick = { customDays = days.toString() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (customDays == days.toString()) Cyan500 else Slate400
                            ),
                            border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (customDays == days.toString()) Cyan500 else Slate600
                                )
                            )
                        ) { Text("$days", style = MaterialTheme.typography.bodySmall) }
                    }
                }

                // Custom input
                OutlinedTextField(
                    value = customDays,
                    onValueChange = { if (it.all(Char::isDigit)) customDays = it },
                    label = { Text("أو أدخل عدد يوم مخصص", color = Slate400) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor   = Slate100,
                        unfocusedTextColor = Slate100,
                        focusedBorderColor = Cyan500,
                        unfocusedBorderColor = Slate600
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = customDays.toIntOrNull() ?: return@Button
                    onAdjust(if (isExtend) days else -days)
                },
                enabled = customDays.toIntOrNull() != null && customDays.toIntOrNull()!! > 0,
                colors  = ButtonDefaults.buttonColors(containerColor = Cyan500)
            ) { Text("تطبيق") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء", color = Slate400) }
        }
    )
}

// ── Generic detail card ───────────────────────────────────────────────────────
@Composable
private fun DetailCard(
    label: String, value: String,
    icon: ImageVector, color: Color
) {
    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Navy900),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(color.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Slate400)
                Text(value, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, color = Slate100)
            }
        }
    }
}
