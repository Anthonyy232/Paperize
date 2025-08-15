package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.core.SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT

data class SettingsState(
    val firstLaunch: Boolean = true,
    val initialized: Boolean = false,
    val themeSettings: ThemeSettings = ThemeSettings(),
    val wallpaperSettings: WallpaperSettings = WallpaperSettings(),
    val scheduleSettings: ScheduleSettings = ScheduleSettings(),
    val effectSettings: EffectSettings = EffectSettings()
) {
    data class ThemeSettings(
        val darkMode: Boolean? = null,
        val amoledTheme: Boolean = false,
        val dynamicTheming: Boolean = false,
        val animate: Boolean = true
    )

    data class WallpaperSettings(
        val enableChanger: Boolean = false,
        val setHomeWallpaper: Boolean = false,
        val setLockWallpaper: Boolean = false,
        val currentHomeWallpaper: String? = null,
        val currentLockWallpaper: String? = null,
        val nextHomeWallpaper: String? = null,
        val nextLockWallpaper: String? = null,
        val homeAlbumName: String? = null,
        val lockAlbumName: String? = null,
        val wallpaperScaling: ScalingConstants = ScalingConstants.FILL
    )

    data class ScheduleSettings(
        val scheduleSeparately: Boolean = false,
        val shuffle: Boolean = true,
        val homeInterval: Int = WALLPAPER_CHANGE_INTERVAL_DEFAULT,
        val lockInterval: Int = WALLPAPER_CHANGE_INTERVAL_DEFAULT,
        val lastSetTime: String? = null,
        val nextSetTime: String? = null,
        val changeStartTime: Boolean = false,
        val startTime: Pair<Int, Int> = Pair(0, 0),
        val refresh: Boolean = true,
        val skipLandscape: Boolean = false
    )

    data class EffectSettings(
        val darken: Boolean = false,
        val homeDarkenPercentage: Int = 100,
        val lockDarkenPercentage: Int = 100,
        val blur: Boolean = false,
        val homeBlurPercentage: Int = 0,
        val lockBlurPercentage: Int = 0,
        val vignette: Boolean = false,
        val homeVignettePercentage: Int = 0,
        val lockVignettePercentage: Int = 0,
        val grayscale: Boolean = false,
        val homeGrayscalePercentage: Int = 0,
        val lockGrayscalePercentage: Int = 0
    )

    data class ServiceSettings(
        val enableChanger: Boolean,
        val setHome: Boolean,
        val setLock: Boolean,
        val scaling: ScalingConstants,
        val darken: Boolean,
        val homeDarkenPercentage: Int,
        val lockDarkenPercentage: Int,
        val blur: Boolean,
        val homeBlurPercentage: Int,
        val lockBlurPercentage: Int,
        val vignette: Boolean,
        val homeVignettePercentage: Int,
        val lockVignettePercentage: Int,
        val grayscale: Boolean,
        val homeGrayscalePercentage: Int,
        val lockGrayscalePercentage: Int,
        val lockAlbumName: String,
        val homeAlbumName: String,
        val shuffle: Boolean,
        val skipLandscape: Boolean
    )
}