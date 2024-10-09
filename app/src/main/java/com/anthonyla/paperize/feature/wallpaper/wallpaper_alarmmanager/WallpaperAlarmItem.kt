package com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager

import com.anthonyla.paperize.core.SettingsConstants

/**
 * Data class for storing wallpaper alarm item
 * @param homeInterval: Int - time in minutes for home wallpaper or both if scheduled together
 * @param lockInterval: Int - time in minutes for lock wallpaper
 * @param scheduleSeparately: Boolean - schedule wallpapers separately
 * @param setHome: Boolean - set home wallpaper
 * @param setLock: Boolean - set lock wallpaper
 */
data class WallpaperAlarmItem(
    val homeInterval: Int = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT,
    val lockInterval: Int = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT,
    val scheduleSeparately: Boolean = false,
    val setHome : Boolean = false,
    val setLock : Boolean = false,
    val changeStartTime: Boolean = false,
    val startTime: Pair<Int, Int> = Pair(0, 0),
)