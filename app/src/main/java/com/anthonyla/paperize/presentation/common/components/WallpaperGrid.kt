package com.anthonyla.paperize.presentation.common.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.presentation.common.theme.AppGrid
import com.anthonyla.paperize.presentation.common.theme.AppSpacing

@Composable
fun WallpaperGrid(modifier: Modifier = Modifier, content: LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(AppGrid.itemMinSize),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.gridPadding),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.gridSpacing),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.gridSpacing),
        content = content
    )
}

fun Modifier.wallpaperGridItem(scope: LazyGridItemScope): Modifier = with(scope) {
    fillMaxWidth()
        .aspectRatio(Constants.WALLPAPER_ASPECT_RATIO)
        .animateItem(
            placementSpec = tween(
                durationMillis = Constants.ANIMATION_DURATION_LONG_MS,
                easing = FastOutSlowInEasing
            )
        )
}
