package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen

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
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumAnimatedFab
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumSmallTopBar
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.FolderItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsViewModel

@Composable
fun AddAlbumScreen(
    addAlbumViewModel: AddAlbumViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    initialAlbumName: String,
    onBackClick: () -> Unit,
    onConfirmation: () -> Unit,
    onShowWallpaperView: (String) -> Unit,
    onShowFolderView: (String?, List<String>) -> Unit
) {
    val context = LocalContext.current
    val lazyGridState = rememberLazyStaggeredGridState()
    val addAlbumState = addAlbumViewModel.state.collectAsStateWithLifecycle()
    val settingsState = settingsViewModel.state.collectAsStateWithLifecycle()
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    if (addAlbumState.value.initialAlbumName.isEmpty()) {
        addAlbumViewModel.onEvent(AddAlbumEvent.SetAlbumName(initialAlbumName))
    }
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
            if (settingsState.value.animate) {
                AnimatedVisibility(
                    visible = !lazyGridState.isScrollInProgress || lazyGridState.firstVisibleItemScrollOffset < 0,
                    enter = scaleIn(tween(400, 50, FastOutSlowInEasing)),
                    exit = scaleOut(tween(400, 50, FastOutSlowInEasing)),
                ) {
                    AddAlbumAnimatedFab(
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
                    onImageClick = {
                        selectionMode = false
                        imagePickerLauncher.launch(arrayOf("image/*"))
                        showSpotlight = true
                    },
                    onFolderClick = {
                        selectionMode = false
                        folderPickerLauncher.launch(null)
                        showSpotlight = true
                    },
                    animate = false
                )
            }
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
                    items (items = addAlbumState.value.folders, key = { folder -> folder.folderUri }
                    ) { folder ->
                        FolderItem(
                            folder = folder,
                            itemSelected = addAlbumState.value.selectedFolders.contains(folder.folderUri),
                            selectionMode = selectionMode,
                            onActivateSelectionMode = { selectionMode = it },
                            onItemSelection = {
                                addAlbumViewModel.onEvent(
                                    if (!addAlbumState.value.selectedFolders.contains(folder.folderUri))
                                        AddAlbumEvent.SelectFolder(folder.folderUri)
                                    else {
                                        AddAlbumEvent.RemoveFolderFromSelection(folder.folderUri)
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
                    items (items = addAlbumState.value.wallpapers, key = { wallpaper -> wallpaper.wallpaperUri }
                    ) { wallpaper ->
                        WallpaperItem(
                            wallpaperUri = wallpaper.wallpaperUri,
                            itemSelected = addAlbumState.value.selectedWallpapers.contains(wallpaper.wallpaperUri),
                            selectionMode = selectionMode,
                            onActivateSelectionMode = { selectionMode = it },
                            onItemSelection = {
                                addAlbumViewModel.onEvent(
                                    if (!addAlbumState.value.selectedWallpapers.contains(wallpaper.wallpaperUri))
                                        AddAlbumEvent.SelectWallpaper(wallpaper.wallpaperUri)
                                    else {
                                        AddAlbumEvent.RemoveWallpaperFromSelection(wallpaper.wallpaperUri)
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
                            ),
                            animate = settingsState.value.animate
                        )
                    }
                }
            )
        }
    )
}