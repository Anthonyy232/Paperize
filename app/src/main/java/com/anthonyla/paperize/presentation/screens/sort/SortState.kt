package com.anthonyla.paperize.presentation.screens.sort

import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper

data class SortState(
    val folders: List<Folder> = emptyList(),
    val wallpapers: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: Int? = null
)
