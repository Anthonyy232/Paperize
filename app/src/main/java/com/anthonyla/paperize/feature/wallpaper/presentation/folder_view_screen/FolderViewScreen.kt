package com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem
import com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen.components.FolderViewTopBar

@Composable
fun FolderViewScreen(
    folderName: String?,
    wallpapers: List<String>,
    onBackClick: () -> Unit,
    onShowWallpaperView: (String) -> Unit
) {
    val lazyListState = rememberLazyStaggeredGridState()
    BackHandler { onBackClick() }

    Scaffold(
        topBar = {
            FolderViewTopBar(
                title = folderName ?: "",
            ) {
                onBackClick()
            }
        },
        content = {
            LazyVerticalStaggeredGrid(
                state = lazyListState,
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp, 4.dp),
                horizontalArrangement = Arrangement.Start,
                content = {
                    items (items = wallpapers, key = { wallpaper -> wallpaper }
                    ) { wallpaper ->
                        WallpaperItem(
                            wallpaperUri = wallpaper,
                            itemSelected = false,
                            selectionMode = false,
                            onActivateSelectionMode = {},
                            onItemSelection = {},
                            onWallpaperViewClick = {
                                onShowWallpaperView(wallpaper)
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
                }
            )
        }
    )
}