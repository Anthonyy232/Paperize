package com.anthonyla.paperize.presentation.screens.sort

import androidx.compose.foundation.lazy.LazyListItemInfo
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper

sealed class SortEvent {
    data object Reset : SortEvent()

    data class LoadSortView(
        val folders: List<Folder>,
        val wallpapers: List<Wallpaper>
    ) : SortEvent()

    data class ShiftFolder(
        val from: LazyListItemInfo,
        val to: LazyListItemInfo
    ) : SortEvent()

    data class ShiftWallpaper(
        val from: LazyListItemInfo,
        val to: LazyListItemInfo
    ) : SortEvent()

    data class ShiftFolderWallpaper(
        val folderId: String,
        val from: LazyListItemInfo,
        val to: LazyListItemInfo
    ) : SortEvent()

    data object SortAlphabetically : SortEvent()
    data object SortAlphabeticallyReverse : SortEvent()
    data object SortByLastModified : SortEvent()
    data object SortByLastModifiedReverse : SortEvent()
}
