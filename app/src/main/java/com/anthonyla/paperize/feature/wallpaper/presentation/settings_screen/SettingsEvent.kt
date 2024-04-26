package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen


sealed class SettingsEvent {
    data object RefreshUiState: SettingsEvent()
    data object RefreshWallpaperState: SettingsEvent()
    data class SetDarkMode(val darkMode: Boolean?): SettingsEvent()
    data class SetDynamicTheming(val dynamicTheming: Boolean): SettingsEvent()
    data class SetWallpaperInterval(val interval: Int): SettingsEvent()
}