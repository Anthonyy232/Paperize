package com.anthonyla.paperize.presentation.screens.sort

import androidx.compose.foundation.lazy.LazyListItemInfo

sealed class SortEvent {
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

    data class Sort(val byDate: Boolean, val ascending: Boolean) : SortEvent()
}
