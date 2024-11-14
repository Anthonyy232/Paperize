package com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen

import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder

sealed class FolderEvent {
    data object Reset: FolderEvent()

    data class LoadFolderView(
        val folder: Folder? = null
    ): FolderEvent()
}