package com.anthonyla.paperize.feature.wallpaper.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor (
    private val settingsDataStoreImpl: SettingsDataStore
): ViewModel() {
    var shouldNotBypassSplashScreen by mutableStateOf(true)
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000), SettingsState()
    )

    private var currentGetJob: Job? = null

    init { viewModelScope.launch { refreshSettings() } }

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
            _state.update { it.copy(
                darkMode = settingsDataStoreImpl.getBoolean(SettingsConstants.DARK_MODE_TYPE),
                dynamicTheming = when(settingsDataStoreImpl.getBoolean(SettingsConstants.DYNAMIC_THEME_TYPE)) {
                    true -> true
                    false, null -> false
                }
            ) }
            shouldNotBypassSplashScreen = false
        }
    }
}
