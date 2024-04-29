package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen

import android.content.Intent
import android.net.Uri
import android.util.Log
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
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumAnimatedFab
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumFabMenuOptions
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumSmallTopBar
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.FolderItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem

@Composable
fun AddAlbumScreen(
    viewModel: AddAlbumViewModel = hiltViewModel(),
    initialAlbumName: String,
    onBackClick: () -> Unit,
    onConfirmation: () -> Unit,
    onShowWallpaperView: (String) -> Unit,
    onShowFolderView: (String, String?, List<String>) -> Unit
) {
    val context = LocalContext.current
    val lazyGridState = rememberLazyStaggeredGridState()
    val state = viewModel.state.collectAsStateWithLifecycle()
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    if (state.value.initialAlbumName.isEmpty()) {
        viewModel.onEvent(AddAlbumEvent.SetAlbumName(initialAlbumName))
    }

    BackHandler {
        if (selectionMode) {
            selectionMode = false
            viewModel.onEvent(AddAlbumEvent.DeselectAll)
        } else {
            onBackClick()
            viewModel.onEvent(AddAlbumEvent.ClearState)
        }
    }

    /** Image picker **/
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri> ->
            for (uri in uris) {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                val persistedUriPermissions = context.contentResolver.persistedUriPermissions
                if (persistedUriPermissions.any { it.uri == uri }) {
                    viewModel.onEvent(AddAlbumEvent.AddWallpaper(wallpaperUri = uri.toString()))
                } else {
                    Log.e("AddAlbumScreen", "Failed to persist permission for $uri")
                }
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
                    viewModel.onEvent(AddAlbumEvent.AddFolder(directoryUri = uri.toString()))
                } else {
                    Log.e("AddAlbumScreen", "Failed to persist permission for $uri")
                }
            }
        }
    )

    Scaffold(
        topBar = {
            AddAlbumSmallTopBar(
                title = initialAlbumName,
                isEmpty = state.value.isEmpty,
                selectionMode = selectionMode,
                albumState = viewModel.state,
                onBackClick = {
                    viewModel.onEvent(AddAlbumEvent.ClearState)
                    onBackClick()
                },
                onConfirmationClick = {
                    viewModel.onEvent(AddAlbumEvent.ReflectAlbumName(it))
                    viewModel.onEvent(AddAlbumEvent.SaveAlbum)
                    viewModel.onEvent(AddAlbumEvent.ClearState)
                    onConfirmation()
                },
                onSelectAllClick = {
                    if (!state.value.allSelected) viewModel.onEvent(AddAlbumEvent.SelectAll)
                    else viewModel.onEvent(AddAlbumEvent.DeselectAll)
                },
                onDeleteSelected = {
                    selectionMode = false
                    viewModel.onEvent(AddAlbumEvent.DeleteSelected)
                    viewModel.onEvent(AddAlbumEvent.DeselectAll)
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !lazyGridState.isScrollInProgress || lazyGridState.firstVisibleItemScrollOffset < 0,
                enter = scaleIn(tween(400, 50, FastOutSlowInEasing)),
                exit = scaleOut(tween(400, 50, FastOutSlowInEasing)),
            ) {
            }
            AddAlbumAnimatedFab(
                menuOptions = AddAlbumFabMenuOptions(),
                onImageClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                onFolderClick = { folderPickerLauncher.launch(null) }
            )
        },
        content = {
            LazyVerticalStaggeredGrid(
                state = lazyGridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp, 4.dp),
                horizontalArrangement = Arrangement.Start,
                content = {
                    items (items = state.value.folders, key = { folder -> folder.folderUri.hashCode()}
                    ) { folder ->
                        FolderItem(
                            folder = folder,
                            itemSelected = state.value.selectedFolders.contains(folder.folderUri),
                            selectionMode = selectionMode,
                            onActivateSelectionMode = { selectionMode = it },
                            onItemSelection = {
                                viewModel.onEvent(
                                    if (!state.value.selectedFolders.contains(folder.folderUri))
                                        AddAlbumEvent.SelectFolder(folder.folderUri)
                                    else {
                                        AddAlbumEvent.RemoveFolderFromSelection(folder.folderUri)
                                    }
                                )
                            },
                            onFolderViewClick = {
                                onShowFolderView(folder.folderUri, folder.folderName, folder.wallpapers)
                            },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    items (items = state.value.wallpapers, key = { wallpaper -> wallpaper.wallpaperUri.hashCode()}
                    ) { wallpaper ->
                        WallpaperItem(
                            wallpaperUri = wallpaper.wallpaperUri,
                            itemSelected = state.value.selectedWallpapers.contains(wallpaper.wallpaperUri),
                            selectionMode = selectionMode,
                            onActivateSelectionMode = { selectionMode = it },
                            onItemSelection = {
                                viewModel.onEvent(
                                    if (!state.value.selectedWallpapers.contains(wallpaper.wallpaperUri))
                                        AddAlbumEvent.SelectWallpaper(wallpaper.wallpaperUri)
                                    else {
                                        AddAlbumEvent.RemoveWallpaperFromSelection(wallpaper.wallpaperUri)
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