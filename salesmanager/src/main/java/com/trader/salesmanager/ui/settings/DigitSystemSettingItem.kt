package com.trader.salesmanager.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trader.core.util.format.AppNumberFormat
import com.trader.core.util.format.DigitSystemPreference
import com.trader.salesmanager.R
import com.trader.salesmanager.ui.theme.Emerald500
import kotlinx.coroutines.launch

/** "نظام الأرقام": Arabic-Indic (default) or Western digits for every screen, invoice and report. */
@Composable
fun DigitSystemSettingItem() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val current = AppNumberFormat.digits
    val options = listOf(
        AppNumberFormat.DigitSystem.ARABIC_INDIC to stringResource(R.string.settings_digits_arabic_indic),
        AppNumberFormat.DigitSystem.LATIN to stringResource(R.string.settings_digits_western)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Emerald500.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Pin, contentDescription = null, tint = Emerald500)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_digits_title), style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.settings_digits_subtitle), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (system, label) ->
                    SegmentedButton(
                        selected = current == system,
                        onClick = { scope.launch { DigitSystemPreference.set(context, system) } },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size)
                    ) { Text(label) }
                }
            }
        }
    }
}
