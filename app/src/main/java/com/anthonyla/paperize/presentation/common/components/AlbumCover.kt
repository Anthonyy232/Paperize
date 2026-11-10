package com.anthonyla.paperize.presentation.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size
import com.anthonyla.paperize.domain.model.AlbumSummary

/** The album's cover image, or a placeholder icon until one loads. */
@Composable
fun AlbumCover(
    album: AlbumSummary,
    thumbnailSize: Int,
    placeholderTint: Color,
    modifier: Modifier = Modifier,
    placeholderModifier: Modifier = Modifier
) {
    val cover = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(album.coverUri)
            .size(Size(thumbnailSize, thumbnailSize))
            .build(),
        contentScale = ContentScale.Crop
    )
    val coverState by cover.state.collectAsState()

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (coverState is AsyncImagePainter.State.Success) {
            Image(
                painter = cover,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.PhotoAlbum,
                contentDescription = album.name,
                modifier = placeholderModifier,
                tint = placeholderTint
            )
        }
    }
}
