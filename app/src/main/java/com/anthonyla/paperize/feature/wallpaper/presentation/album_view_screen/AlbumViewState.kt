package com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen

data class AlbumViewState (
    val selectedWallpapers: List<String> = emptyList(),
    val selectedFolders: List<String> = emptyList(),
    val allSelected: Boolean = false,
    val selectedCount: Int = 0,
    val maxSize: Int = 0
)