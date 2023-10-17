package com.anthonyla.paperize.feature.wallpaper.presentation.album.components

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.anthonyla.paperize.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperItem(
    wallpaperUri: String,
    itemSelected: Boolean,
    selectionMode: Boolean,
    onActivateSelectionMode: (Boolean) -> Unit,
    onItemSelection: () -> Unit,
    onImageViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val transition = updateTransition(itemSelected, label = "")

    val paddingTransition by transition.animateDp(label = "") { selected ->
        if (selected) 5.dp else 0.dp
    }
    val roundedCornerShapeTransition by transition.animateDp(label = "") { selected ->
        if (selected) 24.dp else 16.dp
    }
    Box(
        modifier = modifier
            .padding(paddingTransition)
            .clip(RoundedCornerShape(roundedCornerShapeTransition))
            .combinedClickable(
                onClick = {
                    if (selectionMode) { onItemSelection() }
                    else { onImageViewClick() }
                },
                onLongClick = {
                    if (!selectionMode) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onActivateSelectionMode(true)
                        onItemSelection()
                    }
                }
            )
    ) {
        AsyncImage(
            model = wallpaperUri.toUri(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )
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