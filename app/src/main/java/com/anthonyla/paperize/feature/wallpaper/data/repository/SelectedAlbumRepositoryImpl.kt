package com.anthonyla.paperize.feature.wallpaper.data.repository

import com.anthonyla.paperize.feature.wallpaper.data.data_source.SelectedAlbumDao
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
        dao.upsertSelectedAlbum(selectedAlbum)
    }

    override suspend fun deleteAlbum(initialAlbumName: String) {
        dao.deleteAlbum(initialAlbumName)
    }

    override suspend fun deleteWallpaper(wallpaper: Wallpaper) {
        dao.deleteWallpaper(wallpaper)
    }

    override suspend fun cascadeDeleteAlbum(initialAlbumName: String) {
        dao.deleteAlbum(initialAlbumName)
        dao.cascadeDeleteWallpaper(initialAlbumName)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}