package com.anthonyla.paperize.feature.wallpaper.presentation.album.components

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RectShape
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(
    folder: Folder,
    itemSelected: Boolean,
    selectionMode: Boolean,
    onActivateSelectionMode: (Boolean) -> Unit,
    onItemSelection: () -> Unit,
    onFolderViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val haptics = LocalHapticFeedback.current
    val transition = updateTransition(itemSelected, label = "")

    val bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp)
    val paddingTransition by transition.animateDp(label = "") { selected ->
        if (selected) 5.dp else 0.dp
    }
    val roundedCornerShapeTransition by transition.animateDp(label = "") { selected ->
        if (selected) 24.dp else 16.dp
    }

    Card(
        elevation = CardDefaults.cardElevation(5.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(paddingTransition)
            .clip(RoundedCornerShape(roundedCornerShapeTransition))
            .combinedClickable(
                onClick = {
                    if (!selectionMode) { onFolderViewClick() }
                    else { onItemSelection() }
                },
                onLongClick = {
                    if (!selectionMode) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onActivateSelectionMode(true)
                        onItemSelection()
                    }
                }
            ),
        shape = RoundedCornerShape(roundedCornerShapeTransition),
        border = BorderStroke(1.dp, bgColor)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            tonalElevation = 10.dp,
        ) {
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    if (folder.coverUri != null) {
                        GlideImage(
                            imageModel = { folder.coverUri.toUri() },
                            imageOptions = ImageOptions(
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center
                            ),
                            requestBuilder = {
                                Glide
                                    .with(LocalContext.current)
                                    .asBitmap()
                                    .transition(BitmapTransitionOptions.withCrossFade())
                            },
                            loading = {
                                Box(modifier = Modifier.matchParentSize()) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                }
                            },
                            modifier = Modifier
                                .height(configuration.screenHeightDp.dp / 4.0f)
                                .clip(RoundedCornerShape(roundedCornerShapeTransition))
                        )
                    }
                    if (selectionMode) {
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
                Spacer(modifier = Modifier.padding(6.dp))
                folder.folderName?.let { name ->
                    Text(
                        text = name,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .align(Alignment.Start),
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = folder.wallpapers.size.toString().plus(" wallpapers"),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .align(Alignment.Start),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.padding(8.dp))
            }
        }
    }
}