package com.anthonyla.paperize.domain.model

import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.constants.Constants

data class ScheduleSettings(
    val enableChanger: Boolean = false,
    val separateSchedules: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val homeEnabled: Boolean = false,
    val lockEnabled: Boolean = false,
    val homeAlbumId: String? = null,
    val lockAlbumId: String? = null,
    val homeIntervalMinutes: Int = Constants.DEFAULT_INTERVAL_MINUTES,
    val lockIntervalMinutes: Int = Constants.DEFAULT_INTERVAL_MINUTES,
    val liveIntervalMinutes: Int = Constants.DEFAULT_INTERVAL_MINUTES,
    val homeScalingType: ScalingType = ScalingType.FILL,
    val lockScalingType: ScalingType = ScalingType.FILL,
    val homeScrollingEnabled: Boolean = false,
    val homeEffects: WallpaperEffects = WallpaperEffects(),
    val lockEffects: WallpaperEffects = WallpaperEffects(),
    val liveAlbumId: String? = null,
    val liveScalingType: ScalingType = ScalingType.FILL,
    val liveEffects: WallpaperEffects = WallpaperEffects(),
    val adaptiveBrightness: Boolean = false
) {
    val effectiveLockIntervalMinutes: Int
        get() = if (homeEnabled && lockEnabled && separateSchedules) lockIntervalMinutes else homeIntervalMinutes

    fun hasRequiredAlbums(mode: WallpaperMode): Boolean = when (mode) {
        WallpaperMode.LIVE -> liveAlbumId != null
        WallpaperMode.STATIC -> (homeEnabled || lockEnabled) &&
            (!homeEnabled || homeAlbumId != null) && (!lockEnabled || lockAlbumId != null)
    }

    fun activeScreens(mode: WallpaperMode): Set<ScreenType> {
        if (mode == WallpaperMode.LIVE) return if (liveAlbumId != null) setOf(ScreenType.LIVE) else emptySet()
        val home = homeEnabled && homeAlbumId != null
        val lock = lockEnabled && lockAlbumId != null
        if (home && lock && homeAlbumId == lockAlbumId && !separateSchedules) return setOf(ScreenType.BOTH)
        return buildSet {
            if (home) add(ScreenType.HOME)
            if (lock) add(ScreenType.LOCK)
        }
    }

    fun intervalMinutes(screen: ScreenType): Int = when (screen) {
        ScreenType.HOME, ScreenType.BOTH -> homeIntervalMinutes
        ScreenType.LOCK -> effectiveLockIntervalMinutes
        ScreenType.LIVE -> liveIntervalMinutes
    }

    fun validate(): ScheduleSettings = copy(
        homeIntervalMinutes = homeIntervalMinutes.coerceAtLeast(Constants.MIN_INTERVAL_MINUTES),
        lockIntervalMinutes = lockIntervalMinutes.coerceAtLeast(Constants.MIN_INTERVAL_MINUTES),
        liveIntervalMinutes = liveIntervalMinutes.coerceAtLeast(Constants.MIN_LIVE_INTERVAL_MINUTES),
        homeEffects = homeEffects.validate(),
        lockEffects = lockEffects.validate(),
        liveEffects = liveEffects.validate()
    )

    fun hasSchedulingChanges(other: ScheduleSettings): Boolean {
        return enableChanger != other.enableChanger ||
               homeAlbumId != other.homeAlbumId ||
               lockAlbumId != other.lockAlbumId ||
               liveAlbumId != other.liveAlbumId ||
               homeEnabled != other.homeEnabled ||
               lockEnabled != other.lockEnabled ||
               homeIntervalMinutes != other.homeIntervalMinutes ||
               lockIntervalMinutes != other.lockIntervalMinutes ||
               separateSchedules != other.separateSchedules ||
               liveIntervalMinutes != other.liveIntervalMinutes
    }

    /** Display changes reapply the current wallpaper without rescheduling periodic work. */
    fun hasDisplayChanges(other: ScheduleSettings): Boolean {
        return homeScalingType != other.homeScalingType ||
               lockScalingType != other.lockScalingType ||
               homeScrollingEnabled != other.homeScrollingEnabled ||
               homeEffects != other.homeEffects ||
               lockEffects != other.lockEffects ||
               liveEffects != other.liveEffects ||
               liveScalingType != other.liveScalingType ||
               adaptiveBrightness != other.adaptiveBrightness
    }

}

/**
 * WorkManager cannot run periodic jobs more often than every 15 minutes. Short live-wallpaper
 * intervals are therefore driven by the visible wallpaper engine and stop when it is hidden.
 */
fun usesVisibleLiveTimer(intervalMinutes: Int): Boolean =
    intervalMinutes in Constants.MIN_LIVE_INTERVAL_MINUTES until Constants.MIN_INTERVAL_MINUTES
