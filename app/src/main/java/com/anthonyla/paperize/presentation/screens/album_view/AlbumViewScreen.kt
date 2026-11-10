package com.anthonyla.paperize.presentation.screens.album_view

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.R
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.presentation.common.components.AddAlbumAnimatedFab
import com.anthonyla.paperize.presentation.common.components.ConfirmDialog
import com.anthonyla.paperize.presentation.common.components.EmptyCollection
import com.anthonyla.paperize.presentation.common.components.SnackbarMessageEffect
import com.anthonyla.paperize.presentation.common.components.SortBottomSheet
import com.anthonyla.paperize.presentation.common.components.SortOption
import com.anthonyla.paperize.presentation.common.components.WallpaperGrid
import com.anthonyla.paperize.presentation.common.components.WallpaperItem
import com.anthonyla.paperize.presentation.common.components.wallpaperGridItem
import com.anthonyla.paperize.presentation.screens.album_view.components.AlbumViewTopBar
import com.anthonyla.paperize.presentation.screens.album_view.components.FolderItem
import com.anthonyla.paperize.presentation.screens.album_view.components.ImportProgressDialog
import kotlinx.coroutines.flow.drop

@Composable
fun AlbumViewScreen(
    onBackClick: () -> Unit,
    onNavigateToFolder: (String) -> Unit,
    onNavigateToWallpaperView: (Wallpaper) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumViewViewModel = hiltViewModel()
) {
    val album by viewModel.album.collectAsStateWithLifecycle()
    val folders = album?.folders.orEmpty()
    val wallpapers = album?.wallpapers.orEmpty()

    val selectedWallpapers by viewModel.selectedWallpapers.collectAsStateWithLifecycle()
    val selectedFolders by viewModel.selectedFolders.collectAsStateWithLifecycle()
    val isSelectionMode = selectedWallpapers.isNotEmpty() || selectedFolders.isNotEmpty()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()
    val albumDeleted by viewModel.albumDeleted.collectAsStateWithLifecycle()
    var showDeleteAlbumDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    SnackbarMessageEffect(message, snackbarHostState, viewModel::dismissMessage)
    LaunchedEffect(viewModel) {
        // drop(1): the current value must not close a dialog restored after rotation.
        viewModel.isDeleting.drop(1).collect { deleting -> if (!deleting) showDeleteAlbumDialog = false }
    }
    LaunchedEffect(albumDeleted) {
        if (albumDeleted) onBackClick()
    }
    val selectedCount = selectedWallpapers.size + selectedFolders.size

    val totalItemsCount = wallpapers.size + folders.size
    val allSelected = selectedCount == totalItemsCount && totalItemsCount > 0

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteSelectedDialog by rememberSaveable { mutableStateOf(false) }
    var sortOption by rememberSaveable { mutableStateOf(SortOption.DATE_ADDED_DESC) }

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    val sortedFolders = remember(folders, sortOption) {
        folders.sortedWith(sortOption.folderComparator)
    }

    val sortedWallpapers = remember(wallpapers, sortOption) {
        wallpapers.sortedWith(sortOption.wallpaperComparator)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        viewModel.addWallpapers(uris.map { it.toString() })
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.addFolder(it.toString()) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AlbumViewTopBar(
                title = album?.name ?: "",
                isSelectionMode = isSelectionMode,
                selectedCount = selectedCount,
                allSelected = allSelected,
                onBackClick = onBackClick,
                onSortClick = { showSortSheet = true },
                onDeleteAlbum = { showDeleteAlbumDialog = true },
                onSelectAll = { if (allSelected) viewModel.clearSelection() else viewModel.selectAll() },
                onDeleteSelected = { if (!isDeleting) showDeleteSelectedDialog = true },
                onClearSelection = { viewModel.clearSelection() }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode && totalItemsCount > 0) {
                AddAlbumAnimatedFab(
                    isLoading = importProgress !is ImportProgress.Idle,
                    onImageClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                    onFolderClick = { folderPickerLauncher.launch(null) }
                )
            }
        }
    ) { paddingValues ->
        if (album != null && totalItemsCount == 0) {
            EmptyCollection(
                title = stringResource(R.string.folder_empty_title),
                hint = stringResource(R.string.album_empty_hint),
                modifier = modifier.padding(paddingValues)
            ) {
                FilledTonalButton(onClick = { imagePickerLauncher.launch(arrayOf("image/*")) }) {
                    Text(stringResource(R.string.add_wallpapers))
                }
                TextButton(onClick = { folderPickerLauncher.launch(null) }) {
                    Text(stringResource(R.string.add_folder))
                }
            }
            return@Scaffold
        }
        WallpaperGrid(modifier = modifier.padding(paddingValues)) {
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
                    modifier = Modifier.wallpaperGridItem(this)
                )
            }

            items(
                items = sortedWallpapers,
                key = { wallpaper -> "wallpaper-${wallpaper.id}" }
            ) { wallpaper ->
                WallpaperItem(
                    wallpaperUri = wallpaper.uri,
                    wallpaperName = wallpaper.displayFileName,
                    isSelected = wallpaper.id in selectedWallpapers,
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        if (isSelectionMode) {
                            viewModel.toggleWallpaperSelection(wallpaper.id)
                        } else {
                            onNavigateToWallpaperView(wallpaper)
                        }
                    },
                    onLongClick = {
                        viewModel.toggleWallpaperSelection(wallpaper.id)
                    },
                    modifier = Modifier.wallpaperGridItem(this)
                )
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            selectedOption = sortOption,
            onSortSelected = { sortOption = it },
            onDismiss = { showSortSheet = false }
        )
    }

    ImportProgressDialog(progress = importProgress, onCancel = viewModel::cancelImport)

    if (showDeleteSelectedDialog) {
        ConfirmDialog(
            title = stringResource(R.string.remove_selected_title, selectedCount),
            text = stringResource(R.string.remove_selected_hint),
            onConfirm = {
                showDeleteSelectedDialog = false
                viewModel.deleteSelected()
            },
            onDismiss = { showDeleteSelectedDialog = false }
        )
    }

    if (showDeleteAlbumDialog) {
        ConfirmDialog(
            title = stringResource(R.string.delete_album_question),
            text = stringResource(R.string.are_you_sure_you_want_to_delete_this),
            onConfirm = viewModel::deleteAlbum,
            onDismiss = { showDeleteAlbumDialog = false },
            confirmLabel = stringResource(if (isDeleting) R.string.deleting_album else R.string.confirm),
            enabled = !isDeleting
        )
    }
}
