package com.anthonyla.paperize.presentation.screens.folder_view

import com.anthonyla.paperize.core.constants.Constants

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.common.components.EmptyCollection
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anthonyla.paperize.presentation.screens.album_view.components.SortBottomSheet
import com.anthonyla.paperize.presentation.screens.album_view.components.SortOption
import com.anthonyla.paperize.presentation.screens.album_view.components.WallpaperItem
import com.anthonyla.paperize.presentation.screens.folder_view.components.FolderViewTopBar
import com.anthonyla.paperize.presentation.theme.AppGrid
import com.anthonyla.paperize.presentation.theme.AppSpacing

@Composable
fun FolderViewScreen(
    onBackClick: () -> Unit,
    onNavigateToWallpaperView: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderViewViewModel = hiltViewModel()
) {
    val lazyListState = rememberLazyGridState()

    val folder by viewModel.folder.collectAsStateWithLifecycle()
    val wallpapers = folder?.wallpapers.orEmpty()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val messageText = message?.let { stringResource(it) }
    LaunchedEffect(messageText) {
        messageText?.let {
            snackbarHost.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var sortOption by rememberSaveable { mutableStateOf(SortOption.DATE_ADDED_DESC) }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            FolderViewTopBar(
                title = folder?.displayName ?: "",
                onBackClick = onBackClick,
                onSortClick = { showSortSheet = true },
                isRefreshing = isRefreshing,
                onRefreshClick = viewModel::refresh
            )
        }
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            if (folder != null && wallpapers.isEmpty()) {
                EmptyCollection(
                    title = stringResource(R.string.folder_empty_title),
                    hint = stringResource(R.string.folder_empty_hint)
                )
            } else {
                LazyVerticalGrid(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(AppGrid.itemMinSize),
                    contentPadding = PaddingValues(AppSpacing.gridPadding),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.gridSpacing)
                ) {
                    items(
                        items = sortedWallpapers,
                        key = { wallpaper -> wallpaper.id }
                    ) { wallpaper ->
                        WallpaperItem(
                            wallpaperUri = wallpaper.uri,
                            wallpaperName = wallpaper.displayFileName,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = {
                                onNavigateToWallpaperView(
                                    wallpaper.id,
                                    wallpaper.uri,
                                    wallpaper.fileName
                                )
                            },
                            onLongClick = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(Constants.WALLPAPER_ASPECT_RATIO)
                                .animateItem(
                                    placementSpec = tween(
                                        durationMillis = Constants.ANIMATION_DURATION_LONG_MS,
                                        easing = FastOutSlowInEasing
                                    )
                                )
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
