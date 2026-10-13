package com.anthonyla.paperize.domain.usecase

import android.graphics.Bitmap
import com.anthonyla.paperize.core.NoValidWallpaperException
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.domain.source.WallpaperRenderer
import javax.inject.Inject

class ReapplyEffectsUseCase @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val settingsRepository: SettingsRepository,
    private val renderer: WallpaperRenderer
) {
    suspend operator fun invoke(albumId: String, screenType: ScreenType, wallpaperId: String? = null): Bitmap {
        val wallpaper = if (wallpaperId != null) wallpaperRepository.getWallpaperById(wallpaperId)
            else wallpaperRepository.getCurrentWallpaper(albumId, screenType)
        return wallpaper?.let { renderer.render(it, screenType, settingsRepository.getScheduleSettings()) }
            ?: throw NoValidWallpaperException()
    }
}
