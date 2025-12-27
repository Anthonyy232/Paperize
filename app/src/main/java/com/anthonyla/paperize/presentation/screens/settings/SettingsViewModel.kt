package com.anthonyla.paperize.presentation.screens.settings
import com.anthonyla.paperize.core.constants.Constants

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.domain.model.AppSettings
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.service.worker.WallpaperScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val albumRepository: AlbumRepository,
    private val wallpaperScheduler: WallpaperScheduler
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    val appSettings: StateFlow<AppSettings?> = settingsRepository.getAppSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,  // Start loading immediately to prevent onboarding flicker
            initialValue = null
        )

    val wallpaperMode: StateFlow<WallpaperMode> = settingsRepository.getWallpaperModeFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
            initialValue = WallpaperMode.STATIC
        )

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            // Use atomic update to prevent race conditions
            settingsRepository.updateDarkMode(enabled)
        }
    }

    fun updateDynamicTheming(enabled: Boolean) {
        viewModelScope.launch {
            // Use atomic update to prevent race conditions
            settingsRepository.updateDynamicTheming(enabled)
        }
    }

    fun updateAnimate(enabled: Boolean) {
        viewModelScope.launch {
            // Use atomic update to prevent race conditions
            settingsRepository.updateAnimate(enabled)
        }
    }

    fun updateFirstLaunch(isFirstLaunch: Boolean) {
        viewModelScope.launch {
            // Use atomic update to prevent race conditions
            settingsRepository.updateFirstLaunch(isFirstLaunch)
        }
    }

    /**
     * Switch wallpaper mode and reset all data
     * This is required because STATIC and LIVE modes have different capabilities
     * and incompatible wallpaper types
     */
    fun switchWallpaperMode(newMode: WallpaperMode) {
        viewModelScope.launch {
            // Cancel all scheduled wallpaper changes first
            wallpaperScheduler.cancelAllWallpaperChanges()

            // Delete all albums (cascades to delete all wallpapers, folders, and queues)
            when (val result = albumRepository.deleteAllAlbums()) {
                is com.anthonyla.paperize.core.Result.Success -> { /* Success */ }
                is com.anthonyla.paperize.core.Result.Error -> { 
                    Log.e(TAG, "Error deleting albums during mode switch", result.exception)
                }
                is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
            }

            // Reset schedule settings to default (clears effects, intervals, etc.)
            settingsRepository.clearScheduleSettings()

            // Set new wallpaper mode
            settingsRepository.setWallpaperMode(newMode)
        }
    }

    /**
     * Reset all app data - settings, albums, wallpapers, folders, queues, and alarms
     * This completely resets the app to initial state as if just installed
     */
    fun resetAllData() {
        viewModelScope.launch {
            // Cancel all scheduled wallpaper changes first
            wallpaperScheduler.cancelAllWallpaperChanges()

            // Delete all albums (cascades to delete all wallpapers, folders, and queues)
            when (val result = albumRepository.deleteAllAlbums()) {
                is com.anthonyla.paperize.core.Result.Success -> { /* Success */ }
                is com.anthonyla.paperize.core.Result.Error -> { 
                    Log.e(TAG, "Error deleting albums during reset", result.exception)
                }
                is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
            }

            // Clear all settings (DataStore) - resets to default values
            // This includes setting firstLaunch back to true for onboarding
            settingsRepository.clearAllSettings()
        }
    }
}
