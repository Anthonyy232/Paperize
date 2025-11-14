package com.anthonyla.paperize.presentation.screens.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.usecase.CreateAlbumUseCase
import com.anthonyla.paperize.domain.usecase.DeleteAlbumUseCase
import com.anthonyla.paperize.domain.usecase.GetAlbumsUseCase
import com.anthonyla.paperize.domain.usecase.GetSelectedAlbumsUseCase
import com.anthonyla.paperize.service.alarm.WallpaperAlarmScheduler
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    getAlbumsUseCase: GetAlbumsUseCase,
    getSelectedAlbumsUseCase: GetSelectedAlbumsUseCase,
    private val createAlbumUseCase: CreateAlbumUseCase,
    private val deleteAlbumUseCase: DeleteAlbumUseCase,
    private val albumRepository: AlbumRepository,
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

    val appSettings = settingsRepository.getAppSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.anthonyla.paperize.domain.model.AppSettings.default()
        )

    fun createAlbum(name: String) {
        viewModelScope.launch {
            createAlbumUseCase(name)
        }
    }

    fun deleteAlbum(albumId: String) {
        viewModelScope.launch {
            deleteAlbumUseCase(albumId)
        }
    }

    fun selectHomeAlbum(album: Album) {
        viewModelScope.launch {
            val current = scheduleSettings.value
            val updated = current.copy(homeAlbumId = album.id)
            settingsRepository.updateScheduleSettings(updated)
        }
    }

    fun selectLockAlbum(album: Album) {
        viewModelScope.launch {
            val current = scheduleSettings.value
            val updated = current.copy(lockAlbumId = album.id)
            settingsRepository.updateScheduleSettings(updated)
        }
    }

    fun toggleAlbumSelection(album: Album) {
        viewModelScope.launch {
            val isCurrentlySelected = selectedAlbums.value.any { it.id == album.id }
            albumRepository.updateAlbum(album.copy(isSelected = !isCurrentlySelected))
        }
    }

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

    fun changeWallpaperNow(screenType: ScreenType) {
        val intent = Intent(context, WallpaperChangeService::class.java).apply {
            action = when (screenType) {
                ScreenType.HOME -> "ACTION_CHANGE_HOME"
                ScreenType.LOCK -> "ACTION_CHANGE_LOCK"
                ScreenType.BOTH -> "ACTION_CHANGE_BOTH"
            }
        }
        context.startForegroundService(intent)
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
