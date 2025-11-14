package com.anthonyla.paperize.domain.repository

import com.anthonyla.paperize.domain.model.AppSettings
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.WallpaperEffects
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
     * Get wallpaper effects settings
     */
    suspend fun getWallpaperEffects(): WallpaperEffects

    /**
     * Get wallpaper effects as Flow
     */
    fun getWallpaperEffectsFlow(): Flow<WallpaperEffects>

    /**
     * Update wallpaper effects
     */
    suspend fun updateWallpaperEffects(effects: WallpaperEffects)

    /**
     * Clear all settings
     */
    suspend fun clearAllSettings()
}
