package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen


sealed class SettingsEvent {
    data object Refresh: SettingsEvent()
    data object SetFirstLaunch: SettingsEvent()
    data class SetDarkMode(val darkMode: Boolean?): SettingsEvent()
    data class SetDynamicTheming(val dynamicTheming: Boolean): SettingsEvent()
    data class SetAnimate(val animate: Boolean): SettingsEvent()
    data class SetWallpaperInterval(val interval: Int): SettingsEvent()
    data class SetLockWithHome(val lockWithHome: Boolean): SettingsEvent()
}