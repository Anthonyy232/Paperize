package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen
import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.core.SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT

data class SettingsState(
    val firstLaunch: Boolean = true,
    val firstSet: Boolean = true,
    // App settings
    val darkMode: Boolean? = null,
    val dynamicTheming: Boolean = false,
    val animate: Boolean = true,
    // Wallpaper settings
    val enableChanger: Boolean = false,
    val setHomeWallpaper: Boolean = false,
    val setLockWallpaper: Boolean = false,
    val scheduleSeparately: Boolean = false,
    val homeInterval: Int = WALLPAPER_CHANGE_INTERVAL_DEFAULT,
    val lockInterval: Int = WALLPAPER_CHANGE_INTERVAL_DEFAULT,
    val lastSetTime: String? = null,
    val nextSetTime: String? = null,
    val darkenPercentage: Int = 100,
    val darken: Boolean = false,
    val blur: Boolean = false,
    val blurPercentage: Int = 0,
    val wallpaperScaling: ScalingConstants = ScalingConstants.FILL
)