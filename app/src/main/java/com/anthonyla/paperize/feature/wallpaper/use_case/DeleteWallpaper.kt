package com.anthonyla.paperize.feature.wallpaper.use_case

import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.WallpaperRepository

class DeleteWallpaper(private val repository: WallpaperRepository) {
    suspend operator fun invoke(uriString: String) {
        val wallpaper = Wallpaper(imageUri = uriString, date = "today")
        repository.deleteWallpaper(wallpaper)
    }
}