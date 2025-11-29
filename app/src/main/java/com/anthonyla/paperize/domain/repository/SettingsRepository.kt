package com.anthonyla.paperize.domain.repository

import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.domain.model.AppSettings
import com.anthonyla.paperize.domain.model.ScheduleSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Settings operations
 */
interface SettingsRepository {
    /**
     * Get app settings
     */
    suspend fun getAppSettings(): AppSettings

    /**
     * Get app settings as Flow
     */
    fun getAppSettingsFlow(): Flow<AppSettings>

    /**
     * Update app settings
     */
    suspend fun updateAppSettings(settings: AppSettings)

    /**
     * Get wallpaper mode (STATIC or LIVE)
     */
    suspend fun getWallpaperMode(): WallpaperMode

    /**
     * Get wallpaper mode as Flow
     */
    fun getWallpaperModeFlow(): Flow<WallpaperMode>

    /**
     * Update wallpaper mode
     * IMPORTANT: Caller must handle data reset when switching modes
     */
    suspend fun updateWallpaperMode(mode: WallpaperMode)

    /**
     * Get schedule settings
     */
    suspend fun getScheduleSettings(): ScheduleSettings

    /**
     * Get schedule settings as Flow
     */
    fun getScheduleSettingsFlow(): Flow<ScheduleSettings>

    /**
     * Update schedule settings
     */
    suspend fun updateScheduleSettings(settings: ScheduleSettings)

    /**
     * Clear all settings
     */
    suspend fun clearAllSettings()

    /**
     * Clear schedule settings (reset to defaults)
     * Used when switching wallpaper modes to reset all scheduling configuration
     */
    suspend fun clearScheduleSettings()

    /**
     * Set wallpaper mode (wrapper for updateWallpaperMode)
     */
    suspend fun setWallpaperMode(mode: WallpaperMode) = updateWallpaperMode(mode)

    /**
     * Atomically update home album ID to prevent race conditions
     * Use this instead of updateScheduleSettings when only changing home album
     */
    suspend fun updateHomeAlbumId(albumId: String?)

    /**
     * Atomically update lock album ID to prevent race conditions
     * Use this instead of updateScheduleSettings when only changing lock album
     */
    suspend fun updateLockAlbumId(albumId: String?)

    /**
     * Atomically update live album ID to prevent race conditions
     * Use this instead of updateScheduleSettings when only changing live album
     */
    suspend fun updateLiveAlbumId(albumId: String?)

    /**
     * Atomically clear album selections if they match the given album ID
     * Used when deleting an album to prevent race conditions
     * Returns true if any selections were cleared
     */
    suspend fun clearAlbumSelectionsIfMatches(albumId: String): Boolean

    // ============ Atomic AppSettings Operations ============

    suspend fun updateDarkMode(enabled: Boolean)
    suspend fun updateDynamicTheming(enabled: Boolean)
    suspend fun updateAnimate(enabled: Boolean)
    suspend fun updateFirstLaunch(isFirstLaunch: Boolean)

    // ============ Atomic ScheduleSettings Operations ============

    /**
     * Atomically update enableChanger to prevent race conditions
     * Use this instead of updateScheduleSettings when only toggling changer
     */
    suspend fun updateEnableChanger(enabled: Boolean)
}
