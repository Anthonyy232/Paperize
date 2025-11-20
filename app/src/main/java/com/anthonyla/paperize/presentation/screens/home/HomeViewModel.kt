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
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService
import com.anthonyla.paperize.service.worker.WallpaperScheduler
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
    private val createAlbumUseCase: CreateAlbumUseCase,
    private val deleteAlbumUseCase: DeleteAlbumUseCase,
    private val albumRepository: AlbumRepository,
    private val settingsRepository: SettingsRepository,
    private val wallpaperScheduler: WallpaperScheduler,
    private val wallpaperRepository: com.anthonyla.paperize.domain.repository.WallpaperRepository
) : ViewModel() {

    val albums: StateFlow<List<Album>> = getAlbumsUseCase()
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
            when (createAlbumUseCase(name)) {
                is com.anthonyla.paperize.core.Result.Success -> { /* Success */ }
                is com.anthonyla.paperize.core.Result.Error -> { /* Handle error */ }
                is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
            }
        }
    }

    fun deleteAlbum(albumId: String) {
        viewModelScope.launch {
            // Atomically clear album selections if they match (prevents race conditions)
            val wasCleared = settingsRepository.clearAlbumSelectionsIfMatches(albumId)

            // Disable changer if albums were cleared and no albums remain selected
            if (wasCleared) {
                val settings = settingsRepository.getScheduleSettings()
                if (settings.homeAlbumId == null && settings.lockAlbumId == null) {
                    toggleWallpaperChanger(false)
                }
            }

            // Delete the album
            when (deleteAlbumUseCase(albumId)) {
                is com.anthonyla.paperize.core.Result.Success -> { /* Success */ }
                is com.anthonyla.paperize.core.Result.Error -> { /* Handle error */ }
                is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
            }
        }
    }

    fun selectHomeAlbum(album: Album?) {
        viewModelScope.launch {
            // Use atomic update to prevent race conditions with selectLockAlbum()
            settingsRepository.updateHomeAlbumId(album?.id)

            // Read updated settings after atomic write
            val updated = settingsRepository.getScheduleSettings()

            // If unselecting and no albums left, disable changer and cancel alarms
            if (album == null && updated.lockAlbumId == null) {
                toggleWallpaperChanger(false)
            } else if (album != null && updated.enableChanger) {
                // Check if we have all required albums before triggering wallpaper change
                val homeActive = updated.homeEnabled && updated.homeAlbumId != null
                val lockActive = updated.lockEnabled && updated.lockAlbumId != null
                val hasRequiredAlbums = when {
                    updated.homeEnabled && updated.lockEnabled -> homeActive && lockActive
                    updated.homeEnabled -> homeActive
                    updated.lockEnabled -> lockActive
                    else -> false
                }

                // Only change wallpaper and schedule if all required albums are selected
                if (hasRequiredAlbums) {
                    val screenType = if (
                        updated.homeAlbumId == updated.lockAlbumId &&
                        updated.homeAlbumId != null &&
                        !updated.separateSchedules &&
                        updated.homeEnabled &&
                        updated.lockEnabled
                    ) {
                        // Same album for both screens and not separately scheduled - use BOTH
                        ScreenType.BOTH
                    } else {
                        ScreenType.HOME
                    }
                    changeWallpaperNow(screenType)
                    scheduleAlarms(updated)
                } else {
                    // Not all required albums selected - cancel any existing schedules
                    wallpaperScheduler.cancelAllWallpaperChanges()
                }
            } else if (updated.enableChanger) {
                // Album was unselected - check if we still have all required albums
                val homeActive = updated.homeEnabled && updated.homeAlbumId != null
                val lockActive = updated.lockEnabled && updated.lockAlbumId != null
                val hasRequiredAlbums = when {
                    updated.homeEnabled && updated.lockEnabled -> homeActive && lockActive
                    updated.homeEnabled -> homeActive
                    updated.lockEnabled -> lockActive
                    else -> false
                }

                if (hasRequiredAlbums) {
                    scheduleAlarms(updated)
                } else {
                    wallpaperScheduler.cancelAllWallpaperChanges()
                }
            }
        }
    }

    fun selectLockAlbum(album: Album?) {
        viewModelScope.launch {
            // Use atomic update to prevent race conditions with selectHomeAlbum()
            settingsRepository.updateLockAlbumId(album?.id)

            // Read updated settings after atomic write
            val updated = settingsRepository.getScheduleSettings()

            // If unselecting and no albums left, disable changer and cancel alarms
            if (album == null && updated.homeAlbumId == null) {
                toggleWallpaperChanger(false)
            } else if (album != null && updated.enableChanger) {
                // Check if we have all required albums before triggering wallpaper change
                val homeActive = updated.homeEnabled && updated.homeAlbumId != null
                val lockActive = updated.lockEnabled && updated.lockAlbumId != null
                val hasRequiredAlbums = when {
                    updated.homeEnabled && updated.lockEnabled -> homeActive && lockActive
                    updated.homeEnabled -> homeActive
                    updated.lockEnabled -> lockActive
                    else -> false
                }

                // Only change wallpaper and schedule if all required albums are selected
                if (hasRequiredAlbums) {
                    val screenType = if (
                        updated.homeAlbumId == updated.lockAlbumId &&
                        updated.lockAlbumId != null &&
                        !updated.separateSchedules &&
                        updated.homeEnabled &&
                        updated.lockEnabled
                    ) {
                        // Same album for both screens and not separately scheduled - use BOTH
                        ScreenType.BOTH
                    } else {
                        ScreenType.LOCK
                    }
                    changeWallpaperNow(screenType)
                    scheduleAlarms(updated)
                } else {
                    // Not all required albums selected - cancel any existing schedules
                    wallpaperScheduler.cancelAllWallpaperChanges()
                }
            } else if (updated.enableChanger) {
                // Album was unselected - check if we still have all required albums
                val homeActive = updated.homeEnabled && updated.homeAlbumId != null
                val lockActive = updated.lockEnabled && updated.lockAlbumId != null
                val hasRequiredAlbums = when {
                    updated.homeEnabled && updated.lockEnabled -> homeActive && lockActive
                    updated.homeEnabled -> homeActive
                    updated.lockEnabled -> lockActive
                    else -> false
                }

                if (hasRequiredAlbums) {
                    scheduleAlarms(updated)
                } else {
                    wallpaperScheduler.cancelAllWallpaperChanges()
                }
            }
        }
    }

    fun toggleWallpaperChanger(enabled: Boolean, onlyIfNotScheduled: Boolean = false) {
        viewModelScope.launch {
            // Use atomic update to prevent race conditions with album selection updates
            settingsRepository.updateEnableChanger(enabled)

            // Read updated settings after atomic write
            val updated = settingsRepository.getScheduleSettings()

            if (enabled) {
                // Check if we have all required albums before changing wallpaper
                val homeActive = updated.homeEnabled && updated.homeAlbumId != null
                val lockActive = updated.lockEnabled && updated.lockAlbumId != null
                val hasRequiredAlbums = when {
                    updated.homeEnabled && updated.lockEnabled -> homeActive && lockActive
                    updated.homeEnabled -> homeActive
                    updated.lockEnabled -> lockActive
                    else -> false
                }

                // Only change wallpaper and schedule if all required albums are selected
                if (hasRequiredAlbums) {
                    val screenType = when {
                        homeActive && lockActive && updated.homeAlbumId == updated.lockAlbumId && !updated.separateSchedules ->
                            ScreenType.BOTH
                        homeActive && lockActive ->
                            ScreenType.BOTH // Change both even if different albums
                        homeActive -> ScreenType.HOME
                        lockActive -> ScreenType.LOCK
                        else -> null
                    }

                    // Only change wallpaper now if this is not a "check if scheduled" call
                    if (!onlyIfNotScheduled) {
                        screenType?.let { changeWallpaperNow(it) }
                    }
                    scheduleAlarms(updated, onlyIfNotScheduled)
                } else {
                    // Not all required albums selected - cancel any existing schedules
                    wallpaperScheduler.cancelAllWallpaperChanges()
                }
            } else {
                wallpaperScheduler.cancelAllWallpaperChanges()
            }
        }
    }

    fun updateScheduleSettings(settings: ScheduleSettings) {
        viewModelScope.launch {
            // Check if settings have changed before validation
            val currentSettings = settingsRepository.getScheduleSettings()
            val shuffleChanged = currentSettings.shuffleEnabled != settings.shuffleEnabled

            // Check if screen toggles (homeEnabled/lockEnabled) have changed
            val screenToggleChanged = currentSettings.homeEnabled != settings.homeEnabled ||
                                     currentSettings.lockEnabled != settings.lockEnabled

            // If screen toggles changed, clear all album selections
            val settingsWithClearedAlbums = if (screenToggleChanged) {
                settings.copy(
                    homeAlbumId = null,
                    lockAlbumId = null
                )
            } else {
                settings
            }

            val validated = settingsWithClearedAlbums.validate()

            // Check what changed
            val schedulingChanged = validated.hasSchedulingChanges(currentSettings)
            val displayChanged = validated.hasDisplayChanges(currentSettings)

            settingsRepository.updateScheduleSettings(validated)

            // If shuffle setting changed, clear all queues to force rebuild with new mode
            if (shuffleChanged) {
                wallpaperRepository.clearAllQueues()
            }

            val homeActive = validated.homeEnabled && validated.homeAlbumId != null
            val lockActive = validated.lockEnabled && validated.lockAlbumId != null

            // Determine if we have the required albums selected
            val hasRequiredAlbums = when {
                validated.homeEnabled && validated.lockEnabled -> homeActive && lockActive
                validated.homeEnabled -> homeActive
                validated.lockEnabled -> lockActive
                else -> false
            }

            // Handle scheduling changes (interval, screen enable/disable, etc.)
            if (validated.enableChanger && hasRequiredAlbums && schedulingChanged) {
                scheduleAlarms(validated)
            } else if (validated.enableChanger && !hasRequiredAlbums) {
                // Changer enabled but required albums not selected - cancel existing schedules
                wallpaperScheduler.cancelAllWallpaperChanges()
            }

            // Handle display changes (scaling, effects, adaptive brightness)
            // Reapply current wallpaper immediately to show the effect
            if (validated.enableChanger && hasRequiredAlbums && displayChanged) {
                val screenType = when {
                    homeActive && lockActive && validated.homeAlbumId == validated.lockAlbumId && !validated.separateSchedules ->
                        ScreenType.BOTH
                    homeActive && lockActive ->
                        ScreenType.BOTH // Change both even if different albums
                    homeActive -> ScreenType.HOME
                    lockActive -> ScreenType.LOCK
                    else -> null
                }
                screenType?.let { changeWallpaperNow(it) }
            }
        }
    }

    fun changeWallpaperNow(screenType: ScreenType) {
        val intent = Intent(context, WallpaperChangeService::class.java).apply {
            action = WallpaperChangeService.ACTION_CHANGE_WALLPAPER
            putExtra(WallpaperChangeService.EXTRA_SCREEN_TYPE, screenType.name)
        }
        context.startForegroundService(intent)
    }

    private fun scheduleAlarms(settings: ScheduleSettings, onlyIfNotScheduled: Boolean = false) {
        val homeActive = settings.homeEnabled && settings.homeAlbumId != null
        val lockActive = settings.lockEnabled && settings.lockAlbumId != null

        // Determine intervals based on active screens and schedule settings
        val homeInterval: Int
        val lockInterval: Int

        if (settings.homeEnabled && settings.lockEnabled) {
            // Both screens enabled - require both albums selected before scheduling
            if (homeActive && lockActive) {
                if (settings.separateSchedules) {
                    // Separate intervals for each screen
                    homeInterval = settings.homeIntervalMinutes
                    lockInterval = settings.lockIntervalMinutes
                } else {
                    // Same interval for both screens
                    homeInterval = settings.homeIntervalMinutes
                    lockInterval = settings.homeIntervalMinutes
                }
            } else {
                // Both enabled but not both selected - don't schedule anything
                homeInterval = 0
                lockInterval = 0
            }
        } else {
            // Only one screen enabled - schedule only if that screen has an album
            homeInterval = if (homeActive) settings.homeIntervalMinutes else 0
            lockInterval = if (lockActive) settings.lockIntervalMinutes else 0
        }

        // Determine if screens should be synchronized (same wallpaper)
        // This happens when: both enabled, same album, not separate schedules
        val shouldSync = settings.homeEnabled && settings.lockEnabled &&
                        settings.homeAlbumId != null &&
                        settings.homeAlbumId == settings.lockAlbumId &&
                        !settings.separateSchedules

        // Schedule using WorkManager (handles cancellation automatically)
        wallpaperScheduler.scheduleWallpaperChanges(
            homeIntervalMinutes = homeInterval,
            lockIntervalMinutes = lockInterval,
            synchronized = shouldSync,
            onlyIfNotScheduled = onlyIfNotScheduled
        )
    }
}
