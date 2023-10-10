package com.anthonyla.paperize.feature.wallpaper.data.repository

import com.anthonyla.paperize.feature.wallpaper.data.data_source.WallpaperDao
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.Flow

class WallpaperRepositoryImpl(
    private val dao: WallpaperDao
): WallpaperRepository {
    override fun getWallpapers(): Flow<List<Wallpaper>> {
        return dao.getImages()
    }

    override suspend fun insertWallpaper(wallpaper: Wallpaper) {
        dao.upsertImage(wallpaper)
    }

    override suspend fun deleteWallpaper(wallpaper: Wallpaper) {
        dao.deleteImage(wallpaper)
    }
}