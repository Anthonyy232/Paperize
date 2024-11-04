package com.anthonyla.paperize.feature.wallpaper.data.repository

import com.anthonyla.paperize.feature.wallpaper.data.data_source.SelectedAlbumDao
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SelectedAlbumRepositoryImpl(
    private val dao: SelectedAlbumDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): SelectedAlbumRepository {
    override fun getSelectedAlbum(): Flow<List<SelectedAlbum>> {
        return dao.getSelectedAlbum()
    }

    override suspend fun upsertSelectedAlbum(selectedAlbum: SelectedAlbum) {
        withContext(dispatcher) {
            dao.upsertSelectedAlbum(selectedAlbum)
        }
    }

    override suspend fun deleteAlbum(initialAlbumName: String) {
        withContext(dispatcher) {
            dao.deleteAlbum(initialAlbumName)
        }
    }

    override suspend fun deleteWallpaper(wallpaper: Wallpaper) {
        withContext(dispatcher) {
            dao.deleteWallpaper(wallpaper)
        }
    }

    override suspend fun cascadeDeleteAlbum(initialAlbumName: String) {
        withContext(dispatcher) {
            dao.deleteAlbum(initialAlbumName)
            dao.cascadeDeleteWallpaper(initialAlbumName)
        }
    }

    override suspend fun deleteAll() {
        withContext(dispatcher) {
            dao.deleteAll()
        }
    }
}