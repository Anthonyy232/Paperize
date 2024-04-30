package com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumAnimatedFab
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumFabMenuOptions
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.FolderItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.components.AlbumViewTopBar
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsViewModel

@Composable
fun AlbumViewScreen(
    albumViewScreenViewModel: AlbumViewScreenViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    album: AlbumWithWallpaperAndFolder,
    onBackClick: () -> Unit,
    onShowWallpaperView: (String) -> Unit,
    onShowFolderView: (String?, List<String>) -> Unit,
    onDeleteAlbum: () -> Unit,
    onTitleChange: (String, AlbumWithWallpaperAndFolder) -> Unit,
    onSelectionDeleted: () -> Unit
) {
    albumViewScreenViewModel.onEvent(AlbumViewEvent.SetSize(album.wallpapers.size + album.folders.size)) // For selectedAll state
    val lazyListState = rememberLazyStaggeredGridState()
    val albumState = albumViewScreenViewModel.state.collectAsStateWithLifecycle()
    val settingsState = settingsViewModel.state.collectAsStateWithLifecycle()
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

    BackHandler {
        if (selectionMode) {
            selectionMode = false
            albumViewScreenViewModel.onEvent(AlbumViewEvent.DeselectAll)
        } else {
            onBackClick()
            albumViewScreenViewModel.onEvent(AlbumViewEvent.ClearState)
        }
    }

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
                onTitleChange = { onTitleChange(it, album) },
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
            if (settingsState.value.animate) {
                AnimatedVisibility(
                    visible = !lazyListState.isScrollInProgress || lazyListState.firstVisibleItemScrollOffset < 0,
                    enter = scaleIn(tween(400, 50, FastOutSlowInEasing)),
                    exit = scaleOut(tween(400, 50, FastOutSlowInEasing)),
                ) {
                    AddAlbumAnimatedFab(
                        menuOptions = AddAlbumFabMenuOptions(),
                        onImageClick = {
                            selectionMode = false
                            imagePickerLauncher.launch(arrayOf("image/*"))
                        },
                        onFolderClick = {
                            selectionMode = false
                            folderPickerLauncher.launch(null)
                        },
                        animate = true
                    )
                }
            }
            else {
                AddAlbumAnimatedFab(
                    menuOptions = AddAlbumFabMenuOptions(),
                    onImageClick = {
                        selectionMode = false
                        imagePickerLauncher.launch(arrayOf("image/*"))
                    },
                    onFolderClick = {
                        selectionMode = false
                        folderPickerLauncher.launch(null)
                    },
                    animate = true
                )
            }
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
                                albumViewScreenViewModel.onEvent(
                                    if (!albumState.value.selectedFolders.contains(folder.folderUri))
                                        AlbumViewEvent.SelectFolder(folder.folderUri)
                                    else {
                                        AlbumViewEvent.RemoveFolderFromSelection(folder.folderUri)
                                    }
                                )
                            },
                            onFolderViewClick = {
                                onShowFolderView(folder.folderName, folder.wallpapers)
                            },
                            modifier = Modifier.padding(4.dp).animateItem(
                                placementSpec = tween(
                                    durationMillis = 800,
                                    delayMillis = 0,
                                    easing = FastOutSlowInEasing
                                ),
                            )
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
                                albumViewScreenViewModel.onEvent(
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
                            modifier = Modifier.padding(4.dp).animateItem(
                                placementSpec = tween(
                                    durationMillis = 800,
                                    delayMillis = 0,
                                    easing = FastOutSlowInEasing
                                ),
                            )
                        )
                    }
                }
            )
        }
    )
}