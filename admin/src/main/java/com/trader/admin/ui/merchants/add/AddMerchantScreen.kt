package com.trader.admin.ui.merchants.add

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.admin.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddMerchantScreen(
    onNavigateUp: () -> Unit,
    viewModel: AddMerchantViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current

    // عند الحفظ الناجح اعرض الكود ثم ارجع
    var showCodeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved && state.generatedCode != null) {
            showCodeDialog = true
        }
    }

    // Dialog عرض رمز التفعيل
    if (showCodeDialog && state.generatedCode != null) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Navy900,
            title = {
                Text("تم إضافة البائع ✓",
                    color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("رمز التفعيل الخاص بالبائع:",
                        color = Slate400, style = MaterialTheme.typography.bodyMedium)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Navy800)
                    ) {
                        Text(
                            text = state.generatedCode!!,
                            color = Indigo300,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            letterSpacing = 4.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                    }
                    Text("احتفظ بهذا الرمز وأعطه للبائع",
                        color = Slate400, style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(state.generatedCode!!))
                    }) {
                        Icon(Icons.Rounded.ContentCopy, null, tint = Indigo400,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("نسخ", color = Indigo400)
                    }
                    Button(
                        onClick = { showCodeDialog = false; onNavigateUp() },
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تم", color = Color.White)
                    }
                }
            }
        )
    }

    Scaffold(containerColor = Navy950) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Indigo500, Violet500)))
                    .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("إضافة بائع جديد",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // الاسم
                AdminTextField(
                    value = state.name,
                    onValueChange = viewModel::updateName,
                    label = "اسم البائع",
                    icon = Icons.Rounded.Person,
                    supportingText = if (state.name.isNotEmpty() && state.name.length < 2)
                        "حرفان على الأقل" else null
                )

                // الهاتف
                AdminTextField(
                    value = state.phone,
                    onValueChange = { if (it.length <= 10) viewModel.updatePhone(it) },
                    label = "رقم الهاتف (10 أرقام)",
                    icon = Icons.Rounded.Phone,
                    keyboardType = KeyboardType.Phone,
                    supportingText = if (state.phone.isNotEmpty() && state.phone.length != 10)
                        "${state.phone.length}/10 أرقام" else null
                )

                // مدة التفعيل
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy900)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("مدة التفعيل",
                            style = MaterialTheme.typography.labelLarge, color = Slate300)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DurationChip("دائمة", state.isPermanent) {
                                viewModel.updatePermanent(true)
                            }
                            DurationChip("مؤقتة", !state.isPermanent) {
                                viewModel.updatePermanent(false)
                            }
                        }

                        AnimatedVisibility(!state.isPermanent) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                AdminTextField(
                                    value = state.durationDays,
                                    onValueChange = viewModel::updateDuration,
                                    label = "عدد الأيام",
                                    icon = Icons.Rounded.CalendarToday,
                                    keyboardType = KeyboardType.Number
                                )
                                Spacer(Modifier.height(8.dp))

                                // عداد تنازلي للمؤقت
                                val days = state.durationDays.toIntOrNull() ?: 0
                                if (days > 0) {
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Indigo500.copy(alpha = 0.15f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Rounded.Timer, null,
                                                tint = Indigo400, modifier = Modifier.size(18.dp))
                                            Text("ينتهي بعد $days يوم",
                                                color = Indigo300,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // رسالة خطأ
                AnimatedVisibility(state.error != null) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Error, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp))
                            Text(state.error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // زر الحفظ
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                ) {
                    AnimatedContent(state.isLoading, label = "save") { loading ->
                        if (loading)
                            CircularProgressIndicator(Modifier.size(20.dp),
                                color = Color.White, strokeWidth = 2.dp)
                        else
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.PersonAdd, null,
                                    tint = Color.White, modifier = Modifier.size(20.dp))
                                Text("حفظ البائع",
                                    fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Slate400) },
        leadingIcon = { Icon(icon, null, tint = Indigo400) },
        supportingText = supportingText?.let {
            { Text(it, color = Slate400, style = MaterialTheme.typography.labelSmall) }
        },
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Indigo500,
            unfocusedBorderColor = Slate700,
            focusedTextColor = Slate100,
            unfocusedTextColor = Slate100,
            focusedContainerColor = Navy900,
            unfocusedContainerColor = Navy900
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun DurationChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Indigo500, selectedLabelColor = Color.White,
            containerColor = Slate800, labelColor = Slate300
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = Slate700, selectedBorderColor = Indigo500
        )
    )
}