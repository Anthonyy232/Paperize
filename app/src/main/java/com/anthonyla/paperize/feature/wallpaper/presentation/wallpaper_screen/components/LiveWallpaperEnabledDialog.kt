package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anthonyla.paperize.R

/**
 * Dialog to show when a live wallpaper is detected and ask the user to disable it
 */
@Composable
fun ShowLiveWallpaperEnabledDialog(
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismissRequest() },
        icon = { Icon(Icons.Filled.ErrorOutline, stringResource(R.string.disable_live_wallpaper)) },
        title = {
            Text(stringResource(R.string.live_wallpaper_detected))
        },
        text = {
            Text(stringResource(R.string.please_disable_your_live_wallpaper_to_use_the_wallpaper_changer))
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