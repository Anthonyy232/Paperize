package com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager

import com.anthonyla.paperize.core.SettingsConstants

/**
 * Data class for storing wallpaper alarm item
 * @param timeInMinutes1: Int - time in minutes for home wallpaper or both if scheduled together
 * @param timeInMinutes2: Int - time in minutes for lock wallpaper
 * @param scheduleSeparately: Boolean - schedule wallpapers separately
 */
data class WallpaperAlarmItem(
    val timeInMinutes1: Int = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT,
    val timeInMinutes2: Int = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT,
    val scheduleSeparately: Boolean = false
)