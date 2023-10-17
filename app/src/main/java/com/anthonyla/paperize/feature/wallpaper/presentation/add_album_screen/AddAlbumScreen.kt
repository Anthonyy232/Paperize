package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen

import android.content.Context
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
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumAnimatedFab
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumFabMenuOptions
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumSmallTopBar
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.FolderItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem

@Composable
fun AddAlbumScreen(
    initialAlbumName: String,
    viewModel: AddAlbumViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onConfirmation: () -> Unit,
    ) {
    viewModel.onEvent(AddAlbumEvent.SetAlbumName(initialAlbumName))

    val context = LocalContext.current
    val lazyListState = rememberLazyStaggeredGridState()
    val state = viewModel.state.collectAsStateWithLifecycle()
    var selectionMode by rememberSaveable { mutableStateOf(false) }

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
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                viewModel.onEvent(
                    AddAlbumEvent.AddWallpaper(wallpaperUri = uri.toString())
                )
            }
        }
    )

    /** Folder picker **/
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            val takeFlags: Int =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                viewModel.onEvent(
                    AddAlbumEvent.AddFolder(directoryUri = uri.toString())
                )
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
                    onConfirmation()
                },
                onSelectAllClick = {
                    if (!state.value.allSelected) viewModel.onEvent(AddAlbumEvent.SelectAll)
                    else viewModel.onEvent(AddAlbumEvent.DeselectAll)
                },
                onDeleteSelected = {
                    selectionMode = false
                    viewModel.onEvent(AddAlbumEvent.DeselectAll)
                    state.value.selectedFolders.forEach { folder ->
                        viewModel.onEvent(AddAlbumEvent.DeleteFolder(folder))
                    }
                    state.value.selectedWallpapers.forEach { wallpaper ->
                        viewModel.onEvent(AddAlbumEvent.DeleteWallpaper(wallpaper))
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible =
                !lazyListState.isScrollInProgress || lazyListState.firstVisibleItemScrollOffset < 0,
                enter = scaleIn(tween(400, 50, FastOutSlowInEasing)),
                exit = scaleOut(tween(400, 50, FastOutSlowInEasing)),
            ) {
                AddAlbumAnimatedFab(
                    menuOptions = AddAlbumFabMenuOptions(),
                    onImageClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                    onFolderClick = { folderPickerLauncher.launch(null) }
                )
            }
        },
        content = {
            LazyVerticalStaggeredGrid(
                state = lazyListState,
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp, 4.dp),
                horizontalArrangement = Arrangement.Start,
                content = {
                    if (state.value.folders.isNotEmpty()) {
                        items (items = state.value.folders, key = { folder -> folder.folderUri}
                        ) { folder ->
                            FolderItem(
                                folder = folder,
                                itemSelected = state.value.selectedFolders.contains(folder.folderUri),
                                selectionMode = selectionMode,
                                onActivateSelectionMode = { selectionMode = it },
                                onItemSelection = {
                                    if (!state.value.selectedFolders.contains(folder.folderUri)) {
                                        viewModel.onEvent(AddAlbumEvent.SelectFolder(folder.folderUri))
                                    } else {
                                        viewModel.onEvent(AddAlbumEvent.RemoveFolderFromSelection(folder.folderUri))
                                    } },
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                    if (state.value.wallpapers.isNotEmpty()) {
                        items (items = state.value.wallpapers, key = { wallpaper -> wallpaper.wallpaperUri}
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
                                        else
                                            AddAlbumEvent.RemoveWallpaperFromSelection(wallpaper.wallpaperUri)
                                    )
                                },
                                onImageViewClick = {

                                },
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            )
        }
    )
}

fun getWallpaperFromFolder(folderUri: String, context: Context): List<String> {
    val folderDocumentFile = DocumentFile.fromTreeUri(context, folderUri.toUri())
    return folderDocumentFile?.listFiles()?.map { it.uri.toString() } ?: emptyList()
}
fun getFolderNameFromUri(folderUri: String, context: Context): String? {
    return DocumentFile.fromTreeUri(context, folderUri.toUri())?.name
}