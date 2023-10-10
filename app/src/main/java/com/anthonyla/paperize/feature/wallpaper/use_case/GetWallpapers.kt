package com.anthonyla.paperize.feature.wallpaper.use_case

import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.Flow

class GetWallpapers(private val repository: WallpaperRepository) {
    operator fun invoke(): Flow<List<Wallpaper>> {
        return repository.getWallpapers()
    }
}