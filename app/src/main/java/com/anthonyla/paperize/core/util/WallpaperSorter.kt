package com.anthonyla.paperize.core.util

import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper

object WallpaperSorter {

    fun shiftWallpaper(
        wallpapers: List<Wallpaper>,
        fromUri: String,
        toUri: String
    ): List<Wallpaper> {
        val mutableList = wallpapers.toMutableList()
        val fromIndex = mutableList.indexOfFirst { it.uri == fromUri }
        val toIndex = mutableList.indexOfFirst { it.uri == toUri }
        
        if (fromIndex == -1 || toIndex == -1) return wallpapers
        
        val movedItem = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, movedItem)
        
        return applyDisplayOrder(mutableList)
    }

    fun shiftFolder(
        folders: List<Folder>,
        fromUri: String,
        toUri: String
    ): List<Folder> {
        val mutableList = folders.toMutableList()
        val fromIndex = mutableList.indexOfFirst { it.uri == fromUri }
        val toIndex = mutableList.indexOfFirst { it.uri == toUri }
        
        if (fromIndex == -1 || toIndex == -1) return folders
        
        val movedItem = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, movedItem)
        
        return applyFolderDisplayOrder(mutableList)
    }

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

    private fun applyDisplayOrder(wallpapers: List<Wallpaper>): List<Wallpaper> {
        return wallpapers.mapIndexed { index, wallpaper ->
            wallpaper.copy(displayOrder = index)
        }
    }

    private fun applyFolderDisplayOrder(folders: List<Folder>): List<Folder> {
        return folders.mapIndexed { index, folder ->
            folder.copy(displayOrder = index)
        }
    }
}
