package com.trader.salesmanager.ui.debts

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
import com.trader.salesmanager.ui.components.*
import com.trader.salesmanager.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun DebtsScreen(
    onNavigateUp: () -> Unit,
    onCustomerClick: (Long) -> Unit,
    viewModel: DebtsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(DebtRed, Color(0xFFFF6B6B))))
                    .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("الديون", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${uiState.debts.size} زبون عليه دين", color = Color.White.copy(0.7f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            AnimatedContent(
                targetState = uiState.isLoading to uiState.debts.isEmpty(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "debts"
            ) { (loading, empty) ->
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DebtRed)
                    }
                    empty -> EmptyState(
                        icon = Icons.Rounded.CheckCircle,
                        title = "لا توجد ديون 🎉",
                        subtitle = "جميع الزبائن سددوا ديونهم",
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(uiState.debts, key = { _, d -> d.customer.id }) { index, item ->
                            val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                            AnimatedVisibility(
                                visibleState = visible,
                                enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300, delayMillis = index * 50)) + fadeIn()
                            ) {
                                DebtCard(
                                    name = item.customer.name,
                                    debt = item.debt,
                                    rank = index + 1,
                                    onClick = { onCustomerClick(item.customer.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtCard(name: String, debt: Double, rank: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = DebtRed.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DebtRed.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("#$rank", color = DebtRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("اضغط لعرض التفاصيل", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                String.format("%.2f", debt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DebtRed
            )
        }
    }
}
