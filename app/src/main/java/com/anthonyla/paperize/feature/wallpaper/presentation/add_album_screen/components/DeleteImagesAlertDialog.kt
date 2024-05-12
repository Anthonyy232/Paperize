package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anthonyla.paperize.R

/**
 * AlertDialog to confirm deletion of images
 */
@Composable
fun DeleteImagesAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete_confirmation))
        },
        title = { Text(text = stringResource(R.string.delete_these)) },
        text = { Text(text = stringResource(R.string.are_you_sure_you_want_to_delete_these_wallpapers)) },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(onClick = { onConfirmation() }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}