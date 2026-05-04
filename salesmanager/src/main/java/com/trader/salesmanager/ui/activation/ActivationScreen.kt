package com.trader.salesmanager.ui.activation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.salesmanager.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun ActivationScreen(
    onActivated: () -> Unit,
    onFreeStart: () -> Unit,
    viewModel: ActivationViewModel = koinViewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    val startupState by viewModel.startupState.collectAsState()
    val context      = LocalContext.current

    // ── Navigation triggers ─────────────────────────────────────
    LaunchedEffect(uiState.isSuccess) { if (uiState.isSuccess) onActivated() }
    LaunchedEffect(startupState) {
        if (startupState == StartupState.ProceedFree) onFreeStart()
    }

    // ── Background animation ─────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val pulseScale by infiniteTransition.animateFloat(
        0.95f, 1.05f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Emerald900, Dark900, Emerald700))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Logo ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Emerald500, Cyan500))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.TrendingUp, null,
                    tint = Color.White, modifier = Modifier.size(52.dp))
            }

            Spacer(Modifier.height(24.dp))

            Text("مدير المبيعات",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("إدارة مبيعاتك بذكاء وسهولة",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(0.7f), textAlign = TextAlign.Center)

            Spacer(Modifier.height(36.dp))

            // ════════════════════════════════════════════════════
            // PRIMARY — تفعيل بكود
            // ════════════════════════════════════════════════════
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Label داخل الكارد
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                .background(Emerald500.copy(0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Stars, null,
                                tint = Emerald500.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        }
                        Text("لديك كود تفعيل؟",
                            color = Color.White, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium)
                    }

                    OutlinedTextField(
                        value = uiState.code,
                        onValueChange = viewModel::updateCode,
                        label = { Text("كود التفعيل", color = Color.White.copy(0.7f)) },
                        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = Emerald400) },
                        isError = uiState.error != null,
                        supportingText = uiState.error?.let { { Text(it, color = Color(0xFFF87171)) } },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.activate() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Emerald400,
                            unfocusedBorderColor = Color.White.copy(0.3f),
                            cursorColor = Emerald400,
                            disabledTextColor = Color.White.copy(0.5f),
                            disabledBorderColor = Color.White.copy(0.15f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = viewModel::activate,
                        enabled = !uiState.isLoading && uiState.code.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald500,
                            disabledContainerColor = Emerald700.copy(0.4f)
                        )
                    ) {
                        AnimatedContent(uiState.isLoading, label = "btn") { loading ->
                            if (loading) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Text("جاري التحقق...",
                                        color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Text("تفعيل التطبيق",
                                    color = Color.White, fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            // ════════════════════════════════════════════════════
            // DIVIDER — أو
            // ════════════════════════════════════════════════════
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(0.25f)
                )
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(0.12f)
                ) {
                    Text("أو",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = Color.White.copy(0.7f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(0.25f)
                )
            }

            // ════════════════════════════════════════════════════
            // SECONDARY — ابدأ مجاناً
            // ════════════════════════════════════════════════════
            Spacer(Modifier.height(24.dp))

            // فوائد النسخة المجانية
            FreeFeaturesList()

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.registerFree(context) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(0.3f)
                ),
                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                    brush = Brush.horizontalGradient(
                        if (!uiState.isLoading)
                            listOf(Emerald400, Cyan500)
                        else
                            listOf(Color.White.copy(0.2f), Color.White.copy(0.2f))
                    )
                )
            ) {
                AnimatedContent(uiState.isLoading, label = "freeBtn") { loading ->
                    if (loading) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                Modifier.size(18.dp), color = Emerald400, strokeWidth = 2.dp)
                            Text("جاري التسجيل...",
                                color = Emerald400, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.RocketLaunch, null,
                                modifier = Modifier.size(20.dp),
                                tint = Emerald400)
                            Text("ابدأ مجاناً الآن",
                                color = Emerald400,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "النسخة المجانية لا تحتاج إلى كود أو بطاقة ائتمانية",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(0.4f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── قائمة مميزات النسخة المجانية ─────────────────────────────────
@Composable
private fun FreeFeaturesList() {
    val benefits = listOf(
        Icons.Rounded.ReceiptLong to "عمليات بيع غير محدودة",
        Icons.Rounded.People      to "إدارة زبائن كاملة",
        Icons.Rounded.BarChart    to "تقارير أسبوعية",
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.07f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("ما يمكنك فعله مجاناً:",
                color = Color.White.copy(0.6f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 2.dp))

            benefits.forEach { (icon, text) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Emerald500.copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null,
                            tint = Emerald400,
                            modifier = Modifier.size(14.dp))
                    }
                    Text(text,
                        color = Color.White.copy(0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
