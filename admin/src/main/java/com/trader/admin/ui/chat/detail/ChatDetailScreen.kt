package com.trader.admin.ui.chat.detail

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.SENDER_ADMIN
import com.trader.admin.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChatDetailScreen(
    merchantId: String, merchantName: String, onNavigateUp: () -> Unit,
    viewModel: ChatDetailViewModel = koinViewModel(parameters = { parametersOf(merchantId) })
) {
    val messages by viewModel.messages.collectAsState()
    val text by viewModel.text.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Scaffold(containerColor = Navy950,
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().background(Navy900)
                .navigationBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = text, onValueChange = viewModel::updateText,
                    placeholder = { Text("اكتب رسالة...", color = Slate600) },
                    shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Indigo500, unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100, unfocusedTextColor = Slate100,
                        focusedContainerColor = Slate800, unfocusedContainerColor = Slate800
                    ), maxLines = 3
                )
                FloatingActionButton(onClick = viewModel::send, containerColor = Indigo500, contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.size(52.dp)
                ) { Icon(Icons.Rounded.Send, null) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Cyan500, Indigo500)))
                .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateUp) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(merchantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("دردشة الدعم الفني", color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            LazyColumn(state = listState, contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isAdmin = msg.senderId == SENDER_ADMIN
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isAdmin) Arrangement.End else Arrangement.Start) {
                        Surface(
                            shape = RoundedCornerShape(if (isAdmin) 16.dp else 4.dp, if (isAdmin) 4.dp else 16.dp, 16.dp, 16.dp),
                            color = if (isAdmin) Indigo500 else Navy900,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(msg.text, color = if (isAdmin) Color.White else Slate100, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(2.dp))
                                Text(msg.senderName, color = if (isAdmin) Color.White.copy(0.6f) else Slate600, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
