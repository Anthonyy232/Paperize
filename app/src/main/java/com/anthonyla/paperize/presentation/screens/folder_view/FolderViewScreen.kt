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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anthonyla.paperize.presentation.screens.album_view.components.WallpaperItem
import com.anthonyla.paperize.presentation.screens.folder_view.components.FolderViewTopBar

@Composable
fun FolderViewScreen(
    folderId: String,
    onBackClick: () -> Unit,
    onNavigateToWallpaperView: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderViewViewModel = hiltViewModel()
) {
    val lazyListState = rememberLazyGridState()

    val folder by viewModel.folder.collectAsState()
    val wallpapers by viewModel.wallpapers.collectAsState()

    BackHandler { onBackClick() }

    Scaffold(
        topBar = {
            FolderViewTopBar(
                title = folder?.name ?: "",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            state = lazyListState,
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = wallpapers,
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
                    onClick = { onNavigateToWallpaperView(wallpaper.uri, wallpaper.fileName) },
                    modifier = itemModifier
                )
            }
        }
    }
}
