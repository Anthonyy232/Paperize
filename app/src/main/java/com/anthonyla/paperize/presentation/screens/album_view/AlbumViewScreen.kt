package com.anthonyla.paperize.presentation.screens.album_view

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.presentation.common.components.AddAlbumAnimatedFab
import com.anthonyla.paperize.presentation.screens.album_view.components.AlbumViewTopBar
import com.anthonyla.paperize.presentation.screens.album_view.components.FolderItem
import com.anthonyla.paperize.presentation.screens.album_view.components.SortBottomSheet
import com.anthonyla.paperize.presentation.screens.album_view.components.SortOption
import com.anthonyla.paperize.presentation.screens.album_view.components.WallpaperItem
import com.anthonyla.paperize.presentation.theme.AppGrid
import com.anthonyla.paperize.presentation.theme.AppSpacing

@Composable
fun AlbumViewScreen(
    @Suppress("UNUSED_PARAMETER") albumId: String,
    onBackClick: () -> Unit,
    onNavigateToFolder: (String) -> Unit,
    onNavigateToWallpaperView: (String, String) -> Unit,
    onNavigateToSort: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumViewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyGridState()

    val album by viewModel.album.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val wallpapers by viewModel.wallpapers.collectAsState()

    // Selection state
    val selectedWallpapers by viewModel.selectedWallpapers.collectAsState()
    val selectedFolders by viewModel.selectedFolders.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedCount = selectedWallpapers.size + selectedFolders.size

    // Check if all items are selected
    val totalItemsCount = wallpapers.size + folders.size
    val allSelected = selectedCount == totalItemsCount && totalItemsCount > 0

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var sortOption by rememberSaveable { mutableStateOf(SortOption.DATE_ADDED_DESC) }

    // Handle back press when in selection mode
    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    // Sort wallpapers and folders
    val sortedFolders = remember(folders, sortOption) {
        when (sortOption) {
            SortOption.NAME_ASC -> folders.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> folders.sortedByDescending { it.name.lowercase() }
            SortOption.DATE_ADDED_ASC -> folders.sortedBy { it.addedAt }
            SortOption.DATE_ADDED_DESC -> folders.sortedByDescending { it.addedAt }
            SortOption.DATE_MODIFIED_ASC -> folders.sortedBy { it.dateModified }
            SortOption.DATE_MODIFIED_DESC -> folders.sortedByDescending { it.dateModified }
        }
    }

    val sortedWallpapers = remember(wallpapers, sortOption) {
        when (sortOption) {
            SortOption.NAME_ASC -> wallpapers.sortedBy { it.fileName.lowercase() }
            SortOption.NAME_DESC -> wallpapers.sortedByDescending { it.fileName.lowercase() }
            SortOption.DATE_ADDED_ASC -> wallpapers.sortedBy { it.addedAt }
            SortOption.DATE_ADDED_DESC -> wallpapers.sortedByDescending { it.addedAt }
            SortOption.DATE_MODIFIED_ASC -> wallpapers.sortedBy { it.dateModified }
            SortOption.DATE_MODIFIED_DESC -> wallpapers.sortedByDescending { it.dateModified }
        }
    }

    val commonItemModifier = remember {
        Modifier
            .fillMaxWidth()
            .aspectRatio(Constants.WALLPAPER_ASPECT_RATIO)
    }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {
                    // Handle permission failure
                }
            }
            viewModel.addWallpapers(uris.map { it.toString() })
        }
    }

    // Folder picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
                viewModel.addFolder(it.toString())
            } catch (e: Exception) {
                // Handle permission failure
            }
        }
    }

    Scaffold(
        topBar = {
            AlbumViewTopBar(
                title = album?.name ?: "",
                isSelectionMode = isSelectionMode,
                selectedCount = selectedCount,
                allSelected = allSelected,
                onBackClick = onBackClick,
                onSortClick = onNavigateToSort,
                onDeleteAlbum = { viewModel.deleteAlbum(); onBackClick() },
                onSelectAll = { if (allSelected) viewModel.clearSelection() else viewModel.selectAll() },
                onDeleteSelected = { viewModel.deleteSelected() },
                onClearSelection = { viewModel.clearSelection() }
            )
        },
        floatingActionButton = {
            // Hide FAB when in selection mode
            if (!isSelectionMode) {
                AddAlbumAnimatedFab(
                    isLoading = false,
                    onImageClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                    onFolderClick = { folderPickerLauncher.launch(null) }
                )
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            state = lazyListState,
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            columns = GridCells.Adaptive(AppGrid.itemMinSize),
            contentPadding = PaddingValues(AppSpacing.gridPadding),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.gridSpacing),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.gridSpacing)
        ) {
            // Folders
            items(
                items = sortedFolders,
                key = { folder -> "folder-${folder.id}" }
            ) { folder ->
                FolderItem(
                    folder = folder,
                    isSelected = folder.id in selectedFolders,
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        if (isSelectionMode) {
                            viewModel.toggleFolderSelection(folder.id)
                        } else {
                            onNavigateToFolder(folder.id)
                        }
                    },
                    onLongClick = {
                        viewModel.toggleFolderSelection(folder.id)
                    },
                    modifier = commonItemModifier
                        .then(
                            Modifier.animateItem(
                                placementSpec = tween(
                                    durationMillis = Constants.ANIMATION_DURATION_LONG_MS,
                                    delayMillis = 0,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        )
                )
            }

            // Wallpapers
            items(
                items = sortedWallpapers,
                key = { wallpaper -> "wallpaper-${wallpaper.id}" }
            ) { wallpaper ->
                WallpaperItem(
                    wallpaperUri = wallpaper.uri,
                    isSelected = wallpaper.id in selectedWallpapers,
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        if (isSelectionMode) {
                            viewModel.toggleWallpaperSelection(wallpaper.id)
                        } else {
                            onNavigateToWallpaperView(wallpaper.uri, wallpaper.fileName)
                        }
                    },
                    onLongClick = {
                        viewModel.toggleWallpaperSelection(wallpaper.id)
                    },
                    modifier = commonItemModifier
                        .then(
                            Modifier.animateItem(
                                placementSpec = tween(
                                    durationMillis = Constants.ANIMATION_DURATION_LONG_MS,
                                    delayMillis = 0,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        )
                )
            }
        }
    }

    // Sort Bottom Sheet
    if (showSortSheet) {
        SortBottomSheet(
            currentSort = sortOption,
            onSortSelected = { sortOption = it },
            onDismiss = { showSortSheet = false }
        )
    }
}
