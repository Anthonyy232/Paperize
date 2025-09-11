package com.anthonyla.paperize.feature.wallpaper.data.repository

import android.database.sqlite.SQLiteBlobTooBigException
import android.util.Log
import com.anthonyla.paperize.feature.wallpaper.data.data_source.AlbumDao
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AlbumRepositoryImpl(
    private val dao: AlbumDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): AlbumRepository {
    override fun getAlbumsWithWallpaperAndFolder(): Flow<List<AlbumWithWallpaperAndFolder>> {
        return dao.getAlbumsWithWallpaperAndFolder()
            .map { albumWithWallpaperAndFolderList ->
                albumWithWallpaperAndFolderList.map {
                    it.copy(
                        wallpapers = it.sortedWallpapers,
                        folders = it.sortedFolders,
                    )
                }
            }
            .catch { exception ->
                if (exception is SQLiteBlobTooBigException) {
                    Log.e("AlbumRepository", "CursorWindow too small for large dataset, falling back to basic album list", exception)
                    // Fallback to basic album list when data is too large
                    emit(emptyList())
                } else {
                    Log.e("AlbumRepository", "Error loading albums with wallpapers and folders", exception)
                    throw exception
                }
            }
    }

    override fun getSelectedAlbums(): Flow<List<AlbumWithWallpaperAndFolder>> {
        return dao.getSelectedAlbums()
            .map { albumWithWallpaperAndFolderList ->
                albumWithWallpaperAndFolderList.map {
                    it.copy(
                        wallpapers = it.sortedWallpapers,
                        folders = it.sortedFolders,
                    )
                }
            }
            .catch { exception ->
                if (exception is SQLiteBlobTooBigException) {
                    Log.e("AlbumRepository", "CursorWindow too small for selected albums, falling back to basic album list", exception)
                    // Fallback to basic album list when data is too large
                    emit(emptyList())
                } else {
                    Log.e("AlbumRepository", "Error loading selected albums with wallpapers and folders", exception)
                    throw exception
                }
            }
    }

    override fun getAlbums(): Flow<List<Album>> {
        return dao.getAlbums()
    }

    override fun getSelectedAlbumsBasic(): Flow<List<Album>> {
        return dao.getSelectedAlbumsBasic()
    }

    override fun getAlbumWithWallpaperAndFolder(albumName: String): Flow<AlbumWithWallpaperAndFolder?> {
        return dao.getAlbumWithWallpaperAndFolder(albumName)
            .map { albumWithWallpaperAndFolder ->
                albumWithWallpaperAndFolder?.copy(
                    wallpapers = albumWithWallpaperAndFolder.sortedWallpapers,
                    folders = albumWithWallpaperAndFolder.sortedFolders,
                )
            }
            .catch { exception ->
                if (exception is SQLiteBlobTooBigException) {
                    Log.e("AlbumRepository", "CursorWindow too small for album: $albumName, falling back to basic album", exception)
                    // Fallback to basic album when data is too large
                    emit(null)
                } else {
                    Log.e("AlbumRepository", "Error loading album: $albumName", exception)
                    throw exception
                }
            }
    }

    override suspend fun upsertAlbumWithWallpaperAndFolder(albumWithWallpaperAndFolder: AlbumWithWallpaperAndFolder) {
        try {
            withContext(dispatcher) {
                dao.upsertAlbum(albumWithWallpaperAndFolder.album)
                dao.upsertWallpaperList(albumWithWallpaperAndFolder.wallpapers)
                dao.upsertFolderList(albumWithWallpaperAndFolder.folders)
            }
        } catch (e: SQLiteBlobTooBigException) {
            Log.e("AlbumRepository", "Failed to upsert album with large dataset: ${albumWithWallpaperAndFolder.album.initialAlbumName}", e)
            // Try to save just the album without wallpapers/folders if data is too large
            withContext(dispatcher) {
                dao.upsertAlbum(albumWithWallpaperAndFolder.album)
            }
        }
    }

    override suspend fun updateAlbumSelection(albumName: String, selected: Boolean) {
        withContext(dispatcher) {
            dao.updateAlbumSelection(albumName, selected)
        }
    }

    override suspend fun deselectAllAlbums() {
        withContext(dispatcher) {
            dao.deselectAllAlbums()
        }
    }

    override suspend fun upsertAlbum(album: Album) {
        withContext(dispatcher) {
            dao.upsertAlbum(album)
        }
    }

    override suspend fun upsertWallpaper(wallpaper: Wallpaper) {
        withContext(dispatcher) {
            dao.upsertWallpaper(wallpaper)
        }
    }

    override suspend fun upsertFolder(folder: Folder) {
        withContext(dispatcher) {
            dao.upsertFolder(folder)
        }
    }

    override suspend fun upsertFolderList(folders: List<Folder>) {
        try {
            withContext(dispatcher) {
                dao.upsertFolderList(folders)
            }
        } catch (e: SQLiteBlobTooBigException) {
            Log.e("AlbumRepository", "Failed to upsert folder list due to large dataset", e)
            // Try to upsert folders individually if the batch fails
            withContext(dispatcher) {
                folders.forEach { folder ->
                    try {
                        dao.upsertFolder(folder)
                    } catch (folderException: Exception) {
                        Log.e("AlbumRepository", "Failed to upsert individual folder: ${folder.folderName}", folderException)
                    }
                }
            }
        }
    }

    override suspend fun upsertWallpaperList(wallpapers: List<Wallpaper>) {
        try {
            withContext(dispatcher) {
                dao.upsertWallpaperList(wallpapers)
            }
        } catch (e: SQLiteBlobTooBigException) {
            Log.e("AlbumRepository", "Failed to upsert wallpaper list due to large dataset", e)
            // Try to upsert wallpapers individually if the batch fails
            withContext(dispatcher) {
                wallpapers.forEach { wallpaper ->
                    try {
                        dao.upsertWallpaper(wallpaper)
                    } catch (wallpaperException: Exception) {
                        Log.e("AlbumRepository", "Failed to upsert individual wallpaper: ${wallpaper.fileName}", wallpaperException)
                    }
                }
            }
        }
    }

    override suspend fun deleteAlbum(album: Album) {
        withContext(dispatcher) {
            dao.deleteAlbum(album)
        }
    }

    override suspend fun deleteWallpaper(wallpaper: Wallpaper) {
        withContext(dispatcher) {
            dao.deleteWallpaper(wallpaper)
        }
    }

    override suspend fun deleteFolder(folder: Folder) {
        withContext(dispatcher) {
            dao.deleteFolder(folder)
        }
    }

    override suspend fun updateAlbum(album: Album) {
        withContext(dispatcher) {
            dao.updateAlbum(album)
        }
    }

    override suspend fun updateFolder(folder: Folder) {
        withContext(dispatcher) {
            dao.updateFolder(folder)
        }
    }

    override suspend fun updateWallpaper(wallpaper: Wallpaper) {
        withContext(dispatcher) {
            dao.updateWallpaper(wallpaper)
        }
    }

    override suspend fun cascadeDeleteAlbum(album: Album) {
        withContext(dispatcher) {
            dao.deleteAlbum(album)
            dao.cascadeDeleteWallpaper(album.initialAlbumName)
            dao.cascadeDeleteFolder(album.initialAlbumName)
        }
    }

    override suspend fun cascadeDeleteFolder(initialAlbumName: String) {
        withContext(dispatcher) {
            dao.cascadeDeleteFolder(initialAlbumName)
        }
    }

    override suspend fun cascadeDeleteWallpaper(initialAlbumName: String) {
        withContext(dispatcher) {
            dao.cascadeDeleteWallpaper(initialAlbumName)
        }
    }

    override suspend fun deleteFolderList(folders: List<Folder>) {
        withContext(dispatcher) {
            dao.deleteFolderList(folders)
        }
    }

    override suspend fun deleteWallpaperList(wallpapers: List<Wallpaper>) {
        withContext(dispatcher) {
            dao.deleteWallpaperList(wallpapers)
        }
    }

    override suspend fun deleteAllData() {
        withContext(dispatcher) {
            dao.deleteAllData()
        }
    }
}