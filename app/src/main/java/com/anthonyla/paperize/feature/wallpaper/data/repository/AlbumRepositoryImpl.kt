package com.anthonyla.paperize.feature.wallpaper.data.repository

import com.anthonyla.paperize.feature.wallpaper.data.data_source.AlbumDao
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow

class AlbumRepositoryImpl(
    private val dao: AlbumDao
): AlbumRepository {
    override fun getAlbumsWithWallpapers(): Flow<List<AlbumWithWallpaper>> {
        return dao.getAlbumsWithWallpapers()
    }

    override suspend fun upsertAlbum(album: Album) {
        dao.upsertAlbum(album)
    }

    override suspend fun upsertWallpaper(wallpaper: Wallpaper) {
        dao.upsertWallpaper(wallpaper)
    }

    override suspend fun upsertFolder(folder: Folder) {
        dao.upsertFolder(folder)
    }

    override suspend fun deleteAlbum(album: Album) {
        dao.deleteAlbum(album)
    }

    override suspend fun deleteWallpaper(wallpaper: Wallpaper) {
        dao.deleteWallpaper(wallpaper)
    }

    override suspend fun deleteFolder(folder: Folder) {
        dao.deleteFolder(folder)
    }
}