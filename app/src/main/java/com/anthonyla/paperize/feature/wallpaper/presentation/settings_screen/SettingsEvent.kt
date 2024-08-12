package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import com.anthonyla.paperize.core.ScalingConstants


sealed class SettingsEvent {
    data object Refresh: SettingsEvent()
    data object Reset: SettingsEvent()
    data object SetFirstLaunch: SettingsEvent()
    data object RefreshNextSetTime: SettingsEvent()
    data class SetDarkMode(val darkMode: Boolean?): SettingsEvent()
    data class SetAmoledTheme(val amoledTheme: Boolean): SettingsEvent()
    data class SetDynamicTheming(val dynamicTheming: Boolean): SettingsEvent()
    data class SetAnimate(val animate: Boolean): SettingsEvent()
    data class SetDarken(val darken: Boolean): SettingsEvent()
    data class SetBlur(val blur: Boolean): SettingsEvent()
    data class SetLock(val lock: Boolean): SettingsEvent()
    data class SetHome(val home: Boolean): SettingsEvent()
    data class SetCurrentHomeWallpaper(val currentHomeWallpaper: String?): SettingsEvent()
    data class SetCurrentLockWallpaper(val currentLockWallpaper: String?): SettingsEvent()
    data class SetCurrentWallpaper(val currentHomeWallpaper: String?, val currentLockWallpaper: String?): SettingsEvent()
    data class SetAlbumName(val homeAlbumName: String? = null, val lockAlbumName: String? = null): SettingsEvent()
    data class RemoveSelectedAlbumAsType(val removeLock: Boolean = false, val removeHome: Boolean = false): SettingsEvent()
    data class RemoveSelectedAlbumAsName(val albumName: String): SettingsEvent()
    data class SetScheduleSeparately(val scheduleSeparately: Boolean): SettingsEvent()
    data class SetHomeWallpaperInterval(val interval: Int): SettingsEvent()
    data class SetLockWallpaperInterval(val interval: Int): SettingsEvent()
    data class SetDarkenPercentage(val darkenPercentage: Int): SettingsEvent()
    data class SetBlurPercentage(val blurPercentage: Int): SettingsEvent()
    data class SetChangerToggle(val toggle: Boolean): SettingsEvent()
    data class SetWallpaperScaling(val scaling: ScalingConstants): SettingsEvent()
    data class SetNextHomeWallpaper(val nextHomeWallpaper: String?): SettingsEvent()
    data class SetNextLockWallpaper(val nextLockWallpaper: String?): SettingsEvent()
    data class SetNextWallpaper(val nextHomeWallpaper: String?, val nextLockWallpaper: String?): SettingsEvent()
    data object RefreshNextWallpaper: SettingsEvent()
}