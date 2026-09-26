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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
            settingsRepository.updateDarkMode(enabled)
        }
    }

    fun updateDynamicTheming(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDynamicTheming(enabled)
        }
    }

    fun updateAnimate(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAnimate(enabled)
        }
    }

    fun updateFirstLaunch(isFirstLaunch: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateFirstLaunch(isFirstLaunch)
        }
    }

    private val _isResetting = MutableStateFlow(false)
    val isResetting = _isResetting.asStateFlow()
    private val _resetFailed = MutableStateFlow(false)
    val resetFailed = _resetFailed.asStateFlow()

    fun switchWallpaperMode(newMode: WallpaperMode, onSuccess: () -> Unit = {}) =
        resetData(newMode, onSuccess)

    fun resetAllData() = resetData(mode = null)

    private fun resetData(mode: WallpaperMode?, onSuccess: () -> Unit = {}) {
        if (_isResetting.value) return
        _isResetting.value = true
        _resetFailed.value = false
        viewModelScope.launch {
            try {
                // Stop both the visible live timer and background jobs before deleting their source.
                settingsRepository.updateEnableChanger(false)
                wallpaperScheduler.cancelAllWallpaperChanges()
                albumRepository.deleteAllAlbums().getOrThrow()
                if (mode == null) {
                    settingsRepository.clearAllSettings()
                } else {
                    settingsRepository.clearScheduleSettings()
                    settingsRepository.updateWallpaperMode(mode)
                }
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Could not reset app data", e)
                _resetFailed.value = true
            } finally {
                _isResetting.value = false
            }
        }
    }
}
