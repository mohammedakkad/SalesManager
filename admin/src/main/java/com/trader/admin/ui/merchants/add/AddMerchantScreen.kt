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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trader.admin.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddMerchantScreen(onNavigateUp: () -> Unit, viewModel: AddMerchantViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onNavigateUp() }

    Scaffold(containerColor = Navy950) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Indigo500, Violet500)))
                .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateUp) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
                    Spacer(Modifier.width(8.dp))
                    Text("إضافة بائع جديد", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Name
                AdminTextField(value = state.name, onValueChange = viewModel::updateName,
                    label = "اسم البائع", icon = Icons.Rounded.Person)
                // Phone
                AdminTextField(value = state.phone, onValueChange = viewModel::updatePhone,
                    label = "رقم الهاتف", icon = Icons.Rounded.Phone,
                    keyboardType = KeyboardType.Phone)

                // Duration
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Navy900)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("مدة التفعيل", style = MaterialTheme.typography.labelLarge, color = Slate300)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DurationChip("دائمة", state.isPermanent) { viewModel.updatePermanent(true) }
                            DurationChip("مؤقتة", !state.isPermanent) { viewModel.updatePermanent(false) }
                        }
                        AnimatedVisibility(!state.isPermanent) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                AdminTextField(value = state.durationDays, onValueChange = viewModel::updateDuration,
                                    label = "عدد الأيام", icon = Icons.Rounded.CalendarToday, keyboardType = KeyboardType.Number)
                            }
                        }
                    }
                }

                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                Button(onClick = viewModel::save, enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                ) {
                    AnimatedContent(state.isLoading, label = "save") { loading ->
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("حفظ البائع", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTextField(value: String, onValueChange: (String) -> Unit, label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label, color = Slate400) },
        leadingIcon = { Icon(icon, null, tint = Indigo400) },
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Indigo500, unfocusedBorderColor = Slate700,
            focusedTextColor = Slate100, unfocusedTextColor = Slate100,
            focusedContainerColor = Navy900, unfocusedContainerColor = Navy900
        ),
        modifier = Modifier.fillMaxWidth(), singleLine = true
    )
}

@Composable
private fun DurationChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Indigo500, selectedLabelColor = Color.White,
            containerColor = Slate800, labelColor = Slate300
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected,
            borderColor = Slate700, selectedBorderColor = Indigo500)
    )
}
