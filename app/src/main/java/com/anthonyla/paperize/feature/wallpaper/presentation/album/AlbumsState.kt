package com.anthonyla.paperize.feature.wallpaper.presentation.album

import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder

data class AlbumsState (
    val albumsWithWallpapers: List<AlbumWithWallpaperAndFolder> = emptyList()
)