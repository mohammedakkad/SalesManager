package com.trader.salesmanager.ui.debts

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.core.util.DateUtils.toDateString
import com.trader.salesmanager.R
import com.trader.salesmanager.ui.components.EmptyState
import com.trader.salesmanager.ui.theme.DebtRed
import com.trader.salesmanager.util.WhatsAppMessageBuilder
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun RemindAllScreen(
    onNavigateUp: () -> Unit,
    viewModel: RemindAllViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val whatsAppNotInstalled = stringResource(R.string.whatsapp_not_installed)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF25D366), Color(0xFF128C7E))))
                    .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            stringResource(R.string.remind_all_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (!uiState.isLoading && uiState.debts.isNotEmpty()) {
                            Text(
                                stringResource(R.string.remind_all_subtitle, uiState.debts.size),
                                color = Color.White.copy(0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = uiState.isLoading to uiState.debts.isEmpty(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "remind_all"
            ) { (loading, empty) ->
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF25D366))
                    }

                    empty -> EmptyState(
                        icon = Icons.Rounded.CheckCircle,
                        title = stringResource(R.string.remind_all_empty_title),
                        subtitle = stringResource(R.string.remind_all_empty_subtitle),
                        modifier = Modifier.fillMaxSize()
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.debts, key = { it.customer.id }) { debt ->
                            val isSent = debt.customer.id in uiState.sentCustomerIds
                            RemindableDebtCard(
                                debt = debt,
                                isSent = isSent,
                                onSendWhatsApp = {
                                    val message = WhatsAppMessageBuilder.build(
                                        context = context,
                                        customerName = debt.customer.name,
                                        amount = debt.amount,
                                        dueDate = debt.nearestDueDate,
                                        storeName = uiState.storeName
                                    )
                                    val url = WhatsAppMessageBuilder.buildWaMeUrl(
                                        debt.customer.phone,
                                        message
                                    )
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, url.toUri())
                                        )
                                        viewModel.markSent(debt.customer.id)
                                    } catch (_: ActivityNotFoundException) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(whatsAppNotInstalled)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemindableDebtCard(
    debt: RemindableDebt,
    isSent: Boolean,
    onSendWhatsApp: () -> Unit
) {
    val invalidPhone = WhatsAppMessageBuilder.isPhoneLikelyInvalid(debt.customer.phone)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSent) Color(0xFF25D366).copy(0.08f) else DebtRed.copy(0.06f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSent) Color(0xFF25D366).copy(0.15f) else DebtRed.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        debt.customer.name.take(1),
                        color = if (isSent) Color(0xFF25D366) else DebtRed,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        debt.customer.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (debt.customer.phone.isNotBlank()) {
                        Text(
                            debt.customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    String.format(Locale.US, "%.2f ₪", debt.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSent) Color(0xFF25D366) else DebtRed
                )
            }

            Text(
                stringResource(R.string.remind_all_due_date, debt.nearestDueDate.toDateString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (invalidPhone) {
                Text(
                    stringResource(R.string.whatsapp_invalid_phone_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (isSent) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.remind_all_sent))
                }
            } else {
                Button(
                    onClick = onSendWhatsApp,
                    enabled = !invalidPhone,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Rounded.Chat, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.remind_all_send_whatsapp))
                }
            }
        }
    }
}
