package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import com.anthonyla.paperize.core.ScalingConstants


sealed class SettingsEvent {
    data object Reset: SettingsEvent()
    data object SetFirstLaunch: SettingsEvent()
    data object RefreshNextSetTime: SettingsEvent()
    data class SetDarkMode(val darkMode: Boolean?): SettingsEvent()
    data class SetAmoledTheme(val amoledTheme: Boolean): SettingsEvent()
    data class SetDynamicTheming(val dynamicTheming: Boolean): SettingsEvent()
    data class SetAnimate(val animate: Boolean): SettingsEvent()
    data class SetDarken(val darken: Boolean): SettingsEvent()
    data class SetBlur(val blur: Boolean): SettingsEvent()
    data class SetVignette(val vignette: Boolean): SettingsEvent()
    data class SetGrayscale(val grayscale: Boolean): SettingsEvent()
    data class SetLock(val lock: Boolean): SettingsEvent()
    data class SetHome(val home: Boolean): SettingsEvent()
    data class SetChangeStartTime(val changeStartTime: Boolean): SettingsEvent()
    data class SetCurrentWallpaper(val currentHomeWallpaper: String?, val currentLockWallpaper: String?): SettingsEvent()
    data class SetAlbum(val homeAlbumName: String? = null, val lockAlbumName: String? = null): SettingsEvent()
    data class RemoveSelectedAlbumAsType(val removeLock: Boolean = false, val removeHome: Boolean = false): SettingsEvent()
    data class RemoveSelectedAlbumAsName(val albumName: String): SettingsEvent()
    data class SetScheduleSeparately(val scheduleSeparately: Boolean): SettingsEvent()
    data class SetHomeWallpaperInterval(val interval: Int): SettingsEvent()
    data class SetLockWallpaperInterval(val interval: Int): SettingsEvent()
    data class SetDarkenPercentage(val homeDarkenPercentage: Int?, val lockDarkenPercentage: Int?): SettingsEvent()
    data class SetBlurPercentage(val homeBlurPercentage: Int?, val lockBlurPercentage: Int?): SettingsEvent()
    data class SetVignettePercentage(val homeVignettePercentage: Int?, val lockVignettePercentage: Int?): SettingsEvent()
    data class SetGrayscalePercentage(val homeGrayscalePercentage: Int?, val lockGrayscalePercentage: Int?): SettingsEvent()
    data class SetChangerToggle(val toggle: Boolean): SettingsEvent()
    data class SetWallpaperScaling(val scaling: ScalingConstants): SettingsEvent()
    data class SetStartTime(val hour: Int, val minute: Int): SettingsEvent()
    data class SetShuffle(val shuffle: Boolean): SettingsEvent()
    data class SetRefresh(val refresh: Boolean): SettingsEvent()
}