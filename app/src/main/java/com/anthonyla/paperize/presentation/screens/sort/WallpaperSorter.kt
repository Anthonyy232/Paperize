package com.anthonyla.paperize.presentation.screens.sort

import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper

object WallpaperSorter {

    fun shiftWallpaper(wallpapers: List<Wallpaper>, fromUri: String, toUri: String): List<Wallpaper> =
        applyDisplayOrder(wallpapers.moved(fromUri, toUri) { it.uri } ?: return wallpapers)

    fun shiftFolder(folders: List<Folder>, fromUri: String, toUri: String): List<Folder> =
        (folders.moved(fromUri, toUri) { it.uri } ?: return folders)
            .mapIndexed { index, folder -> folder.copy(displayOrder = index) }

    fun shiftWallpaperInFolder(
        folders: List<Folder>,
        folderId: String,
        fromUri: String,
        toUri: String
    ): List<Folder> {
        return folders.map { folder ->
            if (folder.id == folderId) {
                val updatedWallpapers = shiftWallpaper(folder.wallpapers, fromUri, toUri)
                folder.copy(
                    wallpapers = updatedWallpapers,
                    coverUri = updatedWallpapers.firstOrNull()?.uri
                )
            } else {
                folder
            }
        }
    }

    fun sortAllAlphabetically(
        folders: List<Folder>,
        wallpapers: List<Wallpaper>,
        ascending: Boolean = true
    ): Pair<List<Folder>, List<Wallpaper>> = sortAll(
        folders, wallpapers, ascending,
        compareBy { it.name.lowercase() }, compareBy { it.fileName.lowercase() }
    )

    fun sortAllByDateModified(
        folders: List<Folder>,
        wallpapers: List<Wallpaper>,
        ascending: Boolean = true
    ): Pair<List<Folder>, List<Wallpaper>> = sortAll(
        folders, wallpapers, ascending,
        compareBy { it.dateModified }, compareBy { it.dateModified }
    )

    private fun sortAll(
        folders: List<Folder>,
        wallpapers: List<Wallpaper>,
        ascending: Boolean,
        folderComparator: Comparator<Folder>,
        wallpaperComparator: Comparator<Wallpaper>
    ): Pair<List<Folder>, List<Wallpaper>> {
        val folderOrder = if (ascending) folderComparator else folderComparator.reversed()
        val wallpaperOrder = if (ascending) wallpaperComparator else wallpaperComparator.reversed()
        val sortedFolders = folders.sortedWith(folderOrder).mapIndexed { index, folder ->
            val images = applyDisplayOrder(folder.wallpapers.sortedWith(wallpaperOrder))
            folder.copy(displayOrder = index, wallpapers = images, coverUri = images.firstOrNull()?.uri)
        }
        return sortedFolders to applyDisplayOrder(wallpapers.sortedWith(wallpaperOrder))
    }

    private fun applyDisplayOrder(wallpapers: List<Wallpaper>): List<Wallpaper> =
        wallpapers.mapIndexed { index, wallpaper -> wallpaper.copy(displayOrder = index) }

    /** Moves the item keyed [from] to the position of [to]; null if either key is missing. */
    private fun <T> List<T>.moved(from: String, to: String, key: (T) -> String): List<T>? {
        val fromIndex = indexOfFirst { key(it) == from }
        val toIndex = indexOfFirst { key(it) == to }
        if (fromIndex == -1 || toIndex == -1) return null
        return toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }
}
