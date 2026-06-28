package com.anthonyla.paperize.presentation.screens.wallpaper.components
import com.anthonyla.paperize.presentation.theme.AppMaxWidths
import com.anthonyla.paperize.presentation.theme.AppBorderWidths
import com.anthonyla.paperize.core.constants.Constants

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.theme.AppShapes
import com.anthonyla.paperize.presentation.theme.AppSpacing
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import kotlin.math.max
import kotlin.math.min

/**
 * Displays the home and lock wallpapers Paperize last applied.
 *
 * The images are loaded from the recorded source URIs ([homeWallpaperUri]/[lockWallpaperUri]) via
 * Coil, rather than read back from WallpaperManager.getDrawable(): the latter is privacy-restricted
 * on modern Android and returns null without the optional all-files-access permission, which left
 * the preview blank. The URIs already carry the album's persisted read access, so no extra
 * permission is needed. They update reactively whenever Paperize changes the wallpaper.
 *
 * Note: this shows the source image, not the exact cropped/scaled/effected on-screen result.
 *
 * Adapts to the device screen aspect ratio and respects the app's animate setting.
 */
@Composable
fun CurrentWallpaperPreview(
    homeWallpaperUri: String?,
    lockWallpaperUri: String?,
    animate: Boolean = true,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current

    // Portrait-oriented preview: shorter screen dimension as width.
    val screenAspectRatio = remember(configuration) {
        val screenWidth = configuration.screenWidthDp.toFloat()
        val screenHeight = configuration.screenHeightDp.toFloat()
        min(screenWidth, screenHeight) / max(screenWidth, screenHeight)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = AppMaxWidths.contentMaxWidth)
            .padding(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall),
        shape = AppShapes.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.large)
        ) {
            Text(
                text = stringResource(R.string.current_wallpapers),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = AppSpacing.medium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                // Lock wallpaper preview (on the left)
                WallpaperPreviewBox(
                    wallpaperUri = lockWallpaperUri,
                    aspectRatio = screenAspectRatio,
                    contentDescription = stringResource(R.string.content_desc_current_lock_wallpaper),
                    animate = animate,
                    modifier = Modifier.weight(1f)
                )

                // Home wallpaper preview (on the right)
                WallpaperPreviewBox(
                    wallpaperUri = homeWallpaperUri,
                    aspectRatio = screenAspectRatio,
                    contentDescription = stringResource(R.string.content_desc_current_home_wallpaper),
                    animate = animate,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * A single wallpaper preview. Shows a placeholder background until [wallpaperUri] is non-null,
 * then fades in the image. Uses the device screen aspect ratio to avoid layout jumps.
 */
@Composable
private fun WallpaperPreviewBox(
    wallpaperUri: String?,
    aspectRatio: Float,
    contentDescription: String,
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .border(
                width = AppBorderWidths.thick,
                color = Color.Black,
                shape = AppShapes.imageShape
            )
            .clip(AppShapes.imageShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        AnimatedVisibility(
            visible = wallpaperUri != null,
            enter = fadeIn(animationSpec = tween(if (animate) Constants.PERMISSION_SCREEN_TRANSITION_DELAY_MS.toInt() else 0)),
            exit = fadeOut(animationSpec = tween(if (animate) Constants.PERMISSION_SCREEN_TRANSITION_DELAY_MS.toInt() else 0))
        ) {
            wallpaperUri?.let { uri ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
                        .size(Size(Constants.PREVIEW_THUMBNAIL_WIDTH, Constants.PREVIEW_THUMBNAIL_HEIGHT))
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}
