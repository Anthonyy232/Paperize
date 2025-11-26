package com.anthonyla.paperize.presentation.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.theme.AppSpacing

/**
 * Dialog for adding a new album
 */
@Composable
fun AddAlbumDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var albumName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_album)) },
        text = {
            Column {
                Text(stringResource(R.string.enter_the_name_of_the_album))
                Spacer(modifier = Modifier.height(AppSpacing.small))
                OutlinedTextField(
                    value = albumName,
                    onValueChange = { albumName = it },
                    label = { Text(stringResource(R.string.album_name)) },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = if (errorMessage != null) {
                        { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (albumName.isNotBlank()) {
                        onConfirm(albumName.trim())
                    }
                },
                enabled = albumName.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = modifier
    )
}

/**
 * Dialog for editing album name
 */
@Composable
fun EditAlbumNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var albumName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_name)) },
        text = {
            OutlinedTextField(
                value = albumName,
                onValueChange = { albumName = it },
                label = { Text(stringResource(R.string.album_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (albumName.isNotBlank()) {
                        onConfirm(albumName.trim())
                    }
                },
                enabled = albumName.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = modifier
    )
}

/**
 * Dialog for confirming album deletion
 */
@Composable
fun DeleteAlbumDialog(
    albumName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_album_question)) },
        text = { Text(stringResource(R.string.are_you_sure_you_want_to_delete_this)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete_album))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = modifier
    )
}

/**
 * Dialog for confirming wallpaper deletion
 */
@Composable
fun DeleteWallpapersDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_these)) },
        text = { Text(stringResource(R.string.are_you_sure_you_want_to_delete_these_wallpapers)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete_album))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = modifier
    )
}

/**
 * Dialog for confirming reset
 */
@Composable
fun ResetDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reset_all_data)) },
        text = { Text(stringResource(R.string.are_you_sure_you_want_to_reset_all_settings_and_data_to_default)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = modifier
    )
}
