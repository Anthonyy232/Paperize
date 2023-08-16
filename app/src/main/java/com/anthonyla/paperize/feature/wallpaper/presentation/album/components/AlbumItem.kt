package com.anthonyla.paperize.feature.wallpaper.presentation.album.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album

@Composable
fun AlbumItem(
    album: Album,
    modifier: Modifier = Modifier,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Text(album.albumName)
    }
}