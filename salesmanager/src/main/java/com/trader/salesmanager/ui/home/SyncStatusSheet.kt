package com.trader.salesmanager.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.SyncStatus
import com.trader.core.sync.GlobalSyncState
import com.trader.core.sync.UnsyncedItem
import com.trader.core.sync.UnsyncedItemType
import com.trader.salesmanager.R
import com.trader.salesmanager.ui.components.EmptyState
import com.trader.salesmanager.ui.theme.DebtRed
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.PaidGreen
import com.trader.salesmanager.ui.theme.UnpaidAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SyncStatusIconButton(
    syncState: GlobalSyncState,
    onClick: () -> Unit
) {
    when (syncState) {
        is GlobalSyncState.AllSynced -> SyncedIconButton(onClick)
        is GlobalSyncState.Pending -> PendingIconButton(onClick)
        is GlobalSyncState.Failed -> FailedIconButton(onClick)
    }
}

@Composable
private fun SyncedIconButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(0.2f))
    ) {
        Icon(
            Icons.Rounded.CloudDone,
            contentDescription = stringResource(R.string.sync_status_synced_desc),
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun PendingIconButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(0.2f))
    ) {
        Icon(
            Icons.Rounded.CloudSync,
            contentDescription = stringResource(R.string.sync_status_pending_desc),
            tint = UnpaidAmber,
            modifier = Modifier
                .size(22.dp)
                .rotate(rotation)
        )
    }
}

@Composable
private fun FailedIconButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(0.2f))
    ) {
        Icon(
            Icons.Rounded.SyncProblem,
            contentDescription = stringResource(R.string.sync_status_failed_desc),
            tint = DebtRed.copy(alpha = alpha),
            modifier = Modifier.size(22.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusBottomSheet(
    syncState: GlobalSyncState,
    unsyncedItems: List<UnsyncedItem>,
    onDismiss: () -> Unit,
    onRetrySync: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            SheetHeader(syncState)

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outline.copy(0.2f)
            )

            Spacer(Modifier.height(8.dp))

            RetryButton(
                isSyncing = isSyncing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                onClick = {
                    isSyncing = true
                    onRetrySync()
                    scope.launch {
                        delay(2500)
                        isSyncing = false
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            if (unsyncedItems.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.CheckCircle,
                    title = stringResource(R.string.sync_sheet_empty_title),
                    subtitle = stringResource(R.string.sync_sheet_empty_subtitle)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(unsyncedItems, key = { it.id }) { item ->
                        UnsyncedItemRow(item)
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(syncState: GlobalSyncState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val (icon, color, title, subtitle) = when (syncState) {
            is GlobalSyncState.AllSynced -> SheetHeaderData(
                Icons.Rounded.CloudDone,
                PaidGreen,
                stringResource(R.string.sync_sheet_title_synced),
                stringResource(R.string.sync_sheet_subtitle_synced)
            )
            is GlobalSyncState.Pending -> SheetHeaderData(
                Icons.Rounded.CloudSync,
                UnpaidAmber,
                stringResource(R.string.sync_sheet_title_pending),
                stringResource(R.string.sync_sheet_subtitle_pending, syncState.count)
            )
            is GlobalSyncState.Failed -> SheetHeaderData(
                Icons.Rounded.SyncProblem,
                DebtRed,
                stringResource(R.string.sync_sheet_title_failed),
                stringResource(R.string.sync_sheet_subtitle_failed, syncState.count)
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class SheetHeaderData(
    val icon: ImageVector,
    val color: Color,
    val title: String,
    val subtitle: String
)

@Composable
private fun RetryButton(
    isSyncing: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = { if (!isSyncing) onClick() },
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
    ) {
        if (isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
        } else {
            Icon(
                Icons.Rounded.Sync,
                null,
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            stringResource(R.string.sync_sheet_retry_button),
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun UnsyncedItemRow(item: UnsyncedItem) {
    val statusColor = when (item.syncStatus) {
        SyncStatus.FAILED -> DebtRed
        SyncStatus.PENDING -> UnpaidAmber
        else -> PaidGreen
    }
    val typeIcon = when (item.type) {
        UnsyncedItemType.TRANSACTION -> Icons.Rounded.Receipt
        UnsyncedItemType.CASH_BOX -> Icons.Rounded.AccountBalanceWallet
        UnsyncedItemType.STOCK_MOVEMENT -> Icons.Rounded.Inventory2
        UnsyncedItemType.INVOICE_ITEM -> Icons.Rounded.Receipt
    }
    val statusLabel = when (item.syncStatus) {
        SyncStatus.FAILED -> stringResource(R.string.sync_status_failed_label)
        SyncStatus.PENDING -> stringResource(R.string.sync_status_pending_label)
        else -> stringResource(R.string.sync_status_synced_label)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusColor.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
            }
            Text(
                item.label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = statusColor.copy(0.12f)
            ) {
                Text(
                    statusLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
