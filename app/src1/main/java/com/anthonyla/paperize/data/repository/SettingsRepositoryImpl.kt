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
}
