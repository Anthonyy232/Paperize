package com.anthonyla.paperize.feature.wallpaper.presentation.album.components

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.sharp.Block
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.decompress
import com.anthonyla.paperize.core.isValidUri
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
    onItemSelection: () -> Unit,
    onFolderViewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val transition = updateTransition(itemSelected, label = "")

    val iconSelectionColor = MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp)
    val paddingTransition by transition.animateDp(label = "") { selected ->
        if (selected) 5.dp else 0.dp
    }
    val roundedCornerShapeTransition by transition.animateDp(label = "") { selected ->
        if (selected) 24.dp else 16.dp
    }

    val showCoverUri by remember { mutableStateOf(!folder.coverUri.isNullOrEmpty() && isValidUri(context, folder.coverUri)) }

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
                        onItemSelection()
                    }
                }
            ),
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box (modifier = Modifier.fillMaxHeight(0.8f)) {
                if (showCoverUri) {
                    GlideImage(
                        imageModel = { folder.coverUri?.decompress("content://com.android.externalstorage.documents/")?.toUri() },
                        imageOptions = ImageOptions(
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            requestSize = IntSize(300, 300),
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
                else {
                    Box (modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = stringResource(R.string.image_is_not_selected),
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                        )
                    }
                }
                if (selectionMode) {
                    if (itemSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.image_is_selected),
                            modifier = Modifier
                                .padding(9.dp)
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
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.Start)
                    .fillMaxHeight()
                    .wrapContentHeight(Alignment.CenterVertically),
            ) {
                folder.folderName?.let { name ->
                    Text(
                        text = name,
                        modifier = Modifier,
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = folder.wallpapers.size.toString().plus(" " + stringResource(R.string.wallpapers)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}