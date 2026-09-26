package com.anthonyla.paperize.presentation.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.theme.AppSpacing

@Composable
fun AddAlbumDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isSaving: Boolean = false
) {
    var albumName by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.add_album)) },
        text = {
            Column {
                Text(stringResource(R.string.enter_the_name_of_the_album))
                Spacer(modifier = Modifier.height(AppSpacing.small))
                OutlinedTextField(
                    value = albumName,
                    enabled = !isSaving,
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
                onClick = { onConfirm(albumName.trim()) },
                enabled = !isSaving && albumName.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = modifier
    )
}
