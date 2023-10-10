package com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anthonyla.paperize.R

@Composable
fun DeleteImagesAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.delete_confirmation)
            )
        },
        title = {
            Text(text = "Delete wallpapers?")
        },
        text = {
            Text(text = "Are you sure you want to delete these wallpapers?")
        },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(onClick = { onConfirmation() }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }) {
                Text("Cancel")
            }
        }
    )
}