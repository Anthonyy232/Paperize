package com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumAnimatedFab
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.FolderItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.components.AlbumViewTopBar
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

@Composable
fun AlbumViewScreen(
    albumViewScreenViewModel: AlbumViewScreenViewModel = hiltViewModel(),
    album: AlbumWithWallpaperAndFolder,
    animate: Boolean,
    onBackClick: () -> Unit,
    onShowWallpaperView: (String) -> Unit,
    onShowFolderView: (String?, List<String>) -> Unit,
    onDeleteAlbum: () -> Unit,
    onAlbumNameChange: (String, AlbumWithWallpaperAndFolder) -> Unit,
    onSelectionDeleted: () -> Unit,
) {
    albumViewScreenViewModel.onEvent(AlbumViewEvent.SetSize(album.wallpapers.size + album.folders.size)) // For selectedAll state
    val lazyListState = rememberLazyGridState()
    val albumState = albumViewScreenViewModel.state.collectAsStateWithLifecycle()
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    /** Image picker **/
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri> ->
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val uriList = uris.mapNotNull { uri ->
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                val persistedUriPermissions = context.contentResolver.persistedUriPermissions
                if (persistedUriPermissions.any { it.uri == uri }) uri.toString() else null
            }
            if (uriList.isNotEmpty()) {
                albumViewScreenViewModel.onEvent(AlbumViewEvent.AddWallpapers(album, uriList))
            }
        }
    )

    /** Folder picker **/
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                val persistedUriPermissions = context.contentResolver.persistedUriPermissions
                if (persistedUriPermissions.any { it.uri == uri }) {
                    albumViewScreenViewModel.onEvent(AlbumViewEvent.AddFolder(album, uri.toString()))
                }
            }
        }
    )

    BackHandler(
        enabled = selectionMode,
        onBack = {
            albumViewScreenViewModel.onEvent(AlbumViewEvent.DeselectAll)
            selectionMode = false
        }
    )

    Scaffold(
        topBar = {
            AlbumViewTopBar(
                title = album.album.displayedAlbumName,
                selectionMode = selectionMode,
                albumState = albumViewScreenViewModel.state,
                onBackClick = {
                    albumViewScreenViewModel.onEvent(AlbumViewEvent.ClearState)
                    onBackClick()
                },
                onDeleteAlbum = onDeleteAlbum,
                onTitleChange = { onAlbumNameChange(it, album) },
                onSelectAllClick = {
                    if (!albumState.value.allSelected) albumViewScreenViewModel.onEvent(AlbumViewEvent.SelectAll(album))
                    else albumViewScreenViewModel.onEvent(AlbumViewEvent.DeselectAll)
                },
                onDeleteSelected = {
                    selectionMode = false
                    albumViewScreenViewModel.onEvent(AlbumViewEvent.DeleteSelected(album))
                    albumViewScreenViewModel.onEvent(AlbumViewEvent.DeselectAll)
                    onSelectionDeleted()
                }
            )
        },
        floatingActionButton = {
            AddAlbumAnimatedFab(
                onImageClick = {
                    selectionMode = false
                    imagePickerLauncher.launch(arrayOf("image/*"))
                },
                onFolderClick = {
                    selectionMode = false
                    folderPickerLauncher.launch(null)
                },
                animate = animate
            )
        },
        content = { it ->
            LazyVerticalGridScrollbar(
                state = lazyListState,
                settings = ScrollbarSettings.Default.copy(
                    thumbUnselectedColor = MaterialTheme.colorScheme.primary,
                    thumbSelectedColor = MaterialTheme.colorScheme.primary,
                    thumbShape = RoundedCornerShape(16.dp),
                    scrollbarPadding = 1.dp,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                LazyVerticalGrid(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(4.dp, 4.dp),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    items(count = album.folders.size, key = { index -> album.folders[index].folderUri }) { index ->
                        if (animate) {
                            FolderItem(
                                folder = album.folders[index],
                                itemSelected = albumState.value.selectedFolders.contains(album.folders[index].folderUri),
                                selectionMode = selectionMode,
                                onActivateSelectionMode = { selectionMode = it },
                                onItemSelection = {
                                    albumViewScreenViewModel.onEvent(
                                        if (!albumState.value.selectedFolders.contains(album.folders[index].folderUri))
                                            AlbumViewEvent.SelectFolder(album.folders[index].folderUri)
                                        else {
                                            AlbumViewEvent.RemoveFolderFromSelection(album.folders[index].folderUri)
                                        }
                                    )
                                },
                                onFolderViewClick = {
                                    if (album.folders[index].wallpapers.isNotEmpty()) onShowFolderView(album.folders[index].folderName, album.folders[index].wallpapers)
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(150.dp, 350.dp)
                                    .animateItem(
                                        placementSpec = tween(
                                            durationMillis = 800,
                                            delayMillis = 0,
                                            easing = FastOutSlowInEasing
                                        ),
                                    )
                            )
                        }
                        else {
                            FolderItem(
                                folder = album.folders[index],
                                itemSelected = albumState.value.selectedFolders.contains(album.folders[index].folderUri),
                                selectionMode = selectionMode,
                                onActivateSelectionMode = { selectionMode = it },
                                onItemSelection = {
                                    albumViewScreenViewModel.onEvent(
                                        if (!albumState.value.selectedFolders.contains(album.folders[index].folderUri))
                                            AlbumViewEvent.SelectFolder(album.folders[index].folderUri)
                                        else {
                                            AlbumViewEvent.RemoveFolderFromSelection(album.folders[index].folderUri)
                                        }
                                    )
                                },
                                onFolderViewClick = {
                                    if (album.folders[index].wallpapers.isNotEmpty()) onShowFolderView(album.folders[index].folderName, album.folders[index].wallpapers)
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(150.dp, 350.dp)
                            )
                        }
                    }
                    items(count = album.wallpapers.size, key = { index -> album.wallpapers[index].wallpaperUri }) { index ->
                        if (animate) {
                            WallpaperItem(
                                wallpaperUri = album.wallpapers[index].wallpaperUri,
                                itemSelected = albumState.value.selectedWallpapers.contains(album.wallpapers[index].wallpaperUri),
                                selectionMode = selectionMode,
                                onActivateSelectionMode = { selectionMode = it },
                                onItemSelection = {
                                    albumViewScreenViewModel.onEvent(
                                        if (!albumState.value.selectedWallpapers.contains(album.wallpapers[index].wallpaperUri))
                                            AlbumViewEvent.SelectWallpaper(album.wallpapers[index].wallpaperUri)
                                        else {
                                            AlbumViewEvent.RemoveWallpaperFromSelection(album.wallpapers[index].wallpaperUri)
                                        }
                                    )
                                },
                                onWallpaperViewClick = {
                                    onShowWallpaperView(album.wallpapers[index].wallpaperUri)
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(150.dp, 350.dp)
                                    .animateItem(
                                        placementSpec = tween(
                                            durationMillis = 800,
                                            delayMillis = 0,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                            )
                        }
                        else {
                            WallpaperItem(
                                wallpaperUri = album.wallpapers[index].wallpaperUri,
                                itemSelected = albumState.value.selectedWallpapers.contains(album.wallpapers[index].wallpaperUri),
                                selectionMode = selectionMode,
                                onActivateSelectionMode = { selectionMode = it },
                                onItemSelection = {
                                    albumViewScreenViewModel.onEvent(
                                        if (!albumState.value.selectedWallpapers.contains(album.wallpapers[index].wallpaperUri))
                                            AlbumViewEvent.SelectWallpaper(album.wallpapers[index].wallpaperUri)
                                        else {
                                            AlbumViewEvent.RemoveWallpaperFromSelection(album.wallpapers[index].wallpaperUri)
                                        }
                                    )
                                },
                                onWallpaperViewClick = {
                                    onShowWallpaperView(album.wallpapers[index].wallpaperUri)
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(150.dp, 350.dp)
                            )
                        }
                    }
                }

            }
        }
    )
}