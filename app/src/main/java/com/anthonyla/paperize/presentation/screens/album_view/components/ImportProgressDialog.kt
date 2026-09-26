package com.anthonyla.paperize.presentation.screens.album_view.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.screens.album_view.ImportProgress

@Composable
fun ImportProgressDialog(progress: ImportProgress, onCancel: () -> Unit) {
    if (progress is ImportProgress.Idle) return

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val titleRes = when (progress) {
                    is ImportProgress.Scanning -> R.string.import_adding_folder
                    else -> R.string.import_adding_wallpapers
                }
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium
                )

                when (progress) {
                    is ImportProgress.Scanning -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                text = stringResource(R.string.import_scanning, progress.found),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    is ImportProgress.Saving -> {
                        Text(
                            text = stringResource(R.string.import_saving, progress.saved, progress.total),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val fraction by animateFloatAsState(
                            targetValue = if (progress.total > 0) progress.saved.toFloat() / progress.total else 0f,
                            label = "importProgress"
                        )
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    }

                    ImportProgress.Idle -> Unit
                }
                TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
