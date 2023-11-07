package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

data class SettingsState (
    val darkMode: Boolean? = null,
    val dynamicTheming: Boolean = false,
    val loaded: Boolean = false
)