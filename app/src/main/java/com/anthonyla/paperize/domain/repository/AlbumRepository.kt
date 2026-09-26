package com.anthonyla.paperize.domain.repository

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    fun getAlbumSummaries(): Flow<List<AlbumSummary>>

    fun getAlbumById(albumId: String): Flow<Album?>

    suspend fun getAlbumByName(name: String): Album?

    fun getFolderById(folderId: String): Flow<Folder?>

    suspend fun createAlbum(name: String, coverUri: String? = null): Result<Album>

    suspend fun deleteAlbum(albumId: String): Result<Unit>

    /** Returns the number of new images. Progress counts rows written within the transaction. */
    suspend fun addWallpapersToAlbum(
        albumId: String,
        wallpapers: List<Wallpaper>,
        onProgress: (saved: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Int>

    /** Returns false for an existing folder URI. Progress counts images written within the transaction. */
    suspend fun addFolderToAlbum(
        albumId: String,
        folder: Folder,
        onProgress: (saved: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Boolean>

    suspend fun reorderAlbum(albumId: String, folders: List<Folder>, wallpapers: List<Wallpaper>): Result<Unit>

    suspend fun removeWallpapersFromAlbum(albumId: String, wallpaperIds: List<String>): Result<Unit>

    suspend fun removeFolderFromAlbum(albumId: String, folderId: String): Result<Unit>

    suspend fun deleteAllAlbums(): Result<Unit>

    /** Removes confirmed missing entries and updates covers in the same transaction. */
    suspend fun pruneMissingEntries(albumId: String): Result<Int>
}
