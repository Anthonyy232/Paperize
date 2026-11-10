package com.anthonyla.paperize.presentation.common.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.anthonyla.paperize.core.constants.Constants

@Composable
fun WallpaperItem(
    wallpaperUri: String,
    wallpaperName: String,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    SelectableGridCard(
        isSelected = isSelected,
        isSelectionMode = isSelectionMode,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.semantics {
            contentDescription = wallpaperName
            role = Role.Button
        }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(wallpaperUri.toUri())
                .size(Size(Constants.GRID_THUMBNAIL_WIDTH, Constants.GRID_THUMBNAIL_HEIGHT))
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = if (isSelected) 0.7f else 1f
        )
    }
}
