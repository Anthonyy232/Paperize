package com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaper
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.FolderItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.components.AlbumViewTopBar

@Composable
fun AlbumViewScreen(
    viewModel: AlbumViewScreenViewModel = hiltViewModel(),
    albumWithWallpaper: AlbumWithWallpaper,
    onBackClick: () -> Unit,
    onShowWallpaperView: (String) -> Unit,
    onShowFolderView: (String, String?, List<String>) -> Unit,
    onDeleteAlbum: () -> Unit,
    onTitleChange: (String, AlbumWithWallpaper) -> Unit,
    onSelectionDeleted: () -> Unit
) {
    viewModel.onEvent(AlbumViewEvent.SetSize(albumWithWallpaper.wallpapers.size + albumWithWallpaper.folders.size))
    val lazyListState = rememberLazyStaggeredGridState()
    val state = viewModel.state.collectAsStateWithLifecycle()
    var selectionMode by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        if (selectionMode) {
            selectionMode = false
            viewModel.onEvent(AlbumViewEvent.DeselectAll)
        } else {
            onBackClick()
            viewModel.onEvent(AlbumViewEvent.ClearState)
        }
    }

    Scaffold(
        topBar = {
            AlbumViewTopBar(
                title = albumWithWallpaper.album.displayedAlbumName,
                selectionMode = selectionMode,
                albumState = viewModel.state,
                onBackClick = {
                    viewModel.onEvent(AlbumViewEvent.ClearState)
                    onBackClick()
                },
                onDeleteAlbum = onDeleteAlbum,
                onTitleChange = { onTitleChange(it, albumWithWallpaper) },
                onSelectAllClick = {
                    if (!state.value.allSelected) viewModel.onEvent(AlbumViewEvent.SelectAll(albumWithWallpaper))
                    else viewModel.onEvent(AlbumViewEvent.DeselectAll)
                },
                onDeleteSelected = {
                    selectionMode = false
                    viewModel.onEvent(AlbumViewEvent.DeleteSelected(albumWithWallpaper))
                    viewModel.onEvent(AlbumViewEvent.DeselectAll)
                    onSelectionDeleted()
                }
            )
        },
        content = {
            LazyVerticalStaggeredGrid(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp, 4.dp),
                horizontalArrangement = Arrangement.Start,
                content = {
                    items (items = albumWithWallpaper.folders, key = { folder -> folder.folderUri.hashCode()}
                    ) { folder ->
                        FolderItem(
                            folder = folder,
                            itemSelected = state.value.selectedFolders.contains(folder.folderUri),
                            selectionMode = selectionMode,
                            onActivateSelectionMode = { selectionMode = it },
                            onItemSelection = {
                                viewModel.onEvent(
                                    if (!state.value.selectedFolders.contains(folder.folderUri))
                                        AlbumViewEvent.SelectFolder(folder.folderUri)
                                    else {
                                        AlbumViewEvent.RemoveFolderFromSelection(folder.folderUri)
                                    }
                                )
                            },
                            onFolderViewClick = {
                                onShowFolderView(folder.folderUri, folder.folderName, folder.wallpapers)
                            },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    items (items = albumWithWallpaper.wallpapers, key = { wallpaper -> wallpaper.wallpaperUri.hashCode()}
                    ) { wallpaper ->
                        WallpaperItem(
                            wallpaperUri = wallpaper.wallpaperUri,
                            itemSelected = state.value.selectedWallpapers.contains(wallpaper.wallpaperUri),
                            selectionMode = selectionMode,
                            onActivateSelectionMode = { selectionMode = it },
                            onItemSelection = {
                                viewModel.onEvent(
                                    if (!state.value.selectedWallpapers.contains(wallpaper.wallpaperUri))
                                        AlbumViewEvent.SelectWallpaper(wallpaper.wallpaperUri)
                                    else {
                                        AlbumViewEvent.RemoveWallpaperFromSelection(wallpaper.wallpaperUri)
                                    }
                                )
                            },
                            onWallpaperViewClick = {
                                onShowWallpaperView(wallpaper.wallpaperUri)
                            },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            )
        }
    )
}