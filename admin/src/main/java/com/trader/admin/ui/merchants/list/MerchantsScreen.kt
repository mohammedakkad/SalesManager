package com.trader.admin.ui.merchants.list

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.model.MerchantStatus
import com.trader.admin.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun MerchantsScreen(
    onNavigateUp: () -> Unit,
    onMerchantClick: (String) -> Unit,
    onAddMerchant: () -> Unit,
    viewModel: MerchantsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMerchant, containerColor = Indigo500, contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Rounded.PersonAdd, null) }
        },
        containerColor = Navy950
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Indigo500, Violet500)))
                .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateUp) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
                        Column {
                            Text("البائعون", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${state.merchants.size} بائع", color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.search, onValueChange = viewModel::setSearch,
                        placeholder = { Text("بحث بالاسم أو الرقم...", color = Color.White.copy(0.5f)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.7f)) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(0.5f), unfocusedBorderColor = Color.White.copy(0.2f),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(0.1f), unfocusedContainerColor = Color.White.copy(0.07f)
                        ),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to "الكل", MerchantStatus.ACTIVE to "نشط",
                            MerchantStatus.EXPIRED to "منتهي", MerchantStatus.DISABLED to "معطل"
                        ).forEach { (status, label) ->
                            val sel = state.filter == status
                            FilterChip(selected = sel, onClick = { viewModel.setFilter(status) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.White, selectedLabelColor = Indigo500,
                                    containerColor = Color.White.copy(0.15f), labelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = sel,
                                    borderColor = Color.White.copy(0.2f), selectedBorderColor = Color.Transparent)
                            )
                        }
                    }
                }
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(state.merchants, key = { _, m -> m.id }) { index, merchant ->
                    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                    AnimatedVisibility(visible,
                        enter = slideInVertically(animationSpec = tween(300, index * 40)) + fadeIn()
                    ) {
                        MerchantCard(merchant = merchant, onClick = { onMerchantClick(merchant.id) },
                            onToggle = { viewModel.setStatus(merchant.id, if (merchant.status == MerchantStatus.ACTIVE) MerchantStatus.DISABLED else MerchantStatus.ACTIVE) })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MerchantCard(merchant: Merchant, onClick: () -> Unit, onToggle: () -> Unit) {
    val statusColor = when (merchant.status) {
        MerchantStatus.ACTIVE   -> ActiveGreen
        MerchantStatus.EXPIRED  -> ExpiredAmber
        MerchantStatus.DISABLED -> DisabledRose
    }
    val statusLabel = when (merchant.status) {
        MerchantStatus.ACTIVE   -> "نشط"
        MerchantStatus.EXPIRED  -> "منتهي"
        MerchantStatus.DISABLED -> "معطل"
    }
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Navy900), elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Indigo500.copy(0.3f), Indigo500.copy(0.1f)))),
                contentAlignment = Alignment.Center
            ) {
                Text(merchant.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Indigo400, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(merchant.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Slate100)
                Text(merchant.phone, style = MaterialTheme.typography.bodySmall, color = Slate400)
                Text("كود: ${merchant.activationCode}", style = MaterialTheme.typography.labelSmall, color = Slate600)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(0.15f)) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(statusColor))
                        Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Switch(
                    checked = merchant.status == MerchantStatus.ACTIVE,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.height(20.dp),
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Indigo500, uncheckedTrackColor = Slate700)
                )
            }
        }
    }
}
