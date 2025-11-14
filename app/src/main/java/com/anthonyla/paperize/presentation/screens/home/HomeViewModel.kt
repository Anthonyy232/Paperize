package com.anthonyla.paperize.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.usecase.GetAlbumsUseCase
import com.anthonyla.paperize.domain.usecase.GetSelectedAlbumsUseCase
import com.anthonyla.paperize.service.alarm.WallpaperAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    getAlbumsUseCase: GetAlbumsUseCase,
    getSelectedAlbumsUseCase: GetSelectedAlbumsUseCase,
    private val settingsRepository: SettingsRepository,
    private val wallpaperAlarmScheduler: WallpaperAlarmScheduler
) : ViewModel() {

    val albums: StateFlow<List<Album>> = getAlbumsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedAlbums: StateFlow<List<Album>> = getSelectedAlbumsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val scheduleSettings: StateFlow<ScheduleSettings> = settingsRepository.getScheduleSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScheduleSettings.default()
        )

    fun toggleWallpaperChanger(enabled: Boolean) {
        viewModelScope.launch {
            val current = scheduleSettings.value
            val updated = current.copy(enableChanger = enabled)
            settingsRepository.updateScheduleSettings(updated)

            if (enabled) {
                scheduleAlarms(updated)
            } else {
                wallpaperAlarmScheduler.cancelAllAlarms()
            }
        }
    }

    fun updateScheduleSettings(settings: ScheduleSettings) {
        viewModelScope.launch {
            val validated = settings.validate()
            settingsRepository.updateScheduleSettings(validated)

            if (validated.enableChanger) {
                scheduleAlarms(validated)
            }
        }
    }

    private fun scheduleAlarms(settings: ScheduleSettings) {
        if (settings.separateSchedules) {
            wallpaperAlarmScheduler.scheduleWallpaperChange(
                ScreenType.HOME,
                settings.homeIntervalMinutes,
                if (settings.useStartTime) settings.scheduleStartTime else null
            )
            wallpaperAlarmScheduler.scheduleWallpaperChange(
                ScreenType.LOCK,
                settings.lockIntervalMinutes,
                if (settings.useStartTime) settings.scheduleStartTime else null
            )
        } else {
            wallpaperAlarmScheduler.scheduleWallpaperChange(
                ScreenType.BOTH,
                settings.homeIntervalMinutes,
                if (settings.useStartTime) settings.scheduleStartTime else null
            )
        }

        // Schedule daily refresh
        wallpaperAlarmScheduler.scheduleRefreshAlarm()
    }
}
