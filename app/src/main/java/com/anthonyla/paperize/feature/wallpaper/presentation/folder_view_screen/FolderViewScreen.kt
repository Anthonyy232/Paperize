package com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem
import com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen.components.FolderViewTopBar
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

@Composable
fun FolderViewScreen(
    folderName: String?,
    wallpapers: List<String>,
    onBackClick: () -> Unit,
    onShowWallpaperView: (String) -> Unit,
    animate: Boolean
) {
    val lazyListState = rememberLazyGridState()
    BackHandler { onBackClick() }

    Scaffold(
        topBar = {
            FolderViewTopBar(
                title = folderName ?: "",
                onBackClick = onBackClick
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
                    .padding(it)
            ) {
                LazyVerticalGrid(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(4.dp, 4.dp),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    items(count = wallpapers.size, key = { index -> wallpapers[index] }) { index ->
                        if (animate) {
                            WallpaperItem(
                                wallpaperUri = wallpapers[index],
                                itemSelected = false,
                                selectionMode = false,
                                allowHapticFeedback = false,
                                onActivateSelectionMode = {},
                                onItemSelection = {},
                                onWallpaperViewClick = {
                                    onShowWallpaperView(wallpapers[index])
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
                            WallpaperItem(
                                wallpaperUri = wallpapers[index],
                                itemSelected = false,
                                selectionMode = false,
                                allowHapticFeedback = false,
                                onActivateSelectionMode = {},
                                onItemSelection = {},
                                onWallpaperViewClick = {
                                    onShowWallpaperView(wallpapers[index])
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