package com.anthonyla.paperize.feature.wallpaper.presentation.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.data.settings.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor (
    private val settingsDataStoreImpl: SettingsDataStore
): ViewModel() {
    var shouldNotBypassSplashScreen by mutableStateOf(true)
    private val _state = mutableStateOf(SettingsState(null, false))
    val state: State<SettingsState> = _state
    private var currentGetJob: Job? = null

    init {
        viewModelScope.launch {
            refreshSettings()
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetDarkMode -> {
                viewModelScope.launch {
                    when (event.darkMode) {
                        true -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DARK_MODE_TYPE, true) }
                        false -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DARK_MODE_TYPE, false) }
                        null -> { settingsDataStoreImpl.deleteBoolean(SettingsConstants.DARK_MODE_TYPE) }
                    }
                    refreshSettings()
                }
            }
            is SettingsEvent.SetDynamicTheming -> {
                viewModelScope.launch {
                    when (event.dynamicTheming) {
                        true -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DYNAMIC_THEME_TYPE, true) }
                        false -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DYNAMIC_THEME_TYPE, false) }
                    }
                    refreshSettings()
                }
            }
        }
    }

    private fun refreshSettings() {
        currentGetJob?.cancel()
        currentGetJob = viewModelScope.launch {
            _state.value = state.value.copy(
                darkMode = settingsDataStoreImpl.getBoolean(SettingsConstants.DARK_MODE_TYPE),
                dynamicTheming = when(settingsDataStoreImpl.getBoolean(SettingsConstants.DYNAMIC_THEME_TYPE)) {
                    true -> true
                    false, null -> false
                }
            )
            shouldNotBypassSplashScreen = false
        }
    }
}
