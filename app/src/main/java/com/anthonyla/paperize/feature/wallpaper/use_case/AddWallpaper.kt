package com.anthonyla.paperize.feature.wallpaper.use_case

import android.net.Uri
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.WallpaperRepository
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper.WallpaperEvent

class AddWallpaper(private val repository: WallpaperRepository) {
    suspend operator fun invoke(uriString: String) {
        val wallpaper = Wallpaper(imageUri = uriString, date = "today")
        repository.insertWallpaper(wallpaper)
    }
}