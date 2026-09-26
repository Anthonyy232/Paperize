package com.anthonyla.paperize.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.util.WallpaperRenderer
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ReapplyEffectsUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val wallpaperRepository: WallpaperRepository,
    private val settingsRepository: SettingsRepository,
    private val renderer: WallpaperRenderer
) {
    suspend operator fun invoke(albumId: String, screenType: ScreenType, wallpaperId: String? = null): Result<Bitmap> =
        Result.runCatching {
            val wallpaper = if (wallpaperId != null) wallpaperRepository.getWallpaperById(wallpaperId)
                else wallpaperRepository.getCurrentWallpaper(albumId, screenType)
            val message = context.getString(R.string.error_no_valid_wallpaper_after_retries)
            checkNotNull(wallpaper) { message }
            checkNotNull(renderer.render(wallpaper, screenType, settingsRepository.getScheduleSettings())) { message }
        }
}
