package com.anthonyla.paperize.domain.repository

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.model.Folder
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Album operations
 *
 * Clean interface following repository pattern - implementation in data layer
 */
interface AlbumRepository {
    /**
     * Get all albums with their wallpapers and folders
     */
    fun getAllAlbums(): Flow<List<Album>>

    /**
     * Get a specific album by ID
     */
    fun getAlbumById(albumId: String): Flow<Album?>

    /**
     * Get a specific folder by ID
     */
    fun getFolderById(folderId: String): Flow<Folder?>

    /**
     * Create a new album
     */
    suspend fun createAlbum(name: String, coverUri: String? = null): Result<Album>

    /**
     * Update album
     */
    suspend fun updateAlbum(album: Album): Result<Unit>

    /**
     * Delete album
     */
    suspend fun deleteAlbum(albumId: String): Result<Unit>

    /**
     * Update album name
     */
    suspend fun updateAlbumName(albumId: String, name: String): Result<Unit>

    /**
     * Update album cover
     */
    suspend fun updateAlbumCover(albumId: String, coverUri: String?): Result<Unit>

    /**
     * Add wallpapers to album
     */
    suspend fun addWallpapersToAlbum(
        albumId: String,
        wallpapers: List<Wallpaper>
    ): Result<Unit>

    /**
     * Add folder to album
     */
    suspend fun addFolderToAlbum(albumId: String, folder: Folder): Result<Unit>

    /**
     * Update folder
     */
    suspend fun updateFolder(folder: Folder): Result<Unit>

    /**
     * Remove wallpaper from album
     */
    suspend fun removeWallpaperFromAlbum(albumId: String, wallpaperId: String): Result<Unit>

    /**
     * Remove multiple wallpapers from album (batch operation)
     * Updates album modified time and refreshes cover art
     */
    suspend fun removeWallpapersFromAlbum(albumId: String, wallpaperIds: List<String>): Result<Unit>

    /**
     * Remove folder from album
     */
    suspend fun removeFolderFromAlbum(albumId: String, folderId: String): Result<Unit>

    /**
     * Get album count
     */
    suspend fun getAlbumCount(): Int

    /**
     * Delete all albums
     */
    suspend fun deleteAllAlbums(): Result<Unit>

    /**
     * Validate folder URIs and remove invalid ones
     * Returns number of folders removed
     */
    suspend fun validateAndRemoveInvalidFolders(albumId: String): Result<Int>

    /**
     * Refresh album cover art based on current wallpapers
     */
    suspend fun refreshAlbumCover(albumId: String): Result<Unit>

    /**
     * Refresh all folder covers in an album based on their current wallpapers
     */
    suspend fun refreshFolderCovers(albumId: String): Result<Unit>
}
