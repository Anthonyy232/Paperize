package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen

import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper

data class AddAlbumState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val selectionState: SelectionState = SelectionState(),
    val isEmpty: Boolean = true,
    val isLoading: Boolean = false
)

data class SelectionState(
    val selectedWallpapers: List<String> = emptyList(),
    val selectedFolders: List<String> = emptyList(),
    val allSelected: Boolean = false,
    val selectedCount: Int = 0
)