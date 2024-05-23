package com.anthonyla.paperize.feature.wallpaper.presentation.album.components

import android.content.Context
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

/**
 * FolderItem composable is a single item of the folder list. It shows the folder's cover image,
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(
    folder: Folder,
    itemSelected: Boolean,
    selectionMode: Boolean,
    onActivateSelectionMode: (Boolean) -> Unit,
    onItemSelection: () -> Unit,
    onFolderViewClick: () -> Unit,
    modifier: Modifier = Modifier,
    animate: Boolean = true
) {
    val configuration = LocalConfiguration.current
    val haptics = LocalHapticFeedback.current
    val transition = updateTransition(itemSelected, label = "")

    val iconSelectionColor = MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp)
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

    val showCoverUri = folder.coverUri != null && isValidUri(LocalContext.current, folder.coverUri)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxSize()
            .padding(paddingTransition)
            .clip(RoundedCornerShape(roundedCornerShapeTransition))
            .combinedClickable(
                onClick = {
                    if (!selectionMode) {
                        onFolderViewClick()
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
            ),
        shape = RoundedCornerShape(roundedCornerShapeTransition),
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                if (showCoverUri) {
                    GlideImage(
                        imageModel = { folder.coverUri?.toUri() },
                        imageOptions = ImageOptions(
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            requestSize = IntSize(300, 300),
                        ),
                        loading = {
                            if (animate) {
                                Box(modifier = Modifier.matchParentSize()) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        },
                        modifier = Modifier
                            .aspectRatio(0.8f)
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
                                .border(2.dp, iconSelectionColor, CircleShape)
                                .clip(CircleShape)
                                .background(iconSelectionColor)
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
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = folder.wallpapers.size.toString().plus(" " + stringResource(R.string.wallpapers)),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .align(Alignment.Start),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.padding(8.dp))
        }
    }
}