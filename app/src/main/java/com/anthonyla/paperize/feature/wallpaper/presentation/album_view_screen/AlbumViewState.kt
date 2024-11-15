package com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.SelectionState

data class AlbumViewState (
    val albums: List<AlbumWithWallpaperAndFolder> = emptyList(),
    val initialAlbumName: String = "",
    val selectionState: SelectionState = SelectionState(),
    val isEmpty: Boolean = true,
    val isLoading: Boolean = false
)