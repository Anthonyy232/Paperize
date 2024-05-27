package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anthonyla.paperize.R

/**
 * Dialog to let the user know of the app's potential delays when it comes to scheduling wallpapers.
 */
@Composable
fun ShowDelayNoticeDialog(
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismissRequest() },
        icon = { Icon(Icons.Filled.Notifications, stringResource(R.string.delay_notice)) },
        title = {
            Text(stringResource(R.string.delay_notice_dialog))
        },
        text = {
            Text(stringResource(R.string.to_minimize_battery_usage_changing_wallpapers_may_have_some_delays_once_scheduled_when_the_device_is_asleep_the_wallpaper_may_take_longer_to_apply))
        },
        confirmButton = {
            Button(
                onClick = { onDismissRequest() }
            ) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}