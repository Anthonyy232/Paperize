package com.anthonyla.paperize.presentation.screens.wallpaper.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.presentation.common.theme.AppSpacing
import kotlin.math.max
import kotlin.math.min

private val PreviewHeight = 240.dp

/** Shows the last applied source URIs; WallpaperManager readback is restricted on modern Android.
 * Cropping and effects applied to the actual wallpaper are not included in this preview.
 * Draws nothing until a wallpaper has been applied.
 */
@Composable
fun CurrentWallpaperPreview(
    homeWallpaper: Wallpaper?,
    lockWallpaper: Wallpaper?,
    animate: Boolean,
    onRemove: (Wallpaper) -> Unit
) {
    if (homeWallpaper == null && lockWallpaper == null) return
    val windowSize = LocalWindowInfo.current.containerSize

    // Portrait-oriented preview: shorter screen dimension as width.
    val screenAspectRatio = remember(windowSize) {
        min(windowSize.width, windowSize.height).toFloat() / max(windowSize.width, windowSize.height)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(AppSpacing.large)) {
            Text(
                text = stringResource(R.string.current_wallpapers),
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.medium),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.large, Alignment.CenterHorizontally)
            ) {
                if (lockWallpaper != null) {
                    WallpaperPreviewBox(
                        wallpaperUri = lockWallpaper.uri,
                        aspectRatio = screenAspectRatio,
                        label = stringResource(R.string.lock),
                        contentDescription = stringResource(R.string.content_desc_current_lock_wallpaper),
                        removeDescription = stringResource(R.string.content_desc_remove_lock_wallpaper),
                        onRemove = { onRemove(lockWallpaper) },
                        animate = animate
                    )
                }
                if (homeWallpaper != null) {
                    WallpaperPreviewBox(
                        wallpaperUri = homeWallpaper.uri,
                        aspectRatio = screenAspectRatio,
                        label = stringResource(R.string.home),
                        contentDescription = stringResource(R.string.content_desc_current_home_wallpaper),
                        removeDescription = stringResource(R.string.content_desc_remove_home_wallpaper),
                        onRemove = { onRemove(homeWallpaper) },
                        animate = animate
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperPreviewBox(
    wallpaperUri: String,
    aspectRatio: Float,
    label: String,
    contentDescription: String,
    removeDescription: String,
    onRemove: () -> Unit,
    animate: Boolean
) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        Box(
            modifier = Modifier
                .height(PreviewHeight)
                .aspectRatio(aspectRatio)
                // A phone bezel, so it stays black in every theme.
                .border(width = 3.dp, color = Color.Black, shape = MaterialTheme.shapes.medium)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(wallpaperUri)
                    .size(Size(Constants.PREVIEW_THUMBNAIL_WIDTH, Constants.PREVIEW_THUMBNAIL_HEIGHT))
                    .crossfade(animate)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = removeDescription)
            }
        }
    }
}
