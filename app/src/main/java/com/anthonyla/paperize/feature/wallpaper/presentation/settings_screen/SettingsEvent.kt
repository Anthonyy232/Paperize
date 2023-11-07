package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen


sealed class SettingsEvent {
    data object RefreshUiState: SettingsEvent()
    data class SetDarkMode(val darkMode: Boolean?): SettingsEvent()
    data class SetDynamicTheming(val dynamicTheming: Boolean): SettingsEvent()
}