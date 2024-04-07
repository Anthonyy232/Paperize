package com.anthonyla.paperize.feature.wallpaper.data.repository

import com.anthonyla.paperize.feature.wallpaper.data.data_source.SelectedAlbumDao
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import kotlinx.coroutines.flow.Flow

class SelectedAlbumRepositoryImpl(
    private val dao: SelectedAlbumDao
): SelectedAlbumRepository {
    override fun getSelectedAlbum(): Flow<List<SelectedAlbum>> {
        return dao.getSelectedAlbum()
    }

    override suspend fun upsertSelectedAlbum(selectedAlbum: SelectedAlbum) {
        dao.upsertAlbum(selectedAlbum.album)
        dao.upsertWallpaperList(selectedAlbum.wallpapers)
    }

    override suspend fun upsertAlbum(album: Album) {
        dao.upsertAlbum(album)
    }

    override suspend fun upsertWallpaper(wallpaper: Wallpaper) {
        dao.upsertWallpaper(wallpaper)
    }

    override suspend fun upsertWallpaperList(wallpapers: List<Wallpaper>) {
        dao.upsertWallpaperList(wallpapers)
    }

    override suspend fun updateAlbum(album: Album) {
        dao.updateAlbum(album)
    }

    override suspend fun deleteAlbum(album: Album) {
        dao.deleteAlbum(album)
    }

    override suspend fun deleteWallpaper(wallpaper: Wallpaper) {
        dao.deleteWallpaper(wallpaper)
    }

    override suspend fun cascadeDeleteWallpaper(initialAlbumName: String) {
        dao.cascadeDeleteWallpaper(initialAlbumName)
    }
    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}