package com.anthonyla.paperize.presentation.screens.folder_view

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anthonyla.paperize.presentation.screens.album_view.components.SortBottomSheet
import com.anthonyla.paperize.presentation.screens.album_view.components.SortOption
import com.anthonyla.paperize.presentation.screens.album_view.components.WallpaperItem
import com.anthonyla.paperize.presentation.screens.folder_view.components.FolderViewTopBar
import com.anthonyla.paperize.presentation.theme.AppGrid
import com.anthonyla.paperize.presentation.theme.AppSpacing

@Composable
fun FolderViewScreen(
    @Suppress("UNUSED_PARAMETER") folderId: String,
    onBackClick: () -> Unit,
    onNavigateToWallpaperView: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderViewViewModel = hiltViewModel()
) {
    val lazyListState = rememberLazyGridState()

    val folder by viewModel.folder.collectAsState()
    val wallpapers by viewModel.wallpapers.collectAsState()

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var sortOption by rememberSaveable { mutableStateOf(SortOption.DATE_ADDED_DESC) }

    // Sort wallpapers
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

    BackHandler { onBackClick() }

    Scaffold(
        topBar = {
            FolderViewTopBar(
                title = folder?.displayName ?: "",
                onBackClick = onBackClick
            )
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
            items(
                items = sortedWallpapers,
                key = { wallpaper -> wallpaper.id }
            ) { wallpaper ->
                val itemModifier = remember {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .then(
                            Modifier.animateItem(
                                placementSpec = tween(
                                    durationMillis = 800,
                                    delayMillis = 0,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        )
                }

                WallpaperItem(
                    wallpaperUri = wallpaper.uri,
                    isSelected = false,
                    isSelectionMode = false,
                    onClick = { onNavigateToWallpaperView(wallpaper.uri, wallpaper.fileName) },
                    onLongClick = { /* No selection in folder view */ },
                    modifier = itemModifier
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
