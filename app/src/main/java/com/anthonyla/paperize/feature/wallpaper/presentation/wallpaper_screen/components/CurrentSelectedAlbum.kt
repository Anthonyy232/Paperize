package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

/**
 * The inner card for the album bottom sheet to represent each album
 */
@Composable
fun CurrentSelectedAlbum(
    homeSelectedAlbum: AlbumWithWallpaperAndFolder?,
    lockSelectedAlbum: AlbumWithWallpaperAndFolder?,
    scheduleSeparately: Boolean,
    enableChanger: Boolean,
    animate: Boolean,
    onOpenBottomSheet: (Boolean, Boolean) -> Unit, // lock, home
    onStop: (Boolean, Boolean) -> Unit,
    onToggleChanger: (Boolean) -> Unit
) {
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

    val showHomeCoverUri = isValidUri(LocalContext.current, homeSelectedAlbum?.album?.coverUri)
    val showLockCoverUri = isValidUri(LocalContext.current, lockSelectedAlbum?.album?.coverUri)

    if (scheduleSeparately) {
        Row {
            ListItem(
                modifier = Modifier
                    .padding(PaddingValues(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 8.dp))
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenBottomSheet(true, false) },
                headlineContent = {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box (modifier = Modifier.weight(1f)) {
                            if (lockSelectedAlbum != null) {
                                IconButton(
                                    onClick = { onStop(true, false) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = stringResource(R.string.stop_the_album)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Box (modifier = Modifier.weight(1f)) {
                            IconButton(
                                onClick = { onOpenBottomSheet(true, false) }
                            ) {
                                Icon(
                                    contentDescription = stringResource(R.string.click_to_select_a_different_album),
                                    imageVector = Icons.Filled.ArrowDropDown,
                                )
                            }
                        }
                    }
                },
                supportingContent = {},
                leadingContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(modifier = Modifier.size(48.dp)) {
                            if (lockSelectedAlbum != null) {
                                if (showLockCoverUri) {
                                    GlideImage(
                                        imageModel = { lockSelectedAlbum.album.coverUri },
                                        imageOptions = ImageOptions(
                                            contentScale = ContentScale.Crop,
                                            alignment = Alignment.Center,
                                            requestSize = IntSize(150, 150),
                                        ),
                                        loading = {
                                            if (animate) {
                                                Box(modifier = Modifier.matchParentSize()) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.align(
                                                            Alignment.Center
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                    )
                                }
                            } else {
                                Image(
                                    imageVector = Icons.Default.RadioButtonUnchecked,
                                    contentDescription = stringResource(R.string.no_icon),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                },
                trailingContent = {},
                tonalElevation = 10.dp
            )
            ListItem(
                modifier = Modifier
                    .padding(PaddingValues(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 16.dp))
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenBottomSheet(false, true) },
                headlineContent = {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box (modifier = Modifier.weight(1f)) {
                            if (homeSelectedAlbum != null) {
                                IconButton(onClick = { onStop(false, true) }) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = stringResource(R.string.stop_the_album)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Box (modifier = Modifier.weight(1f)) {
                            IconButton(onClick = { onOpenBottomSheet(false, true) }) {
                                Icon(
                                    contentDescription = stringResource(R.string.click_to_select_a_different_album),
                                    imageVector = Icons.Filled.ArrowDropDown,
                                )
                            }
                        }
                    }
                },
                supportingContent = {},
                leadingContent = {
                    Box(modifier = Modifier.size(48.dp)) {
                        if (homeSelectedAlbum != null) {
                            if (showHomeCoverUri) {
                                GlideImage(
                                    imageModel = { homeSelectedAlbum.album.coverUri },
                                    imageOptions = ImageOptions(
                                        contentScale = ContentScale.Crop,
                                        alignment = Alignment.Center,
                                        requestSize = IntSize(150, 150),
                                    ),
                                    loading = {
                                        if (animate) {
                                            Box(modifier = Modifier.matchParentSize()) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.align(
                                                        Alignment.Center
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                            }
                        } else {
                            Image(
                                imageVector = Icons.Default.RadioButtonUnchecked,
                                contentDescription = stringResource(R.string.no_icon),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                },
                trailingContent = {

                },
                tonalElevation = 10.dp
            )
        }
    }
    else {
        ListItem(
            modifier = Modifier
                .padding(PaddingValues(vertical = 8.dp, horizontal = 16.dp))
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpenBottomSheet(true, true) },
            headlineContent = {
                Text(
                    text = homeSelectedAlbum?.album?.displayedAlbumName ?: stringResource(R.string.no_album_selected),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                if (homeSelectedAlbum != null) {
                    Text(
                        text = LocalContext.current.resources.getQuantityString(
                            R.plurals.wallpaper_count,
                            homeSelectedAlbum.wallpapers.size,
                            homeSelectedAlbum.wallpapers.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            leadingContent = {
                Box(modifier = Modifier.size(60.dp)) {
                    if (homeSelectedAlbum != null) {
                        if (showHomeCoverUri) {
                            GlideImage(
                                imageModel = { homeSelectedAlbum.album.coverUri },
                                imageOptions = ImageOptions(
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.Center,
                                    requestSize = IntSize(150, 150),
                                ),
                                loading = {
                                    if (animate) {
                                        Box(modifier = Modifier.matchParentSize()) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(
                                                    Alignment.Center
                                                )
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                    } else {
                        Image(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = stringResource(R.string.no_icon),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            },
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-8).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (homeSelectedAlbum != null) {
                        IconButton(onClick = { onStop(true, true) }) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = stringResource(R.string.stop_the_album)
                            )
                        }
                    }
                    IconButton(onClick = { onOpenBottomSheet(true, true) }) {
                        Icon(
                            contentDescription = stringResource(R.string.click_to_select_a_different_album),
                            imageVector = Icons.Filled.ArrowDropDown,
                        )
                    }
                    if (homeSelectedAlbum != null) {
                        Switch(
                            checked = enableChanger,
                            onCheckedChange = onToggleChanger,
                            modifier = Modifier
                                .rotate(270f)
                                .scale(0.75f)
                        )
                    }
                }
            },
            tonalElevation = 10.dp
        )
    }
}