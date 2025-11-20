package com.anthonyla.paperize.data.repository

import com.anthonyla.paperize.data.datastore.PreferencesManager
import com.anthonyla.paperize.domain.model.AppSettings
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesManager: PreferencesManager
) : SettingsRepository {

    override suspend fun getAppSettings(): AppSettings =
        preferencesManager.getAppSettings()

    override fun getAppSettingsFlow(): Flow<AppSettings> =
        preferencesManager.getAppSettingsFlow()

    override suspend fun updateAppSettings(settings: AppSettings) =
        preferencesManager.updateAppSettings(settings)

    override suspend fun getScheduleSettings(): ScheduleSettings =
        preferencesManager.getScheduleSettings()

    override fun getScheduleSettingsFlow(): Flow<ScheduleSettings> =
        preferencesManager.getScheduleSettingsFlow()

    override suspend fun updateScheduleSettings(settings: ScheduleSettings) =
        preferencesManager.updateScheduleSettings(settings)

    override suspend fun clearAllSettings() =
        preferencesManager.clear()

    override suspend fun updateHomeAlbumId(albumId: String?) =
        preferencesManager.updateHomeAlbumId(albumId)

    override suspend fun updateLockAlbumId(albumId: String?) =
        preferencesManager.updateLockAlbumId(albumId)

    override suspend fun clearAlbumSelectionsIfMatches(albumId: String): Boolean =
        preferencesManager.clearAlbumSelectionsIfMatches(albumId)

    // ============ Atomic AppSettings Operations ============

    override suspend fun updateDarkMode(enabled: Boolean) =
        preferencesManager.updateDarkMode(enabled)

    override suspend fun updateAmoledTheme(enabled: Boolean) =
        preferencesManager.updateAmoledTheme(enabled)

    override suspend fun updateDynamicTheming(enabled: Boolean) =
        preferencesManager.updateDynamicTheming(enabled)

    override suspend fun updateAnimate(enabled: Boolean) =
        preferencesManager.updateAnimate(enabled)

    override suspend fun updateFirstLaunch(isFirstLaunch: Boolean) =
        preferencesManager.updateFirstLaunch(isFirstLaunch)
}
