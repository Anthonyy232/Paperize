package com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember // Added remember import
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem
import com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen.components.FolderViewTopBar
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

@Composable
fun FolderViewScreen(
    folder: Folder,
    onBackClick: () -> Unit,
    onShowWallpaperView: (String, String) -> Unit,
    animate: Boolean
) {
    val lazyListState = rememberLazyGridState()
    val colorScheme = MaterialTheme.colorScheme

    val scrollbarSettings = remember {
        ScrollbarSettings.Default.copy(
            thumbUnselectedColor = colorScheme.primary,
            thumbSelectedColor = colorScheme.primary,
            thumbShape = RoundedCornerShape(16.dp),
            scrollbarPadding = 1.dp,
        )
    }

    val noOpItemSelection: () -> Unit = remember { { /* Do nothing */ } }

    BackHandler { onBackClick() }

    Scaffold(
        topBar = {
            FolderViewTopBar(
                title = folder.folderName ?: "",
                onBackClick = onBackClick
            )
        },
        content = { paddingValues ->
            LazyVerticalGridScrollbar(
                state = lazyListState,
                settings = scrollbarSettings,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyVerticalGrid(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = folder.wallpapers, key = { wallpaper -> wallpaper.wallpaperUri }) { wallpaper ->
                        val itemModifier = remember(animate) {
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f / 16f)
                                .then(
                                    if (animate) Modifier.animateItem(
                                        placementSpec = tween(
                                            durationMillis = 800,
                                            delayMillis = 0,
                                            easing = FastOutSlowInEasing
                                        )
                                    ) else Modifier
                                )
                        }

                        WallpaperItem(
                            wallpaperUri = wallpaper.wallpaperUri,
                            itemSelected = false,
                            selectionMode = false,
                            allowHapticFeedback = false,
                            onItemSelection = noOpItemSelection,
                            onWallpaperViewClick = {
                                onShowWallpaperView(wallpaper.wallpaperUri, wallpaper.fileName)
                            },
                            modifier = itemModifier
                        )
                    }
                }
            }
        }
    )
}