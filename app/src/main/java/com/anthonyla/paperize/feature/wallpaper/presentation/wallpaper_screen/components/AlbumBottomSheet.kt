package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.launch

/**
 * Contains the album bottom sheet that shows from the bottom when the user selects which sets the current rotating album
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumBottomSheet(
    onDismiss: () -> Unit,
    currentSelectedAlbum: SelectedAlbum?,
    albums: List<AlbumWithWallpaperAndFolder>,
    onSelect: (AlbumWithWallpaperAndFolder) -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = modalBottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        albums.forEach {
            ListItem(
                modifier = Modifier
                    .padding(4.dp)
                    .clickable {
                        scope.launch {
                            modalBottomSheetState.hide()
                            onSelect(it)
                            onDismiss()
                        }
                    },
                headlineContent = {
                    Text(
                        text = it.album.displayedAlbumName,
                        style = MaterialTheme.typography.titleMedium
                    ) },
                supportingContent = {
                    Text(
                        text = (it.folders.sumOf { folder -> folder.wallpapers.size } + it.wallpapers.size).toString() + stringResource(R.string.wallpaper),
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    Box (
                        modifier = Modifier.size(60.dp)
                    ) {
                        GlideImage(
                            imageModel = { it.album.coverUri },
                            imageOptions = ImageOptions(
                                contentScale = ContentScale.FillBounds,
                                alignment = Alignment.Center,
                            ),
                            requestBuilder = {
                                Glide
                                    .with(LocalContext.current)
                                    .asBitmap()
                                    .transition(BitmapTransitionOptions.withCrossFade())
                            },
                            loading = {
                                Box(modifier = Modifier.matchParentSize()) {
                                    CircularProgressIndicator(modifier = Modifier.align(
                                        Alignment.Center))
                                }
                            },
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                },
                trailingContent = {
                    if (currentSelectedAlbum != null) {
                        if (currentSelectedAlbum.album.initialAlbumName == it.album.initialAlbumName) {
                            Icon(
                                contentDescription = stringResource(R.string.currently_selected_album),
                                imageVector = Icons.Filled.RadioButtonChecked,
                            )
                        }
                        else {
                            Icon(
                                contentDescription = stringResource(R.string.currently_selected_album),
                                imageVector = Icons.Filled.RadioButtonUnchecked,
                            )
                        }
                    }
                },
            )
        }
    }
}