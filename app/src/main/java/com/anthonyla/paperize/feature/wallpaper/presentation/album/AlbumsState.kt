package com.anthonyla.paperize.feature.wallpaper.presentation.album

import com.anthonyla.paperize.feature.wallpaper.domain.model.Album

data class AlbumsState(
    val albums: List<Album> = emptyList(),
)