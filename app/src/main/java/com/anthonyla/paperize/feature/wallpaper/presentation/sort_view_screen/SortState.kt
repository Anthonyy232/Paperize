package com.anthonyla.paperize.feature.wallpaper.presentation.sort_view_screen

import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper

data class SortState(
    val folders: List<Folder> = emptyList(),
    val wallpapers: List<Wallpaper> = emptyList()
)
