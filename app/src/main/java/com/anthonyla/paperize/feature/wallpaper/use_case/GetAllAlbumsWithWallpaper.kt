package com.anthonyla.paperize.feature.wallpaper.use_case

import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class GetAllAlbumsWithWallpaper(private val repository: AlbumRepository) {
    operator fun invoke(): Flow<List<AlbumWithWallpaper>> {
        return repository.getAlbumsWithWallpapers().flowOn(Dispatchers.IO)
    }
}