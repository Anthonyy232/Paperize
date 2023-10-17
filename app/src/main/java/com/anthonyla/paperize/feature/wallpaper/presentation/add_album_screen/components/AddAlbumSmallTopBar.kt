package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.R
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumState
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components.DeleteImagesAlertDialog
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlbumSmallTopBar(
    albumState: StateFlow<AddAlbumState>,
    title: String,
    isEmpty: Boolean,
    selectionMode: Boolean,
    onSelectAllClick: () -> Unit,
    onBackClick: () -> Unit,
    onDeleteSelected: () -> Unit,
    onConfirmationClick: (String) -> Unit,
    modifier: Modifier = Modifier
    ) {
    val state = albumState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var editableTitle by rememberSaveable { mutableStateOf(title) }
    var showAlertDialog by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        title = {
            if (!selectionMode) {
                TextField(
                    value = editableTitle,
                    onValueChange = { editableTitle = it },
                    placeholder = { Text(stringResource(R.string.enter_the_name_of_the_album)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    isError = editableTitle.isEmpty(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }
        },
        navigationIcon = {
            if (!selectionMode) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .requiredSize(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.home_screen),
                    )
                }
            }
            else {
                Row (
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSelectAllClick
                    ) {
                        val bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
                        if (state.value.allSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.all_images_selected_for_deletion),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .border(2.dp, bgColor, CircleShape)
                                    .clip(CircleShape)
                                    .background(bgColor)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.RadioButtonUnchecked,
                                contentDescription = stringResource(R.string.select_all_images_for_deletion),
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                    Text("${state.value.selectedCount} selected")
                }
            }
        },
        actions = {
            if(!selectionMode) {
                IconButton(onClick = { onConfirmationClick(editableTitle) }) {
                    if (!isEmpty) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.add_album),
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
            else {
                if (showAlertDialog) DeleteImagesAlertDialog (
                    onDismissRequest = { showAlertDialog = false },
                    onConfirmation = {
                        showAlertDialog = false
                        onDeleteSelected()
                    }
                )
                IconButton(
                    onClick = { showAlertDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.select_all_images_for_deletion),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    )
}