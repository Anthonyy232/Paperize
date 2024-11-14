package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.isValidUri
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
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
    homeSelectedAlbum: AlbumWithWallpaperAndFolder?,
    lockSelectedAlbum: AlbumWithWallpaperAndFolder?,
    albums: List<AlbumWithWallpaperAndFolder>,
    onSelect: (AlbumWithWallpaperAndFolder) -> Unit,
    animate: Boolean
) {
    val modalBottomSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = modalBottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 5.dp,
        modifier = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Modifier
                .fillMaxSize()
                .padding(
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                )
        } else {
            Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        }
    ) {
        albums.forEach {
            if (it.folders.sumOf { folder -> folder.wallpapers.size } + it.wallpapers.size == 0) return@forEach
            ListItem(
                modifier = Modifier
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
                    val totalWallpapers = it.folders.sumOf { folder -> folder.wallpapers.size } + it.wallpapers.size
                    Text(
                        text = LocalContext.current.resources.getQuantityString(R.plurals.wallpaper_count, totalWallpapers, totalWallpapers),
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    Box (
                        modifier = Modifier.size(60.dp)
                    ) {
                        val showCoverUri = isValidUri(LocalContext.current, it.album.coverUri)
                        if (showCoverUri) {
                            GlideImage(
                                imageModel = { it.album.coverUri },
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
                    }
                },
                trailingContent = {
                    val isSelected = (homeSelectedAlbum?.album?.initialAlbumName == it.album.initialAlbumName) ||
                            (lockSelectedAlbum?.album?.displayedAlbumName == it.album.displayedAlbumName)
                    Icon(
                        contentDescription = if (isSelected) stringResource(R.string.currently_selected_album) else stringResource(R.string.unselected_album),
                        imageVector = if (isSelected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                    )
                },
            )
        }
    }
}