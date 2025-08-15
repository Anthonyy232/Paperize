package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
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
    onShowWallpaperView: (String, String) -> Unit,
    onShowFolderView: (Folder) -> Unit,
    onSortClick: (List<Folder>, List<Wallpaper>) -> Unit,
    animate: Boolean // This likely controls item animation, similar to AlbumViewScreen
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val addAlbumState by addAlbumViewModel.state.collectAsStateWithLifecycle() // Use by for direct access
    val lazyListState = rememberLazyGridState()
    var showSpotlight by rememberSaveable { mutableStateOf(false) }

    // Remember scrollbar settings for consistency and performance
    val colorScheme = MaterialTheme.colorScheme
    val scrollbarSettings = remember {
        ScrollbarSettings.Default.copy(
            thumbUnselectedColor = colorScheme.primary,
            thumbSelectedColor = colorScheme.primary,
            thumbShape = RoundedCornerShape(16.dp),
            scrollbarPadding = 1.dp,
        )
    }

    // Remember common modifier for grid items for consistency and responsiveness
    val commonItemModifier = remember {
        Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f) // Common aspect ratio for wallpapers/photos
    }

    BackHandler(
        enabled = addAlbumState.selectionState.selectedCount > 0,
        onBack = remember(addAlbumState.isLoading, addAlbumViewModel) { // Remember the lambda
            {
                if (!addAlbumState.isLoading) {
                    addAlbumViewModel.onEvent(AddAlbumEvent.DeselectAll)
                }
            }
        }
    )

    /** Image picker **/
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri> ->
            scope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    addAlbumViewModel.onEvent(AddAlbumEvent.SetLoading(true))
                }
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                val uriList = uris.mapNotNull { uri ->
                    try {
                        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                        uri.toString()
                    } catch (e: Exception) {
                        Log.e("AddAlbumScreen", "Failed to take persistable URI permission for image: ${e.message}")
                        null
                    }
                }
                withContext(Dispatchers.Main) {
                    if (uriList.isNotEmpty()) {
                        addAlbumViewModel.onEvent(AddAlbumEvent.AddWallpapers(uriList))
                    }
                    addAlbumViewModel.onEvent(AddAlbumEvent.SetLoading(false))
                }
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
                    try {
                        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                        withContext(Dispatchers.Main) {
                            addAlbumViewModel.onEvent(AddAlbumEvent.AddFolder(uri.toString()))
                        }
                    } catch (e: Exception) {
                        Log.e("AddAlbumScreen", "Failed to take persistable URI permission: ${e.message}")
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = {
            val onBackClickTopBar = remember(addAlbumViewModel, onBackClick) {
                {
                    showSpotlight = false
                    addAlbumViewModel.onEvent(AddAlbumEvent.Reset)
                    onBackClick()
                }
            }

            val onConfirmationClickTopBar = remember(addAlbumState.isLoading, addAlbumViewModel, initialAlbumName, onConfirmation) {
                { text: String ->
                    if (!addAlbumState.isLoading) {
                        showSpotlight = false
                        addAlbumViewModel.onEvent(AddAlbumEvent.SaveAlbum(text))
                        onConfirmation()
                    }
                }
            }

            val onSelectAllClickTopBar = remember(addAlbumState.isLoading, addAlbumState.selectionState.allSelected, addAlbumViewModel) {
                {
                    if (!addAlbumState.isLoading) {
                        if (!addAlbumState.selectionState.allSelected) {
                            addAlbumViewModel.onEvent(AddAlbumEvent.SelectAll)
                        } else {
                            addAlbumViewModel.onEvent(AddAlbumEvent.DeselectAll)
                        }
                    }
                }
            }

            val onDeleteSelectedTopBar = remember(addAlbumState.isLoading, addAlbumViewModel) {
                {
                    if (!addAlbumState.isLoading) {
                        addAlbumViewModel.onEvent(AddAlbumEvent.DeleteSelected)
                    }
                }
            }

            val onSortClickTopBar = remember(addAlbumState.isLoading, addAlbumViewModel, addAlbumState.folders, addAlbumState.wallpapers, onSortClick) {
                {
                    if (!addAlbumState.isLoading) {
                        addAlbumViewModel.onEvent(AddAlbumEvent.DeselectAll)
                        onSortClick(addAlbumState.folders, addAlbumState.wallpapers)
                    }
                }
            }

            AddAlbumSmallTopBar(
                title = initialAlbumName,
                allSelected = addAlbumState.selectionState.allSelected,
                selectedCount = addAlbumState.selectionState.selectedCount,
                isEmpty = addAlbumState.isEmpty,
                selectionMode = addAlbumState.selectionState.selectedCount > 0,
                showSpotlight = showSpotlight,
                isLoad = addAlbumState.isLoading,
                onBackClick = onBackClickTopBar,
                onConfirmationClick = onConfirmationClickTopBar,
                onSelectAllClick = onSelectAllClickTopBar,
                onDeleteSelected = onDeleteSelectedTopBar,
                onSortClick = onSortClickTopBar
            )
        },
        floatingActionButton = {
            // Remember FAB callbacks
            val onImageClickFab = remember(addAlbumViewModel, imagePickerLauncher) {
                {
                    addAlbumViewModel.onEvent(AddAlbumEvent.DeselectAll)
                    imagePickerLauncher.launch(arrayOf("image/*"))
                }
            }
            val onFolderClickFab = remember(addAlbumViewModel, folderPickerLauncher) {
                {
                    addAlbumViewModel.onEvent(AddAlbumEvent.DeselectAll)
                    folderPickerLauncher.launch(null)
                }
            }

            AddAlbumAnimatedFab(
                isLoading = addAlbumState.isLoading,
                animate = animate,
                onImageClick = onImageClickFab,
                onFolderClick = onFolderClickFab
            )
        },
        bottomBar = {
            if (addAlbumState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        content = { paddingValues -> // Use paddingValues from Scaffold
            LazyVerticalGridScrollbar(
                state = lazyListState,
                settings = scrollbarSettings, // Use remembered settings
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), // Apply Scaffold padding
            ) {
                LazyVerticalGrid(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(4.dp), // Apply padding to grid content
                    horizontalArrangement = Arrangement.spacedBy(4.dp), // Use spacedBy for consistent spacing
                    verticalArrangement = Arrangement.spacedBy(4.dp), // Use spacedBy for consistent spacing
                ) {
                    items(
                        items = addAlbumState.folders,
                        key = { folder -> folder.folderUri }
                    ) { folder -> // Renamed 'it' to 'folder' for clarity
                        val isSelected = addAlbumState.selectionState.selectedFolders.contains(folder.folderUri)
                        val onFolderItemSelection = remember(folder.folderUri, addAlbumState.isLoading, addAlbumViewModel, isSelected) {
                            {
                                if (!addAlbumState.isLoading) {
                                    if (!isSelected) {
                                        addAlbumViewModel.onEvent(AddAlbumEvent.SelectFolder(folder.folderUri))
                                    } else {
                                        addAlbumViewModel.onEvent(AddAlbumEvent.DeselectFolder(folder.folderUri))
                                    }
                                }
                            }
                        }

                        val onFolderViewClickItem = remember(folder, addAlbumState.isLoading, onShowFolderView) {
                            {
                                if (!addAlbumState.isLoading) {
                                    onShowFolderView(folder)
                                }
                            }
                        }

                        FolderItem(
                            folder = folder,
                            itemSelected = isSelected,
                            selectionMode = addAlbumState.selectionState.selectedCount > 0,
                            onItemSelection = onFolderItemSelection,
                            onFolderViewClick = onFolderViewClickItem,
                            modifier = commonItemModifier // Use common responsive modifier
                                .then(
                                    if (animate) Modifier.animateItem( // Add item animation
                                        placementSpec = tween(
                                            durationMillis = 800,
                                            delayMillis = 0,
                                            easing = FastOutSlowInEasing
                                        )
                                    ) else Modifier
                                )
                        )
                    }
                    items(
                        items = addAlbumState.wallpapers,
                        key = { wallpaper -> wallpaper.wallpaperUri }
                    ) { wallpaper -> // Renamed 'it' to 'wallpaper' for clarity
                        val isSelected = addAlbumState.selectionState.selectedWallpapers.contains(wallpaper.wallpaperUri)
                        val onWallpaperItemSelection = remember(wallpaper.wallpaperUri, addAlbumState.isLoading, addAlbumViewModel, isSelected) {
                            {
                                if (!addAlbumState.isLoading) {
                                    if (!isSelected) {
                                        addAlbumViewModel.onEvent(AddAlbumEvent.SelectWallpaper(wallpaper.wallpaperUri))
                                    } else {
                                        addAlbumViewModel.onEvent(AddAlbumEvent.DeselectWallpaper(wallpaper.wallpaperUri))
                                    }
                                }
                            }
                        }

                        val onWallpaperViewClickItem = remember(wallpaper, addAlbumState.isLoading, onShowWallpaperView) {
                            {
                                if (!addAlbumState.isLoading) {
                                    onShowWallpaperView(wallpaper.wallpaperUri, wallpaper.fileName)
                                }
                            }
                        }

                        WallpaperItem(
                            wallpaperUri = wallpaper.wallpaperUri,
                            itemSelected = isSelected,
                            selectionMode = addAlbumState.selectionState.selectedCount > 0,
                            onItemSelection = onWallpaperItemSelection,
                            onWallpaperViewClick = onWallpaperViewClickItem,
                            modifier = commonItemModifier // Use common responsive modifier
                                .then(
                                    if (animate) Modifier.animateItem( // Add item animation
                                        placementSpec = tween(
                                            durationMillis = 800,
                                            delayMillis = 0,
                                            easing = FastOutSlowInEasing
                                        )
                                    ) else Modifier
                                )
                        )
                    }
                }
            }
        }
    )
}