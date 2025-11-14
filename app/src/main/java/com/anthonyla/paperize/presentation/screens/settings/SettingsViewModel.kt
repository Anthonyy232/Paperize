package com.anthonyla.paperize.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.domain.model.AppSettings
import com.anthonyla.paperize.domain.repository.SettingsRepository
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val appSettings: StateFlow<AppSettings> = settingsRepository.getAppSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings.default()
        )

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = appSettings.value
            settingsRepository.updateAppSettings(current.copy(darkMode = enabled))
        }
    }

    fun updateAmoledTheme(enabled: Boolean) {
        viewModelScope.launch {
            val current = appSettings.value
            settingsRepository.updateAppSettings(current.copy(amoledTheme = enabled))
        }
    }

    fun updateDynamicTheming(enabled: Boolean) {
        viewModelScope.launch {
            val current = appSettings.value
            settingsRepository.updateAppSettings(current.copy(dynamicTheming = enabled))
        }
    }

    fun updateAnimate(enabled: Boolean) {
        viewModelScope.launch {
            val current = appSettings.value
            settingsRepository.updateAppSettings(current.copy(animate = enabled))
        }
    }

    fun updateFirstLaunch(isFirstLaunch: Boolean) {
        viewModelScope.launch {
            val current = appSettings.value
            settingsRepository.updateAppSettings(current.copy(firstLaunch = isFirstLaunch))
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            settingsRepository.clearAllSettings()
        }
    }
}
