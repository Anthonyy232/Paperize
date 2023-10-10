package com.anthonyla.paperize.feature.wallpaper.domain.repository

import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {
    fun getWallpapers(): Flow<List<Wallpaper>>

    suspend fun insertWallpaper(wallpaper: Wallpaper)

    suspend fun deleteWallpaper(wallpaper: Wallpaper)
}