package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.decompress
import com.anthonyla.paperize.core.isValidUri
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@Composable
fun CurrentSelectedAlbum(
    homeSelectedAlbum: AlbumWithWallpaperAndFolder?,
    lockSelectedAlbum: AlbumWithWallpaperAndFolder?,
    scheduleSeparately: Boolean,
    enableChanger: Boolean,
    animate: Boolean,
    onOpenBottomSheet: (lock: Boolean, home: Boolean) -> Unit,
    onDeselect: (lock: Boolean, home: Boolean) -> Unit,
    onToggleChanger: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val showHomeCoverUri = isValidUri(context, homeSelectedAlbum?.album?.coverUri)
    val showLockCoverUri = isValidUri(context, lockSelectedAlbum?.album?.coverUri)

    val baseListItemModifier = { horizontal: Int ->
        Modifier
            .padding(PaddingValues(vertical = 8.dp, horizontal = horizontal.dp))
            .clip(RoundedCornerShape(16.dp))
    }

    val albumImage = @Composable { album: AlbumWithWallpaperAndFolder?, showCoverUri: Boolean, size: Int ->
        Box(modifier = Modifier.size(size.dp)) {
            if (album != null && showCoverUri) {
                GlideImage(
                    imageModel = { album.album.coverUri?.decompress("content://com.android.externalstorage.documents/") },
                    imageOptions = ImageOptions(
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        requestSize = IntSize(150, 150),
                    ),
                    loading = {
                        if (animate) {
                            Box(modifier = Modifier.matchParentSize()) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    },
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Image(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = stringResource(R.string.no_icon),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (scheduleSeparately) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Lock screen album
            ListItem(
                modifier = baseListItemModifier(16)
                    .weight(1f)
                    .clickable { onOpenBottomSheet(true, false) },
                headlineContent = {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (lockSelectedAlbum != null) {
                                IconButton(onClick = { onDeselect(true, false) }) {
                                    Icon(Icons.Default.Stop, stringResource(R.string.stop_the_album))
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            IconButton(onClick = { onOpenBottomSheet(true, false) }) {
                                Icon(Icons.Filled.ArrowDropDown, stringResource(R.string.click_to_select_a_different_album))
                            }
                        }
                    }
                },
                leadingContent = { albumImage(lockSelectedAlbum, showLockCoverUri, 48) },
                supportingContent = {},
                trailingContent = {},
                tonalElevation = 10.dp
            )

            // Home screen album
            ListItem(
                modifier = baseListItemModifier(16)
                    .weight(1f)
                    .clickable { onOpenBottomSheet(false, true) },
                headlineContent = {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (homeSelectedAlbum != null) {
                                IconButton(onClick = { onDeselect(false, true) }) {
                                    Icon(Icons.Default.Stop, stringResource(R.string.stop_the_album))
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            IconButton(onClick = { onOpenBottomSheet(false, true) }) {
                                Icon(Icons.Filled.ArrowDropDown, stringResource(R.string.click_to_select_a_different_album))
                            }
                        }
                    }
                },
                leadingContent = { albumImage(homeSelectedAlbum, showHomeCoverUri, 48) },
                supportingContent = {},
                trailingContent = {},
                tonalElevation = 10.dp
            )
        }
    } else {
        ListItem(
            modifier = baseListItemModifier(16).clickable { onOpenBottomSheet(true, true) },
            headlineContent = {
                Text(
                    text = homeSelectedAlbum?.album?.displayedAlbumName ?: stringResource(R.string.no_album_selected),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                homeSelectedAlbum?.let {
                    Text(
                        text = context.resources.getQuantityString(
                            R.plurals.wallpaper_count,
                            it.folders.sumOf { folder -> folder.wallpapers.size } + it.wallpapers.size,
                            it.folders.sumOf { folder -> folder.wallpapers.size } + it.wallpapers.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            leadingContent = { albumImage(homeSelectedAlbum, showHomeCoverUri, 60) },
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-8).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (homeSelectedAlbum != null) {
                        IconButton(onClick = { onDeselect(true, true) }) {
                            Icon(Icons.Default.Stop, stringResource(R.string.stop_the_album))
                        }
                        Switch(
                            checked = enableChanger,
                            onCheckedChange = onToggleChanger,
                            modifier = Modifier.rotate(270f).scale(0.75f)
                        )
                    }
                    IconButton(onClick = { onOpenBottomSheet(true, true) }) {
                        Icon(Icons.Filled.ArrowDropDown, stringResource(R.string.click_to_select_a_different_album))
                    }
                }
            },
            tonalElevation = 10.dp
        )
    }
}