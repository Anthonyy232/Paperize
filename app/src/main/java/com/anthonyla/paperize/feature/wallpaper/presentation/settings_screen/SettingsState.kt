package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen
import com.anthonyla.paperize.core.SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT

data class SettingsState (
    val firstLaunch: Boolean = true,
    val darkMode: Boolean? = null,
    val dynamicTheming: Boolean = false,
    val interval: Int = WALLPAPER_CHANGE_INTERVAL_DEFAULT,
    val setLockWithHome: Boolean = false,
)