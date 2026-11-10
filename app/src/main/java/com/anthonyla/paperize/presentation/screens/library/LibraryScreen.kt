package com.anthonyla.paperize.presentation.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anthonyla.paperize.R
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.presentation.common.components.AddAlbumDialog
import com.anthonyla.paperize.presentation.common.components.EmptyCollection
import com.anthonyla.paperize.presentation.common.theme.AppGrid
import com.anthonyla.paperize.presentation.common.theme.AppIconSizes
import com.anthonyla.paperize.presentation.common.theme.AppSpacing
import com.anthonyla.paperize.presentation.screens.library.components.AlbumItem
import kotlinx.coroutines.launch

/** [onCreateAlbum] returns the string resource of the error to show, or null once the album exists. */
@Composable
fun LibraryScreen(
    albums: List<AlbumSummary>,
    onViewAlbum: (String) -> Unit,
    onCreateAlbum: suspend (String) -> Int?,
    modifier: Modifier = Modifier
) {
    var showAddAlbumDialog by rememberSaveable { mutableStateOf(false) }
    var albumNameError by rememberSaveable { mutableStateOf<Int?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            LargeFloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { showAddAlbumDialog = true },
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_album),
                    modifier = Modifier.size(AppIconSizes.medium)
                )
            }
        }
    ) { paddingValues ->
        if (albums.isEmpty()) {
            EmptyCollection(
                title = stringResource(R.string.no_albums_found),
                hint = stringResource(R.string.create_first_album_hint),
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                columns = GridCells.Adaptive(AppGrid.itemMinSize),
                contentPadding = PaddingValues(AppSpacing.gridPadding),
                horizontalArrangement = Arrangement.Start
            ) {
                items(albums, key = { album -> album.id }) { album ->
                    AlbumItem(
                        album = album,
                        onAlbumViewClick = { onViewAlbum(album.id) },
                        modifier = Modifier.padding(AppSpacing.gridPadding)
                    )
                }
            }
        }
    }

    if (showAddAlbumDialog) {
        AddAlbumDialog(
            onDismiss = {
                showAddAlbumDialog = false
                albumNameError = null
            },
            onConfirm = { name ->
                isSaving = true
                albumNameError = null
                scope.launch {
                    try {
                        albumNameError = onCreateAlbum(name)
                        if (albumNameError == null) showAddAlbumDialog = false
                    } finally {
                        isSaving = false
                    }
                }
            },
            errorMessage = albumNameError?.let { stringResource(it) },
            isSaving = isSaving
        )
    }
}
