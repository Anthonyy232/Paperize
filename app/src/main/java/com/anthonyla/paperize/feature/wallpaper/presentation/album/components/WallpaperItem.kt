package com.anthonyla.paperize.feature.wallpaper.presentation.album.components

import android.content.Context
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.core.getImageDimensions
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

/**
 * A composable that displays a wallpaper item. It shows the wallpaper image and a selection icon when selected
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperItem(
    wallpaperUri: String,
    itemSelected: Boolean,
    selectionMode: Boolean,
    onActivateSelectionMode: (Boolean) -> Unit,
    onItemSelection: () -> Unit,
    onWallpaperViewClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float? = null,
    clickable: Boolean = true,
    animate: Boolean = true,
    darken: Boolean = false,
    darkenPercentage: Int? = null,
    blur: Boolean = false,
    blurPercentage: Int? = null,
    scaling: ScalingConstants? = null
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val transition = updateTransition(itemSelected, label = "")

    val paddingTransition by transition.animateDp(label = "") { selected ->
        if (selected) 5.dp else 0.dp
    }
    val roundedCornerShapeTransition by transition.animateDp(label = "") { selected ->
        if (selected) 24.dp else 16.dp
    }

    fun isValidUri(context: Context, uriString: String?): Boolean {
        val uri = uriString?.toUri()
        return try {
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                inputStream?.close()
            }
            true
        } catch (e: Exception) { false }
    }

    val showUri = isValidUri(LocalContext.current, wallpaperUri)
    val boxModifier = if (clickable) {
        modifier
            .padding(paddingTransition)
            .clip(RoundedCornerShape(roundedCornerShapeTransition))
            .combinedClickable(
                onClick = {
                    if (!selectionMode) {
                        onWallpaperViewClick()
                    } else {
                        onItemSelection()
                    }
                },
                onLongClick = {
                    if (!selectionMode) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onActivateSelectionMode(true)
                        onItemSelection()
                    }
                }
            )
    } else {
        modifier
            .padding(paddingTransition)
            .clip(RoundedCornerShape(roundedCornerShapeTransition))
    }
    Box(modifier = boxModifier) {
        if (showUri) {
            val dimension = wallpaperUri.toUri().getImageDimensions(context)
            val imageAspectRatio = aspectRatio ?: ((dimension?.width?.toFloat() ?: 9f) / (dimension?.height?.toFloat() ?: 19.5f))
            GlideImage(
                imageModel = { wallpaperUri },
                imageOptions = ImageOptions(
                    contentScale = if (scaling != null) {
                        when (scaling) {
                            ScalingConstants.FILL -> ContentScale.FillHeight
                            ScalingConstants.FIT -> ContentScale.FillWidth
                            ScalingConstants.STRETCH -> ContentScale.FillBounds
                        }
                    } else { ContentScale.Crop },
                    requestSize = IntSize(300, 300),
                    alignment = Alignment.Center,
                    colorFilter = if (darken && darkenPercentage != null && darkenPercentage < 100) {
                        ColorFilter.tint(
                            Color.Black.copy(alpha = (100 - darkenPercentage).toFloat().div(100f)),
                            BlendMode.Darken
                        )
                    } else { null }
                ),
                loading = {
                    if (animate) {
                        Box(modifier = Modifier.matchParentSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                },
                requestBuilder =  {
                    Glide
                        .with(LocalContext.current)
                        .asBitmap()
                        .format(DecodeFormat.PREFER_RGB_565)
                },
                modifier = Modifier
                    .aspectRatio(imageAspectRatio)
                    .background(if (scaling != null) Color.Black else Color.Transparent)
                    .blur(
                        if (blur && blurPercentage != null && blurPercentage > 0 ) {
                            blurPercentage.toFloat().div(100f) * 1.5.dp
                        } else { 0.dp })
                    .clip(RoundedCornerShape(roundedCornerShapeTransition))
            )
        }
        if (selectionMode) {
            val bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
            if (itemSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.image_is_selected),
                    modifier = Modifier
                        .padding(4.dp)
                        .border(2.dp, bgColor, CircleShape)
                        .clip(CircleShape)
                        .background(bgColor)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = stringResource(R.string.image_is_not_selected),
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}