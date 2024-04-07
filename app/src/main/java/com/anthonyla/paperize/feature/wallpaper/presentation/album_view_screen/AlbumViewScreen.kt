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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.FolderItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.components.AlbumViewTopBar

@Composable
fun AlbumViewScreen(
    albumViewModel: AlbumViewScreenViewModel = hiltViewModel(),
    album: AlbumWithWallpaperAndFolder,
    onBackClick: () -> Unit,
    onShowWallpaperView: (String) -> Unit,
    onShowFolderView: (String, String?, List<String>) -> Unit,
    onDeleteAlbum: () -> Unit,
    onTitleChange: (String, AlbumWithWallpaperAndFolder) -> Unit,
    onSelectionDeleted: () -> Unit
) {
    albumViewModel.onEvent(AlbumViewEvent.SetSize(album.wallpapers.size + album.folders.size))
    val lazyListState = rememberLazyStaggeredGridState()
    val albumState = albumViewModel.state.collectAsStateWithLifecycle()
    var selectionMode by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        if (selectionMode) {
            selectionMode = false
            albumViewModel.onEvent(AlbumViewEvent.DeselectAll)
        } else {
            onBackClick()
            albumViewModel.onEvent(AlbumViewEvent.ClearState)
        }
    }

    Scaffold(
        topBar = {
            AlbumViewTopBar(
                title = album.album.displayedAlbumName,
                selectionMode = selectionMode,
                albumState = albumViewModel.state,
                onBackClick = {
                    albumViewModel.onEvent(AlbumViewEvent.ClearState)
                    onBackClick()
                },
                onDeleteAlbum = onDeleteAlbum,
                onTitleChange = { onTitleChange(it, album) },
                onSelectAllClick = {
                    if (!albumState.value.allSelected) albumViewModel.onEvent(AlbumViewEvent.SelectAll(album))
                    else albumViewModel.onEvent(AlbumViewEvent.DeselectAll)
                },
                onDeleteSelected = {
                    selectionMode = false
                    albumViewModel.onEvent(AlbumViewEvent.DeleteSelected(album))
                    albumViewModel.onEvent(AlbumViewEvent.DeselectAll)
                    onSelectionDeleted()
                }
            )
        },
        content = { it ->
            LazyVerticalStaggeredGrid(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp, 4.dp),
                horizontalArrangement = Arrangement.Start,
                content = {
                    items (items = album.folders, key = { folder -> folder.folderUri.hashCode() }
                    ) { folder ->
                        FolderItem(
                            folder = folder,
                            itemSelected = albumState.value.selectedFolders.contains(folder.folderUri),
                            selectionMode = selectionMode,
                            onActivateSelectionMode = { selectionMode = it },
                            onItemSelection = {
                                albumViewModel.onEvent(
                                    if (!albumState.value.selectedFolders.contains(folder.folderUri))
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
                    items (items = album.wallpapers, key = { wallpaper -> wallpaper.wallpaperUri.hashCode()}
                    ) { wallpaper ->
                        WallpaperItem(
                            wallpaperUri = wallpaper.wallpaperUri,
                            itemSelected = albumState.value.selectedWallpapers.contains(wallpaper.wallpaperUri),
                            selectionMode = selectionMode,
                            onActivateSelectionMode = { selectionMode = it },
                            onItemSelection = {
                                albumViewModel.onEvent(
                                    if (!albumState.value.selectedWallpapers.contains(wallpaper.wallpaperUri))
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