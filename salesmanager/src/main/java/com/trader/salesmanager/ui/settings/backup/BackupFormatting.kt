package com.trader.salesmanager.ui.settings.backup

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.trader.salesmanager.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun formatLastBackupRelative(timestamp: Long?): String {
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
