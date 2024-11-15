package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumAnimatedFab
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumSmallTopBar
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.FolderItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

@Composable
fun AddAlbumScreen(
    addAlbumViewModel: AddAlbumViewModel,
    initialAlbumName: String,
    onBackClick: () -> Unit,
    onConfirmation: () -> Unit,
    onShowWallpaperView: (String) -> Unit,
    onShowFolderView: (Folder) -> Unit,
    onSortClick: (List<Folder>, List<Wallpaper>)  -> Unit,
    animate: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val addAlbumState = addAlbumViewModel.state.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyGridState()
    var showSpotlight by rememberSaveable { mutableStateOf(false) }

    BackHandler(
        enabled = addAlbumState.value.selectionState.selectedCount > 0,
        onBack = {
            if (!addAlbumState.value.isLoading) {
                addAlbumViewModel.onEvent(AddAlbumEvent.DeselectAll)
            }
        }
    )

    /** Image picker **/
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri> ->
            scope.launch(Dispatchers.IO) {
                addAlbumViewModel.onEvent(AddAlbumEvent.SetLoading(true))
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                val uriList = uris.mapNotNull { uri ->
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    val persistedUriPermissions = context.contentResolver.persistedUriPermissions
                    if (persistedUriPermissions.any { it.uri == uri }) uri.toString() else null
                }
                if (uriList.isNotEmpty()) {
                    addAlbumViewModel.onEvent(AddAlbumEvent.AddWallpapers(uriList))
                }
                addAlbumViewModel.onEvent(AddAlbumEvent.SetLoading(false))
            }
        }
    )

    /** Folder picker **/
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            scope.launch(Dispatchers.IO) {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                if (uri != null) {
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    val persistedUriPermissions = context.contentResolver.persistedUriPermissions
                    if (persistedUriPermissions.any { it.uri == uri }) {
                        addAlbumViewModel.onEvent(AddAlbumEvent.AddFolder(uri.toString()))
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = {
            AddAlbumSmallTopBar(
                title = initialAlbumName,
                allSelected = addAlbumState.value.selectionState.allSelected,
                selectedCount = addAlbumState.value.selectionState.selectedCount,
                isEmpty = addAlbumState.value.isEmpty,
                selectionMode = addAlbumState.value.selectionState.selectedCount > 0,
                showSpotlight = showSpotlight,
                isLoad = addAlbumState.value.isLoading,
                onBackClick = {
                    showSpotlight = false
                    addAlbumViewModel.onEvent(AddAlbumEvent.Reset)
                    onBackClick()
                },
                onConfirmationClick = {
                    if (!addAlbumState.value.isLoading) {
                        showSpotlight = false
                        addAlbumViewModel.onEvent(AddAlbumEvent.SaveAlbum(initialAlbumName))
                        onConfirmation()
                    }
                },
                onSelectAllClick = {
                    if (!addAlbumState.value.isLoading) {
                        if (!addAlbumState.value.selectionState.allSelected) {
                            addAlbumViewModel.onEvent(AddAlbumEvent.SelectAll)
                        } else {
                            addAlbumViewModel.onEvent(AddAlbumEvent.DeselectAll)
                        }
                    }
                },
                onDeleteSelected = {
                    if (!addAlbumState.value.isLoading) {
                        addAlbumViewModel.onEvent(AddAlbumEvent.DeleteSelected)
                    }
                },
                onSortClick = {
                    if (!addAlbumState.value.isLoading) {
                        addAlbumViewModel.onEvent(AddAlbumEvent.DeselectAll)
                        onSortClick(addAlbumState.value.folders, addAlbumState.value.wallpapers)
                    }
                }
            )
        },
        floatingActionButton = {
            AddAlbumAnimatedFab(
                isLoading = addAlbumState.value.isLoading,
                animate = animate,
                onImageClick = {
                    addAlbumViewModel.onEvent(AddAlbumEvent.DeselectAll)
                    imagePickerLauncher.launch(arrayOf("image/*"))
                },
                onFolderClick = {
                    addAlbumViewModel.onEvent(AddAlbumEvent.DeselectAll)
                    folderPickerLauncher.launch(null)
                }
            )
        },
        bottomBar = {
            if (addAlbumState.value.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
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
                modifier = Modifier.fillMaxSize().padding(it),
            ) {
                LazyVerticalGrid(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(4.dp, 4.dp),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    items(
                        items = addAlbumState.value.folders,
                        key = { folder -> folder.folderUri }
                    ) {
                        FolderItem(
                            folder = it,
                            itemSelected = addAlbumState.value.selectionState.selectedFolders.contains(it.folderUri),
                            selectionMode = addAlbumState.value.selectionState.selectedCount > 0,
                            onItemSelection = {
                                if (!addAlbumState.value.isLoading) {
                                    if (!addAlbumState.value.selectionState.selectedFolders.contains(it.folderUri)) {
                                        addAlbumViewModel.onEvent(AddAlbumEvent.SelectFolder(it.folderUri)) }
                                    else {
                                        addAlbumViewModel.onEvent(
                                            AddAlbumEvent.DeselectFolder(it.folderUri)
                                        )
                                    }
                                }
                            },
                            onFolderViewClick = {
                                if (!addAlbumState.value.isLoading) {
                                    onShowFolderView(it)
                                }
                            },
                            modifier = Modifier
                                .padding(4.dp)
                                .size(150.dp, 350.dp)
                        )
                    }
                    items(
                        items = addAlbumState.value.wallpapers,
                        key = { wallpaper -> wallpaper.wallpaperUri }
                    ) {
                        WallpaperItem(
                            wallpaperUri = it.wallpaperUri,
                            itemSelected = addAlbumState.value.selectionState.selectedWallpapers.contains(it.wallpaperUri),
                            selectionMode = addAlbumState.value.selectionState.selectedCount > 0,
                            onItemSelection = {
                                if (!addAlbumState.value.isLoading) {
                                    if (!addAlbumState.value.selectionState.selectedWallpapers.contains(it.wallpaperUri)) {
                                        addAlbumViewModel.onEvent(AddAlbumEvent.SelectWallpaper(it.wallpaperUri))
                                    }
                                    else {
                                        addAlbumViewModel.onEvent(AddAlbumEvent.DeselectWallpaper(it.wallpaperUri))
                                    }
                                }
                            },
                            onWallpaperViewClick = {
                                if (!addAlbumState.value.isLoading) {
                                    onShowWallpaperView(it.wallpaperUri)
                                }
                            },
                            modifier = Modifier
                                .padding(4.dp)
                                .size(150.dp, 350.dp)
                        )
                    }
                }
            }
        }
    )
}