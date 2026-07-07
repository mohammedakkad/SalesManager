package com.trader.salesmanager.ui.settings.backup

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.salesmanager.R
import com.trader.salesmanager.ui.theme.Cyan500
import com.trader.salesmanager.ui.theme.Emerald100
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.Emerald700
import com.trader.salesmanager.ui.theme.InfoBlue
import com.trader.salesmanager.ui.theme.Violet500
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val HEADER_CONTENT_HEIGHT = 120.dp
private val OVERLAP = 40.dp

@Composable
fun BackupScreen(
    onNavigateUp: () -> Unit,
    viewModel: BackupViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        viewModel.onImportFileSelected(uri)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
        }
    }

    LaunchedEffect(uiState.shareFilePath) {
        if (uiState.shareFilePath != null) {
            viewModel.shareExportedFile()
        }
    }

    if (uiState.showImportConfirmDialog) {
        ImportConfirmDialog(
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::dismissImportConfirm
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(scrollState)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HEADER_CONTENT_HEIGHT + OVERLAP)
                        .background(
                            Brush.linearGradient(
                                listOf(Emerald700, Cyan500),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(
                                    Float.POSITIVE_INFINITY,
                                    Float.POSITIVE_INFINITY
                                )
                            )
                        )
                )
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 20.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateUp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(0.2f))
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(R.string.backup_title),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.backup_subtitle),
                                color = Color.White.copy(0.85f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    LastBackupStatusCard(
                        lastBackupAt = uiState.lastBackupAt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .offset(y = OVERLAP / 2)
                            .shadow(12.dp, RoundedCornerShape(24.dp))
                    )
                }
            }

            Spacer(Modifier.height(OVERLAP / 2 + 24.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.backup_reassurance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                BackupActionCard(
                    index = 0,
                    icon = Icons.Rounded.Backup,
                    title = stringResource(R.string.backup_export_title),
                    subtitle = stringResource(R.string.backup_export_subtitle),
                    accentColor = Emerald500,
                    trailingIcon = Icons.Rounded.Share,
                    isLoading = uiState.isExporting,
                    enabled = !uiState.isImporting,
                    onClick = viewModel::exportBackup
                )

                BackupActionCard(
                    index = 1,
                    icon = Icons.Rounded.Restore,
                    title = stringResource(R.string.backup_import_title),
                    subtitle = stringResource(R.string.backup_import_subtitle),
                    accentColor = InfoBlue,
                    trailingIcon = Icons.Rounded.Restore,
                    isLoading = uiState.isImporting,
                    enabled = !uiState.isExporting,
                    onClick = { importLauncher.launch(arrayOf("application/zip", "application/json", "*/*")) }
                )

                InfoTipCard()

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun LastBackupStatusCard(
    lastBackupAt: Long?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Emerald100.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.History,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.backup_last_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatLastBackup(lastBackupAt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (lastBackupAt != null) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.Shield,
                contentDescription = null,
                tint = Emerald500.copy(0.5f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BackupActionCard(
    index: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    trailingIcon: ImageVector,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val visible = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    AnimatedVisibility(
        visible,
        enter = slideInVertically(tween(320, index * 80)) { it / 2 } + fadeIn(tween(320, index * 80))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled && !isLoading, onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = accentColor,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(icon, null, tint = accentColor, modifier = Modifier.size(28.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    trailingIcon,
                    contentDescription = null,
                    tint = accentColor.copy(0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoTipCard() {
    val visible = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    AnimatedVisibility(
        visible,
        enter = slideInVertically(tween(360, 160)) { it / 3 } + fadeIn(tween(360, 160))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Violet500.copy(0.06f)
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.backup_tip_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Violet500
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.25f))
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.backup_tip_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )
            }
        }
    }
}

@Composable
private fun ImportConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Rounded.Warning, null, tint = InfoBlue)
        },
        title = {
            Text(
                stringResource(R.string.backup_confirm_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                stringResource(R.string.backup_confirm_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = InfoBlue)
            ) {
                Text(stringResource(R.string.backup_confirm_yes))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.backup_confirm_no))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun formatLastBackup(timestamp: Long?): String {
    if (timestamp == null) return stringResource(R.string.backup_never)
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> stringResource(R.string.backup_time_now)
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff).toInt()
            stringResource(R.string.backup_time_minutes, minutes)
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff).toInt()
            stringResource(R.string.backup_time_hours, hours)
        }
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
            stringResource(R.string.backup_time_days, days)
        }
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
