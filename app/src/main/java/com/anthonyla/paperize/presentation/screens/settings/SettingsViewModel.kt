package com.anthonyla.paperize.presentation.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.runCatchingCancellable
import com.anthonyla.paperize.domain.model.AppSettings
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.presentation.common.launchBusy
import com.anthonyla.paperize.presentation.common.stateWhileSubscribed
import com.anthonyla.paperize.service.SettingsCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val coordinator: SettingsCoordinator
) : ViewModel() {

    // Eager so the first frame already knows whether to show onboarding.
    val appSettings: StateFlow<AppSettings?> =
        settingsRepository.getAppSettingsFlow().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val wallpaperMode: StateFlow<WallpaperMode> =
        settingsRepository.getWallpaperModeFlow().stateWhileSubscribed(viewModelScope, WallpaperMode.STATIC)

    private val _isResetting = MutableStateFlow(false)
    val isResetting = _isResetting.asStateFlow()
    private val _resetFailed = MutableStateFlow(false)
    val resetFailed = _resetFailed.asStateFlow()

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDarkMode(enabled) }
    }

    fun updateDynamicTheming(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDynamicTheming(enabled) }
    }

    fun updateAnimate(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAnimate(enabled) }
    }

    fun completeFirstLaunch() {
        viewModelScope.launch { settingsRepository.completeFirstLaunch() }
    }

    fun switchWallpaperMode(newMode: WallpaperMode, onSuccess: () -> Unit = {}) = resetData(newMode, onSuccess)

    fun resetAllData() = resetData(mode = null)

    private fun resetData(mode: WallpaperMode?, onSuccess: () -> Unit = {}) {
        if (_isResetting.value) return
        _resetFailed.value = false
        launchBusy(_isResetting) {
            runCatchingCancellable {
                coordinator.resetData(mode)
                onSuccess()
            }.onFailure { e ->
                Log.e(TAG, "Could not reset app data", e)
                _resetFailed.value = true
            }
        }
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
