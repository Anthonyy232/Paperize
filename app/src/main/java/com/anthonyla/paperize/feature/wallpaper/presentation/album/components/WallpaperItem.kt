package com.anthonyla.paperize.feature.wallpaper.presentation.album.components

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember // Added
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.decompress
import com.anthonyla.paperize.core.isValidUri
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
    modifier: Modifier = Modifier,
    allowHapticFeedback: Boolean = true,
    onItemSelection: () -> Unit,
    onWallpaperViewClick: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val showUri by remember(wallpaperUri) { mutableStateOf(isValidUri(context, wallpaperUri)) }
    val decompressedUri by remember(wallpaperUri) {
        mutableStateOf(wallpaperUri.decompress("content://com.android.externalstorage.documents/").toUri())
    }

    val imageOptions = remember {
        ImageOptions(
            requestSize = IntSize(200, 200),
            alignment = Alignment.Center,
        )
    }

    val transition = updateTransition(itemSelected, label = "WallpaperItemSelection")

    val paddingTransition by transition.animateDp(label = "padding") { selected ->
        if (selected) 5.dp else 0.dp
    }
    val roundedCornerShapeTransition by transition.animateDp(label = "roundedCornerShape") { selected ->
        if (selected) 24.dp else 16.dp
    }

    Box(
        modifier = modifier
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
                        if (allowHapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onItemSelection()
                    }
                }
            )
    ) {
        if (showUri) {
            GlideImage(
                imageModel = { decompressedUri },
                imageOptions = imageOptions,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(roundedCornerShapeTransition))
            )
        }
        if (selectionMode) {
            val bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
            if (itemSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.image_is_selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(9.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .border(2.dp, bgColor, CircleShape)
                        .align(Alignment.TopEnd)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = stringResource(R.string.image_is_not_selected),
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}