package com.anthonyla.paperize.presentation.screens.album_view.components
import com.anthonyla.paperize.core.constants.Constants

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.theme.AppSpacing

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperItem(
    wallpaperUri: String,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Animate selection state changes
    val transition = updateTransition(isSelected, label = "WallpaperItemSelection")
    val paddingTransition by transition.animateDp(label = "padding") { selected ->
        if (selected) 5.dp else 0.dp
    }
    val roundedCornerShapeTransition by transition.animateDp(label = "roundedCornerShape") { selected ->
        if (selected) 24.dp else 16.dp
    }

    Card(
        modifier = modifier
            .padding(paddingTransition)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(roundedCornerShapeTransition),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(wallpaperUri.toUri())
                    .size(Size(Constants.GRID_THUMBNAIL_WIDTH, Constants.GRID_THUMBNAIL_HEIGHT))  // Limit size for performance - suitable for grid thumbnails
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = if (isSelected) 0.7f else 1f
            )

            // Selection indicator
            if (isSelectionMode) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.content_desc_selected),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(AppSpacing.small)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = stringResource(R.string.content_desc_not_selected),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(AppSpacing.small)
                    )
                }
            }
        }
    }
}
