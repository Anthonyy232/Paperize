package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen

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
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumAnimatedFab
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumSmallTopBar
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.FolderItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

@Composable
fun AddAlbumScreen(
    addAlbumViewModel: AddAlbumViewModel = hiltViewModel(),
    initialAlbumName: String,
    onBackClick: () -> Unit,
    onConfirmation: () -> Unit,
    onShowWallpaperView: (String) -> Unit,
    onShowFolderView: (String?, List<String>) -> Unit,
    animate: Boolean
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyGridState()
    val addAlbumState = addAlbumViewModel.state.collectAsStateWithLifecycle()
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var showSpotlight by rememberSaveable { mutableStateOf(false) }

    BackHandler(
        enabled = selectionMode,
        onBack = {
            addAlbumViewModel.onEvent(AddAlbumEvent.DeselectAll)
            selectionMode = false
        }
    )

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
                addAlbumViewModel.onEvent(AddAlbumEvent.AddWallpapers(uriList))
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
                    addAlbumViewModel.onEvent(AddAlbumEvent.AddFolder(uri.toString()))
                }
            }
        }
    )

    Scaffold(
        topBar = {
            AddAlbumSmallTopBar(
                title = initialAlbumName,
                isEmpty = addAlbumState.value.isEmpty,
                selectionMode = selectionMode,
                albumState = addAlbumViewModel.state,
                showSpotlight = showSpotlight,
                onBackClick = {
                    showSpotlight = false
                    addAlbumViewModel.onEvent(AddAlbumEvent.Reset)
                    onBackClick()
                },
                onConfirmationClick = {
                    showSpotlight = false
                    addAlbumViewModel.onEvent(AddAlbumEvent.ReflectAlbumName(it))
                    addAlbumViewModel.onEvent(AddAlbumEvent.SaveAlbum)
                    addAlbumViewModel.onEvent(AddAlbumEvent.Reset)
                    onConfirmation()
                },
                onSelectAllClick = {
                    if (!addAlbumState.value.allSelected) addAlbumViewModel.onEvent(AddAlbumEvent.SelectAll)
                    else addAlbumViewModel.onEvent(AddAlbumEvent.DeselectAll)
                },
                onDeleteSelected = {
                    selectionMode = false
                    addAlbumViewModel.onEvent(AddAlbumEvent.DeleteSelected)
                    addAlbumViewModel.onEvent(AddAlbumEvent.DeselectAll)
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
        content = {
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
                    items(count = addAlbumState.value.folders.size, key = { index -> addAlbumState.value.folders[index].folderUri }) { index ->
                        if (animate) {
                            FolderItem(
                                folder = addAlbumState.value.folders[index],
                                itemSelected = addAlbumState.value.selectedFolders.contains(addAlbumState.value.folders[index].folderUri),
                                selectionMode = selectionMode,
                                onActivateSelectionMode = { selectionMode = it },
                                onItemSelection = {
                                    addAlbumViewModel.onEvent(
                                        if (!addAlbumState.value.selectedFolders.contains(addAlbumState.value.folders[index].folderUri))
                                            AddAlbumEvent.SelectFolder(addAlbumState.value.folders[index].folderUri)
                                        else {
                                            AddAlbumEvent.RemoveFolderFromSelection(addAlbumState.value.folders[index].folderUri)
                                        }
                                    )
                                },
                                onFolderViewClick = {
                                    if (addAlbumState.value.folders[index].wallpapers.isNotEmpty()) onShowFolderView(addAlbumState.value.folders[index].folderName, addAlbumState.value.folders[index].wallpapers)
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
                                folder = addAlbumState.value.folders[index],
                                itemSelected = addAlbumState.value.selectedFolders.contains(addAlbumState.value.folders[index].folderUri),
                                selectionMode = selectionMode,
                                onActivateSelectionMode = { selectionMode = it },
                                onItemSelection = {
                                    addAlbumViewModel.onEvent(
                                        if (!addAlbumState.value.selectedFolders.contains(addAlbumState.value.folders[index].folderUri))
                                            AddAlbumEvent.SelectFolder(addAlbumState.value.folders[index].folderUri)
                                        else {
                                            AddAlbumEvent.RemoveFolderFromSelection(addAlbumState.value.folders[index].folderUri)
                                        }
                                    )
                                },
                                onFolderViewClick = {
                                    if (addAlbumState.value.folders[index].wallpapers.isNotEmpty()) onShowFolderView(addAlbumState.value.folders[index].folderName, addAlbumState.value.folders[index].wallpapers)
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(150.dp, 350.dp)
                            )
                        }
                    }
                    items(count = addAlbumState.value.wallpapers.size, key = { index -> addAlbumState.value.wallpapers[index].wallpaperUri }) { index ->
                        if (animate) {
                            WallpaperItem(
                                wallpaperUri = addAlbumState.value.wallpapers[index].wallpaperUri,
                                itemSelected = addAlbumState.value.selectedWallpapers.contains(addAlbumState.value.wallpapers[index].wallpaperUri),
                                selectionMode = selectionMode,
                                onActivateSelectionMode = { selectionMode = it },
                                onItemSelection = {
                                    addAlbumViewModel.onEvent(
                                        if (!addAlbumState.value.selectedWallpapers.contains(addAlbumState.value.wallpapers[index].wallpaperUri))
                                            AddAlbumEvent.SelectWallpaper(addAlbumState.value.wallpapers[index].wallpaperUri)
                                        else {
                                            AddAlbumEvent.RemoveWallpaperFromSelection(addAlbumState.value.wallpapers[index].wallpaperUri)
                                        }
                                    )
                                },
                                onWallpaperViewClick = {
                                    onShowWallpaperView(addAlbumState.value.wallpapers[index].wallpaperUri)
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
                                wallpaperUri = addAlbumState.value.wallpapers[index].wallpaperUri,
                                itemSelected = addAlbumState.value.selectedWallpapers.contains(addAlbumState.value.wallpapers[index].wallpaperUri),
                                selectionMode = selectionMode,
                                onActivateSelectionMode = { selectionMode = it },
                                onItemSelection = {
                                },
                                onWallpaperViewClick = {
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