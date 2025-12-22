package com.anthonyla.paperize.core.util

import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper

/**
 * Utility object for sorting wallpapers and folders
 * Extracted from ViewModels for testability
 */
object WallpaperSorter {

    /**
     * Sort wallpapers by filename (alphabetically)
     * @param wallpapers List of wallpapers to sort
     * @param ascending True for A-Z, false for Z-A
     * @return New sorted list preserving original list
     */
    fun sortWallpapersByName(
        wallpapers: List<Wallpaper>,
        ascending: Boolean = true
    ): List<Wallpaper> {
        return if (ascending) {
            wallpapers.sortedBy { it.fileName.lowercase() }
        } else {
            wallpapers.sortedByDescending { it.fileName.lowercase() }
        }
    }

    /**
     * Sort wallpapers by date added (addedAt timestamp)
     * @param wallpapers List of wallpapers to sort
     * @param ascending True for oldest first, false for newest first
     * @return New sorted list preserving original list
     */
    fun sortWallpapersByDateAdded(
        wallpapers: List<Wallpaper>,
        ascending: Boolean = true
    ): List<Wallpaper> {
        return if (ascending) {
            wallpapers.sortedBy { it.addedAt }
        } else {
            wallpapers.sortedByDescending { it.addedAt }
        }
    }

    /**
     * Sort wallpapers by date modified (dateModified timestamp)
     * @param wallpapers List of wallpapers to sort
     * @param ascending True for oldest first, false for newest first
     * @return New sorted list preserving original list
     */
    fun sortWallpapersByDateModified(
        wallpapers: List<Wallpaper>,
        ascending: Boolean = true
    ): List<Wallpaper> {
        return if (ascending) {
            wallpapers.sortedBy { it.dateModified }
        } else {
            wallpapers.sortedByDescending { it.dateModified }
        }
    }

    /**
     * Sort folders by name (alphabetically)
     */
    fun sortFoldersByName(
        folders: List<Folder>,
        ascending: Boolean = true
    ): List<Folder> {
        return if (ascending) {
            folders.sortedBy { it.name.lowercase() }
        } else {
            folders.sortedByDescending { it.name.lowercase() }
        }
    }

    /**
     * Sort folders by date modified
     */
    fun sortFoldersByDateModified(
        folders: List<Folder>,
        ascending: Boolean = true
    ): List<Folder> {
        return if (ascending) {
            folders.sortedBy { it.dateModified }
        } else {
            folders.sortedByDescending { it.dateModified }
        }
    }

    /**
     * Shift a wallpaper within a list (drag and drop)
     * @param wallpapers Current list of wallpapers
     * @param fromUri URI of the wallpaper being moved
     * @param toUri URI of the target position
     * @return New list with wallpaper moved and display orders updated
     */
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

    /**
     * Shift a folder within a list (drag and drop)
     * @param folders Current list of folders
     * @param fromUri URI of the folder being moved
     * @param toUri URI of the target position
     * @return New list with folder moved and display orders updated
     */
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

    /**
     * Shift a wallpaper within a folder's wallpaper list
     * @param folders List of all folders
     * @param folderId ID of the folder containing the wallpaper
     * @param fromUri URI of the wallpaper being moved
     * @param toUri URI of the target position
     * @return Pair of updated folders list and the new cover URI for the folder
     */
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
                    coverUri = updatedWallpapers.firstOrNull()?.uri ?: folder.coverUri
                )
            } else {
                folder
            }
        }
    }

    /**
     * Sort all folders and wallpapers alphabetically
     */
    fun sortAllAlphabetically(
        folders: List<Folder>,
        wallpapers: List<Wallpaper>,
        ascending: Boolean = true
    ): Pair<List<Folder>, List<Wallpaper>> {
        val sortedFolders = folders.map { folder ->
            val sortedWallpapers = sortWallpapersByName(folder.wallpapers, ascending)
            folder.copy(wallpapers = applyDisplayOrder(sortedWallpapers))
        }
        
        val finalFolders = if (ascending) {
            sortedFolders.sortedBy { it.name.lowercase() }
        } else {
            sortedFolders.sortedByDescending { it.name.lowercase() }
        }
        
        val finalWallpapers = sortWallpapersByName(wallpapers, ascending)
        
        return Pair(
            applyFolderDisplayOrder(finalFolders),
            applyDisplayOrder(finalWallpapers)
        )
    }

    /**
     * Sort all folders and wallpapers by date modified
     */
    fun sortAllByDateModified(
        folders: List<Folder>,
        wallpapers: List<Wallpaper>,
        ascending: Boolean = true
    ): Pair<List<Folder>, List<Wallpaper>> {
        val sortedFolders = folders.map { folder ->
            val sortedWallpapers = sortWallpapersByDateModified(folder.wallpapers, ascending)
            folder.copy(wallpapers = applyDisplayOrder(sortedWallpapers))
        }
        
        val finalFolders = if (ascending) {
            sortedFolders.sortedBy { it.dateModified }
        } else {
            sortedFolders.sortedByDescending { it.dateModified }
        }
        
        val finalWallpapers = sortWallpapersByDateModified(wallpapers, ascending)
        
        return Pair(
            applyFolderDisplayOrder(finalFolders),
            applyDisplayOrder(finalWallpapers)
        )
    }

    /**
     * Apply displayOrder indices to sorted wallpapers
     * @param wallpapers Sorted list of wallpapers
     * @return New list with displayOrder updated to match list position
     */
    fun applyDisplayOrder(wallpapers: List<Wallpaper>): List<Wallpaper> {
        return wallpapers.mapIndexed { index, wallpaper ->
            wallpaper.copy(displayOrder = index)
        }
    }

    /**
     * Apply displayOrder indices to sorted folders
     * @param folders Sorted list of folders
     * @return New list with folder moved and display orders updated
     */
    fun applyFolderDisplayOrder(folders: List<Folder>): List<Folder> {
        return folders.mapIndexed { index, folder ->
            folder.copy(displayOrder = index)
        }
    }
}
