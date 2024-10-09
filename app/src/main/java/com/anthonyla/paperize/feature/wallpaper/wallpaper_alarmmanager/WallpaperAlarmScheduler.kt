package com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager

interface WallpaperAlarmScheduler {
    /**
     * Schedule a wallpaper alarm to change the wallpaper
     * @param wallpaperAlarmItem the wallpaper alarm item to schedule
     * @param origin where the request came from (null if not from a specific origin, 0 if home screen, 1 if lock screen, 2 if both)
     * @param changeImmediate whether to change the wallpaper immediately or just schedule the alarm
     * @param cancelImmediate whether to cancel all alarms before scheduling the new one
     * @param setAlarm whether to set the alarm or not
     * @param firstLaunch whether this is the first launch of the wallpaper changer
     */
    fun scheduleWallpaperAlarm(wallpaperAlarmItem: WallpaperAlarmItem, origin: Int? = null, changeImmediate: Boolean = false, cancelImmediate: Boolean = false, setAlarm: Boolean = true, firstLaunch: Boolean = false)

    /**
     * Update the wallpaper alarm with new times without changing the wallpaper
     */
    fun updateWallpaperAlarm(wallpaperAlarmItem: WallpaperAlarmItem, firstLaunch: Boolean = false)

    /**
     * Update the wallpaper without changing the alarm
     */
    fun updateWallpaper(scheduleSeparately: Boolean, setHome: Boolean, setLock: Boolean)

    /**
     * Cancel all wallpaper alarms
     */
    fun cancelWallpaperAlarm(cancelLock: Boolean = true, cancelHome: Boolean = true)
}