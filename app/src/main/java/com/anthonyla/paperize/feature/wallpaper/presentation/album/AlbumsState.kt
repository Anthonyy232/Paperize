package com.anthonyla.paperize.feature.wallpaper.presentation.album

import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaper


data class AlbumsState (
    val albumWithWallpapers: List<AlbumWithWallpaper> = emptyList(),
    val loaded: Boolean = false
)