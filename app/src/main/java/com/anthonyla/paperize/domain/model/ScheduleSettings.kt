package com.anthonyla.paperize.domain.model

import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.constants.Constants

/**
 * Domain model for Wallpaper Schedule Settings
 */
data class ScheduleSettings(
    val enableChanger: Boolean = false,
    val separateSchedules: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val homeEnabled: Boolean = true,
    val lockEnabled: Boolean = true,
    val homeAlbumId: String? = null,
    val lockAlbumId: String? = null,
    val homeIntervalMinutes: Int = Constants.DEFAULT_INTERVAL_MINUTES,
    val lockIntervalMinutes: Int = Constants.DEFAULT_INTERVAL_MINUTES,
    val scheduleStartTime: String? = null,
    val useStartTime: Boolean = false,
    val homeNextChangeTime: Long? = null,
    val lockNextChangeTime: Long? = null,
    val skipLandscape: Boolean = false,
    val skipNonInteractive: Boolean = false,
    val homeScalingType: ScalingType = ScalingType.FILL,
    val lockScalingType: ScalingType = ScalingType.FILL,
    val homeEffects: WallpaperEffects = WallpaperEffects.none(),
    val lockEffects: WallpaperEffects = WallpaperEffects.none()
) {
    /**
     * Validate interval values
     */
    fun validate(): ScheduleSettings = copy(
        homeIntervalMinutes = homeIntervalMinutes.coerceAtLeast(Constants.MIN_INTERVAL_MINUTES),
        lockIntervalMinutes = lockIntervalMinutes.coerceAtLeast(Constants.MIN_INTERVAL_MINUTES),
        homeEffects = homeEffects.validate(),
        lockEffects = lockEffects.validate()
    )

    companion object {
        fun default() = ScheduleSettings()
    }
}
