package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.data.settings.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor (
    private val settingsDataStoreImpl: SettingsDataStore
): ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000), SettingsState()
    )

    private var currentGetJob: Job? = null
    var setKeepOnScreenCondition: Boolean = true

    init {
        currentGetJob = viewModelScope.launch {
            val darkMode = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DARK_MODE_TYPE) }
            val dynamicTheming = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DYNAMIC_THEME_TYPE) ?: false }
            val wallpaperInterval = async { settingsDataStoreImpl.getInt(SettingsConstants.WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT }
            val firstLaunch = async { settingsDataStoreImpl.getBoolean(SettingsConstants.FIRST_LAUNCH) ?: true }
            val setLockWithHome = async { settingsDataStoreImpl.getBoolean(SettingsConstants.SET_LOCK_WITH_HOME) ?: false }

            _state.update { it.copy(
                darkMode = darkMode.await(),
                dynamicTheming = dynamicTheming.await(),
                interval = wallpaperInterval.await(),
                setLockWithHome = setLockWithHome.await(),
                firstLaunch = firstLaunch.await()
            ) }
            setKeepOnScreenCondition = false
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetFirstLaunch -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.FIRST_LAUNCH, false)
                    _state.update { it.copy(
                        firstLaunch = false
                    ) }
                }
            }
            is SettingsEvent.RefreshUiState -> {
                viewModelScope.launch {
                    currentGetJob?.cancel()
                    currentGetJob = viewModelScope.launch {
                        _state.update { it.copy(
                            darkMode = settingsDataStoreImpl.getBoolean(SettingsConstants.DARK_MODE_TYPE),
                            dynamicTheming = when(settingsDataStoreImpl.getBoolean(SettingsConstants.DYNAMIC_THEME_TYPE)) {
                                true -> true
                                false, null -> false
                            }
                        ) }
                    }
                }
            }
            is SettingsEvent.SetDarkMode -> {
                viewModelScope.launch {
                    when (event.darkMode) {
                        true -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DARK_MODE_TYPE, true) }
                        false -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DARK_MODE_TYPE, false) }
                        null -> { settingsDataStoreImpl.deleteBoolean(SettingsConstants.DARK_MODE_TYPE) }
                    }
                    _state.update { it.copy(
                        darkMode = event.darkMode
                    ) }
                }
            }
            is SettingsEvent.SetDynamicTheming -> {
                viewModelScope.launch {
                    when (event.dynamicTheming) {
                        true -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DYNAMIC_THEME_TYPE, true) }
                        false -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DYNAMIC_THEME_TYPE, false) }
                    }
                    _state.update { it.copy(
                        dynamicTheming = event.dynamicTheming
                    ) }
                }
            }
            is SettingsEvent.SetWallpaperInterval -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putInt(SettingsConstants.WALLPAPER_CHANGE_INTERVAL, event.interval)
                    _state.update { it.copy(
                        interval = event.interval
                    ) }
                }
            }
            is SettingsEvent.SetLockWithHome -> {
                viewModelScope.launch {
                    when (event.lockWithHome) {
                        true -> { settingsDataStoreImpl.putBoolean(SettingsConstants.SET_LOCK_WITH_HOME, true) }
                        false -> { settingsDataStoreImpl.putBoolean(SettingsConstants.SET_LOCK_WITH_HOME, false) }
                    }
                    _state.update { it.copy(
                        setLockWithHome = event.lockWithHome
                    ) }
                }
            }
            is SettingsEvent.RefreshWallpaperState -> {
                viewModelScope.launch {
                    currentGetJob?.cancel()
                    currentGetJob = viewModelScope.launch {
                        _state.update { it.copy(
                            interval = settingsDataStoreImpl.getInt(SettingsConstants.WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT,
                            setLockWithHome = settingsDataStoreImpl.getBoolean(SettingsConstants.SET_LOCK_WITH_HOME) ?: false
                        ) }
                    }
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
                },
                interval = settingsDataStoreImpl.getInt(SettingsConstants.WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
            ) }
        }
    }
}
