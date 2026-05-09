package com.trader.admin.ui.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.trader.admin.ui.theme.*
import com.trader.core.domain.model.SubscriptionRequest
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRequestsScreen(
    onNavigateUp: () -> Unit,
    viewModel: AdminRequestsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var previewUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("طلبات الاشتراك", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = Slate100)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy950,
                    titleContentColor = Slate100
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Navy950
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            if (uiState.isLoading && uiState.requests.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Indigo500)
            } else if (uiState.requests.isEmpty()) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Inbox, null, Modifier.size(64.dp), tint = Slate700)
                    Text("لا توجد طلبات معلقة", color = Slate400)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.requests) { request ->
                        RequestItem(
                            request = request,
                            onPreview = { previewUrl = it },
                            onApprove = { viewModel.approve(request) },
                            onReject = { viewModel.reject(request) }
                        )
                    }
                }
            }
        }
    }

    previewUrl?.let { url ->
        Dialog(onDismissRequest = { previewUrl = null }) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Navy900)
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { previewUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(0.5f), CircleShape)
                ) { Icon(Icons.Rounded.Close, null, tint = Color.White) }
            }
        }
    }
}

@Composable
private fun RequestItem(
    request: SubscriptionRequest,
    onPreview: (String) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val date =
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(request.requestedAt))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy900)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(request.merchantCode, fontWeight = FontWeight.Bold, color = Slate100)
                    Text(
                        request.planType,
                        color = Indigo300,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        request.paymentMethod,
                        color = Slate400,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(date, color = Slate600, style = MaterialTheme.typography.labelSmall)
                }
                AsyncImage(
                    model = request.receiptUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPreview(request.receiptUrl) },
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("رفض")
                }
                Button(
                    onClick = onApprove,
                    Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تفعيل")
                }
            }
        }
    }
}