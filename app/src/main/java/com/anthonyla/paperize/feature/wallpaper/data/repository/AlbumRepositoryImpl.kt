package com.anthonyla.paperize.feature.wallpaper.data.repository

import com.anthonyla.paperize.feature.wallpaper.data.data_source.AlbumDao
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithImages
import com.anthonyla.paperize.feature.wallpaper.domain.model.Image
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow

class AlbumRepositoryImpl(
    private val dao: AlbumDao
): AlbumRepository {
    override fun getAlbums(): Flow<List<AlbumWithImages>> {
        return dao.getAlbumsWithImages()
    }

    override suspend fun insertAlbum(album: Album) : Long {
        return dao.upsertAlbum(album)
    }

    override suspend fun insertImage(image: Image) {
        dao.upsertImage(image)
    }

    override suspend fun deleteAlbum(album: Album) {
        dao.deleteAlbum(album)
    }

    override suspend fun deleteImage(image: Image) {
        dao.deleteImage(image)
    }
}