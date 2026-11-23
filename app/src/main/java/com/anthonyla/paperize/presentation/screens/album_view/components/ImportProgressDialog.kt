package com.anthonyla.paperize.presentation.screens.album_view.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.common.theme.AppSpacing
import com.anthonyla.paperize.presentation.screens.album_view.ImportProgress

@Composable
fun ImportProgressDialog(progress: ImportProgress, onCancel: () -> Unit) {
    val active = progress as? ImportProgress.Active ?: return

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = {
            Text(
                stringResource(
                    when (active) {
                        is ImportProgress.Scanning -> R.string.import_adding_folder
                        is ImportProgress.Saving -> R.string.import_adding_wallpapers
                    }
                )
            )
        },
        text = {
            when (active) {
                is ImportProgress.Scanning -> Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(pluralStringResource(R.plurals.import_scanning, active.found, active.found))
                }

                is ImportProgress.Saving -> Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                    Text(stringResource(R.string.import_saving, active.saved, active.total))
                    val fraction by animateFloatAsState(
                        targetValue = if (active.total > 0) active.saved.toFloat() / active.total else 0f,
                        label = "importProgress"
                    )
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
