package com.anthonyla.paperize.feature.wallpaper.presentation.settings

import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper

sealed class SettingsEvent {
    data class SetDarkMode(val darkMode: Boolean?): SettingsEvent()
    data class SetDynamicTheming(val dynamicTheming: Boolean): SettingsEvent()
}