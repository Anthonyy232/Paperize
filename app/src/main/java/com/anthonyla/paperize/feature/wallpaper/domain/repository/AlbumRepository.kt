package com.anthonyla.paperize.feature.wallpaper.domain.repository

import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    /**
     * Get all albums with wallpaper and folder
     */
    fun getAlbumsWithWallpaperAndFolder(): Flow<List<AlbumWithWallpaperAndFolder>>

    /**
     * Get all selected albums with wallpaper and folder
     */
    fun getSelectedAlbums(): Flow<List<AlbumWithWallpaperAndFolder>>

    /**
     * Get all albums (basic info only, no wallpapers/folders)
     */
    fun getAlbums(): Flow<List<Album>>

    /**
     * Get all selected albums (basic info only)
     */
    fun getSelectedAlbumsBasic(): Flow<List<Album>>

    /**
     * Get a specific album with wallpapers and folders
     */
    fun getAlbumWithWallpaperAndFolder(albumName: String): Flow<AlbumWithWallpaperAndFolder?>

    /**
     * Update album selection
     */
    suspend fun updateAlbumSelection(albumName: String, selected: Boolean)

    /**
     * Deselect all albums
     */
    suspend fun deselectAllAlbums()

    /**
     * Insert or update album with wallpaper and folder
     */
    suspend fun upsertAlbumWithWallpaperAndFolder(albumWithWallpaperAndFolder: AlbumWithWallpaperAndFolder)

    /**
     * Insert or update album and its wallpaper and folder
     */
    suspend fun upsertAlbum(album: Album)

    /**
     * Delete album and its wallpaper and folder
     */
    suspend fun deleteAlbum(album: Album)

    /**
     * Update album
     */
    suspend fun updateAlbum(album: Album)

    /**
     * Cascade delete album and its wallpaper and folder. Proper way to delete an AlbumWithWallpaperAndFolder
     */
    suspend fun cascadeDeleteAlbum(album: Album)

    /**
     * Insert or update wallpaper
     */
    suspend fun upsertWallpaper(wallpaper: Wallpaper)

    /**
     * Insert or update wallpaper list in one transaction
     */
    suspend fun upsertWallpaperList(wallpapers: List<Wallpaper>)

    /**
     * Delete wallpaper
     */
    suspend fun deleteWallpaper(wallpaper: Wallpaper)

    /**
     * Delete wallpaper list in one transaction
     */
    suspend fun deleteWallpaperList(wallpapers: List<Wallpaper>)

    /**
     * Update wallpaper
     */
    suspend fun updateWallpaper(wallpaper: Wallpaper)

    /**
     * Cascade delete wallpaper. Proper way to delete a single wallpaper
     */
    suspend fun cascadeDeleteWallpaper(initialAlbumName: String)

    /**
     * Insert or update folder
     */
    suspend fun upsertFolder(folder: Folder)

    /**
     * Insert or update folder list in one transaction
     */
    suspend fun upsertFolderList(folders: List<Folder>)

    /**
     * Delete folder
     */
    suspend fun deleteFolder(folder: Folder)

    /**
     * Update folder
     */
    suspend fun updateFolder(folder: Folder)

    /**
     * Cascade delete folder. Proper way to delete a single folder
     */
    suspend fun cascadeDeleteFolder(initialAlbumName: String)

    /**
     * Delete folder list in one transaction
     */
    suspend fun deleteFolderList(folders: List<Folder>)

    /**
     * Delete all data in database
     */
    suspend fun deleteAllData()
}