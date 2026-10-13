package com.anthonyla.paperize.domain.repository

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

    suspend fun createAlbum(name: String, coverUri: String? = null): Album

    suspend fun deleteAlbum(albumId: String)

    /** Returns the number of new images. Progress counts rows written within the transaction. */
    suspend fun addWallpapersToAlbum(
        albumId: String,
        wallpapers: List<Wallpaper>,
        onProgress: (saved: Int, total: Int) -> Unit = { _, _ -> }
    ): Int

    /** Returns false for an existing folder URI. Progress counts images written within the transaction. */
    suspend fun addFolderToAlbum(
        albumId: String,
        folder: Folder,
        onProgress: (saved: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean

    suspend fun reorderAlbum(albumId: String, folders: List<Folder>, wallpapers: List<Wallpaper>)

    suspend fun removeWallpapersFromAlbum(albumId: String, wallpaperIds: List<String>)

    suspend fun removeFolderFromAlbum(albumId: String, folderId: String)

    suspend fun deleteAllAlbums()

    /** Removes confirmed missing entries and updates covers in the same transaction. */
    suspend fun pruneMissingEntries(albumId: String): Int
}
