package com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.R
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.DeleteImagesAlertDialog
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.AlbumViewState
import kotlinx.coroutines.flow.StateFlow

/**
 * Top bar for the album view screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumViewTopBar(
    title: String,
    allSelected: Boolean,
    selectedCount: Int,
    selectionMode: Boolean,
    isLoad: Boolean,
    onBackClick: () -> Unit,
    onSortClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onDeleteAlbum: () -> Unit,
    onDeleteSelected: () -> Unit,
    onTitleChange: (String) -> Unit,
) {
    var showDeleteAlertDialog by rememberSaveable { mutableStateOf(false) }
    var showNameChangeDialog by rememberSaveable { mutableStateOf(false) }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var showSelectionDeleteDialog by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        colors = topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)),
        title = { if (!selectionMode) Text(title) },
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
                        contentDescription = stringResource(id = R.string.home_screen)
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
                    IconButton(onClick = onSelectAllClick) {
                        Icon(
                            imageVector = if (allSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = stringResource(
                                if (allSelected) R.string.all_images_selected_for_deletion
                                else R.string.select_all_images_for_deletion
                            ),
                            modifier = if (allSelected) {
                                Modifier
                                    .padding(4.dp)
                                    .border(2.dp, bgColor, CircleShape)
                                    .clip(CircleShape)
                                    .background(bgColor)
                            } else {
                                Modifier.padding(6.dp)
                            }
                        )
                    }
                    Text(stringResource(R.string.selected, selectedCount))
                }
            }
        },
        actions = {
            if (!selectionMode) {
                if (showDeleteAlertDialog) DeleteAlbumAlertDialog (
                    onDismissRequest = { showDeleteAlertDialog = false },
                    onConfirmation = {
                        showDeleteAlertDialog = false
                        if (!isLoad) { onDeleteAlbum() }
                    }
                )
                if (showNameChangeDialog) AlbumNameDialog (
                    onDismissRequest = { showNameChangeDialog = false },
                    onConfirmation = {
                        showNameChangeDialog = false
                        if (!isLoad) { onTitleChange(it) }
                    }
                )
                Row {
                    Box {
                        IconButton(
                            onClick = { if (!isLoad) { onSortClick() } }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.Sort,
                                contentDescription = stringResource(R.string.sort_items),
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = {
                    if (!isLoad) { menuExpanded = true }
                }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                    )
                    MaterialTheme(
                        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            offset = DpOffset(0.dp, 5.dp),
                            modifier = Modifier.padding(8.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_album)) },
                                onClick = {
                                    if (!isLoad) {
                                        showDeleteAlertDialog = true
                                    }
                                        menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.change_name)) },
                                onClick = {
                                    if (!isLoad) {
                                        showNameChangeDialog = true
                                    }
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                if (showSelectionDeleteDialog) DeleteImagesAlertDialog (
                    onDismissRequest = { showSelectionDeleteDialog = false },
                    onConfirmation = {
                        showSelectionDeleteDialog = false
                        if (!isLoad) {
                            onDeleteSelected()
                        }
                    }
                )
                IconButton(
                    onClick = { showSelectionDeleteDialog = true }
                ) {
                    Icon(
                        imageVector = if (showSelectionDeleteDialog) Icons.Filled.Delete else Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.select_all_images_for_deletion),
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    )
}