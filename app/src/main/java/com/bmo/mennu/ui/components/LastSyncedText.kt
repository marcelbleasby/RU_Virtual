package com.bmo.mennu.ui.components

import android.text.format.DateUtils
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LastSyncedText(lastSyncedAt: Long?, modifier: Modifier = Modifier) {
    if (lastSyncedAt == null) return
    Text(
        text = "Atualizado ${DateUtils.getRelativeTimeSpanString(lastSyncedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
