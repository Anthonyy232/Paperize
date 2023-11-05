package com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumViewTopBar(
    title: String,
    onBackClick: () -> Unit,
    onDeleteAlbum: () -> Unit,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteAlertDialog by rememberSaveable { mutableStateOf(false) }
    var showNameChangeDialog by rememberSaveable { mutableStateOf(false) }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        title = { Text(title) },
        navigationIcon = {
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
        },
        actions = {
            if (showDeleteAlertDialog) DeleteAlbumAlertDialog (
                onDismissRequest = { showDeleteAlertDialog = false },
                onConfirmation = {
                    showDeleteAlertDialog = false
                    onDeleteAlbum()
                }
            )
            if (showNameChangeDialog) AlbumNameDialog (
                onDismissRequest = { showNameChangeDialog = false },
                onConfirmation = {
                    showNameChangeDialog = false
                    onTitleChange(it)
                }
            )
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.MoreVertIcon)
                )
                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                ) {
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        offset = DpOffset(0.dp, 5.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_album)) },
                            onClick = {
                                showDeleteAlertDialog = true
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.change_name)) },
                            onClick = {
                                showNameChangeDialog = true
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}