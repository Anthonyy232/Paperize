package com.anthonyla.paperize.presentation.screens.folder_view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
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
import com.anthonyla.paperize.presentation.common.components.BackTopAppBar
import com.anthonyla.paperize.presentation.common.components.EmptyCollection
import com.anthonyla.paperize.presentation.common.components.SnackbarMessageEffect
import com.anthonyla.paperize.presentation.common.components.SortBottomSheet
import com.anthonyla.paperize.presentation.common.components.SortOption
import com.anthonyla.paperize.presentation.common.components.WallpaperGrid
import com.anthonyla.paperize.presentation.common.components.WallpaperItem
import com.anthonyla.paperize.presentation.common.components.wallpaperGridItem

@Composable
fun FolderViewScreen(
    onBackClick: () -> Unit,
    onNavigateToWallpaperView: (Wallpaper) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderViewViewModel = hiltViewModel()
) {
    val folder by viewModel.folder.collectAsStateWithLifecycle()
    val wallpapers = folder?.wallpapers.orEmpty()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    SnackbarMessageEffect(message, snackbarHostState, viewModel::dismissMessage)

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var sortOption by rememberSaveable { mutableStateOf(SortOption.DATE_ADDED_DESC) }

    val sortedWallpapers = remember(wallpapers, sortOption) {
        wallpapers.sortedWith(sortOption.wallpaperComparator)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BackTopAppBar(title = folder?.displayName ?: "", onBack = onBackClick) {
                IconButton(onClick = viewModel::refresh, enabled = !isRefreshing) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_folder))
                }
                IconButton(onClick = { showSortSheet = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            if (folder != null && wallpapers.isEmpty()) {
                EmptyCollection(
                    title = stringResource(R.string.folder_empty_title),
                    hint = stringResource(R.string.folder_empty_hint)
                )
            } else {
                WallpaperGrid {
                    items(
                        items = sortedWallpapers,
                        key = { wallpaper -> wallpaper.id }
                    ) { wallpaper ->
                        WallpaperItem(
                            wallpaperUri = wallpaper.uri,
                            wallpaperName = wallpaper.displayFileName,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = { onNavigateToWallpaperView(wallpaper) },
                            onLongClick = null,
                            modifier = Modifier.wallpaperGridItem(this)
                        )
                    }
                }
            }
            if (isRefreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            selectedOption = sortOption,
            onSortSelected = { sortOption = it },
            onDismiss = { showSortSheet = false }
        )
    }
}
