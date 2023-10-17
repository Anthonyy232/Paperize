package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anthonyla.paperize.R
import kotlinx.coroutines.android.awaitFrame

@Composable
fun AddAlbumDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var albumName by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "New Album",
                    modifier = Modifier.padding(20.dp),
                    fontSize = 25.sp
                )
                LaunchedEffect(focusRequester) {
                    awaitFrame()
                    focusRequester.requestFocus()
                }
                OutlinedTextField(
                    value = albumName,
                    onValueChange = { albumName = it },
                    label = { Text(stringResource(R.string.album_name)) },
                    modifier = Modifier
                        .padding(vertical = 12.dp, horizontal = 32.dp)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (albumName.isNotEmpty()) {
                                onConfirmation(albumName)
                                onDismissRequest()
                            }
                        }
                    )
                )
            }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        modifier = Modifier.padding(8.dp),
                        onClick = {
                            if (albumName.isNotEmpty()) {
                                onConfirmation(albumName)
                                onDismissRequest()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
        }
    }
}
