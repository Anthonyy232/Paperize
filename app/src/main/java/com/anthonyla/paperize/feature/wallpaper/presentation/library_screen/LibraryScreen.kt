package com.anthonyla.paperize.feature.wallpaper.presentation.library_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.R
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.AlbumItem

@Composable
fun LibraryScreen(
    albumsViewModel: AlbumsViewModel = hiltViewModel(),
    onAddNewAlbumClick: () -> Unit,
    onViewAlbum: (String) -> Unit,
    animate: Boolean
    ) {
    val lazyListState = rememberLazyGridState()
    val state = albumsViewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            LargeFloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onAddNewAlbumClick,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_album),
                    modifier = Modifier.padding(16.dp),
                )
            }
        },
        content = { it
            LazyVerticalGrid(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp, 4.dp),
                horizontalArrangement = Arrangement.Start,
                content = {
                    items (state.value.albumsWithWallpapers, key = { album -> album.album.initialAlbumName}
                    ) { album ->
                        AlbumItem(
                            album = album.album,
                            onAlbumViewClick = { onViewAlbum(album.album.initialAlbumName) },
                            modifier = Modifier.padding(4.dp),
                            animate = animate
                        )
                    }
                }
            )
        }
    )
}