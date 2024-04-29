package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.data.settings.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
            val lastSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.LAST_SET_TIME) }
            val nextSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.NEXT_SET_TIME) }
            val animate = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ANIMATE_TYPE) ?: true }

            _state.update { it.copy(
                darkMode = darkMode.await(),
                dynamicTheming = dynamicTheming.await(),
                interval = wallpaperInterval.await(),
                setLockWithHome = setLockWithHome.await(),
                firstLaunch = firstLaunch.await(),
                lastSetTime = lastSetTime.await(),
                nextSetTime = nextSetTime.await(),
                animate = animate.await()
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
            is SettingsEvent.Refresh -> {
                currentGetJob = viewModelScope.launch {
                    val darkMode = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DARK_MODE_TYPE) }
                    val dynamicTheming = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DYNAMIC_THEME_TYPE) ?: false }
                    val wallpaperInterval = async { settingsDataStoreImpl.getInt(SettingsConstants.WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT }
                    val firstLaunch = async { settingsDataStoreImpl.getBoolean(SettingsConstants.FIRST_LAUNCH) ?: true }
                    val setLockWithHome = async { settingsDataStoreImpl.getBoolean(SettingsConstants.SET_LOCK_WITH_HOME) ?: false }
                    val lastSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.LAST_SET_TIME) }
                    val nextSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.NEXT_SET_TIME) }
                    val animate = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ANIMATE_TYPE) ?: true }

                    _state.update { it.copy(
                        darkMode = darkMode.await(),
                        dynamicTheming = dynamicTheming.await(),
                        interval = wallpaperInterval.await(),
                        setLockWithHome = setLockWithHome.await(),
                        firstLaunch = firstLaunch.await(),
                        lastSetTime = lastSetTime.await(),
                        nextSetTime = nextSetTime.await(),
                        animate = animate.await()
                    ) }
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
            is SettingsEvent.SetAnimate -> {
                viewModelScope.launch {
                    when (event.animate) {
                        true -> { settingsDataStoreImpl.putBoolean(SettingsConstants.ANIMATE_TYPE, true) }
                        false -> { settingsDataStoreImpl.putBoolean(SettingsConstants.ANIMATE_TYPE, false) }
                    }
                    _state.update { it.copy(
                        animate = event.animate
                    ) }
                }
            }
            is SettingsEvent.SetWallpaperInterval -> {
                viewModelScope.launch {
                    val formatter = DateTimeFormatter.ofPattern("MM/dd/yy hh:mm\na")
                    val time = LocalDateTime.now()
                    val formattedLastSetTime = time.format(formatter)
                    val formattedNextSetTime = time.plusMinutes(event.interval.toLong()).format(formatter)
                    settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, formattedLastSetTime)
                    settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, formattedNextSetTime)
                    settingsDataStoreImpl.putInt(SettingsConstants.WALLPAPER_CHANGE_INTERVAL, event.interval)
                    _state.update { it.copy(
                        interval = event.interval,
                        lastSetTime = formattedLastSetTime,
                        nextSetTime = formattedNextSetTime
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
        }
    }
}
