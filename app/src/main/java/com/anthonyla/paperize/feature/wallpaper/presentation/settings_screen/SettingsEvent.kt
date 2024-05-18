package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import com.anthonyla.paperize.core.ScalingConstants


sealed class SettingsEvent {
    data object Refresh: SettingsEvent()
    data object Reset: SettingsEvent()
    data object SetFirstLaunch: SettingsEvent()
    data class SetDarkMode(val darkMode: Boolean?): SettingsEvent()
    data class SetDynamicTheming(val dynamicTheming: Boolean): SettingsEvent()
    data class SetAnimate(val animate: Boolean): SettingsEvent()
    data class SetDarken(val darken: Boolean): SettingsEvent()
    data class SetWallpaperInterval(val interval: Int): SettingsEvent()
    data class SetDarkenPercentage(val darkenPercentage: Int): SettingsEvent()
    data class SetLockWithHome(val lockWithHome: Boolean): SettingsEvent()
    data class SetChangerToggle(val toggle: Boolean): SettingsEvent()
    data class SetWallpaperScaling(val scaling: ScalingConstants): SettingsEvent()
}