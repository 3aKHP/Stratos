package com.gpsplane.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gpsplane.app.R

/**
 * Inline banner shown when the foreground service is running but
 * [android.Manifest.permission.POST_NOTIFICATIONS] was denied on
 * API 33+. Without the persistent notification the system is more
 * aggressive about killing the background GPS subscription — the user
 * loses flight tracking on screen-off without any visible cue.
 *
 * Callers decide whether to show it; this Composable only renders.
 * [onGrant] re-requests the permission, [onDismiss] records the user's
 * "don't show again" choice (persisted by the caller) and hides it.
 */
@Composable
fun NotificationBanner(
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.NotificationsOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            stringResource(R.string.notification_blocked_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onGrant) {
            Text(stringResource(R.string.notification_blocked_grant))
        }
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.notification_blocked_dismiss),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
