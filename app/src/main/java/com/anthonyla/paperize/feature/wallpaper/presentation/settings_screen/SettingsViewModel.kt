package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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

    init {
        currentGetJob = viewModelScope.launch(Dispatchers.IO) {
            val darkMode = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DARK_MODE_TYPE) }
            val dynamicTheming = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DYNAMIC_THEME_TYPE) ?: false }
            val wallpaperInterval = async { settingsDataStoreImpl.getInt(SettingsConstants.WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT }
            val firstLaunch = async { settingsDataStoreImpl.getBoolean(SettingsConstants.FIRST_LAUNCH) ?: true }
            val setLockWithHome = async { settingsDataStoreImpl.getBoolean(SettingsConstants.SET_LOCK_WITH_HOME) ?: false }
            val lastSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.LAST_SET_TIME) }
            val nextSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.NEXT_SET_TIME) }
            val animate = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ANIMATE_TYPE) ?: true }
            val enableChanger = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: false }

            _state.update {
                it.copy(
                    darkMode = darkMode.await(),
                    dynamicTheming = dynamicTheming.await(),
                    interval = wallpaperInterval.await(),
                    setLockWithHome = setLockWithHome.await(),
                    firstLaunch = firstLaunch.await(),
                    lastSetTime = lastSetTime.await(),
                    nextSetTime = nextSetTime.await(),
                    animate = animate.await(),
                    enableChanger = enableChanger.await(),
                    isDataLoaded = true
                )
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetFirstLaunch -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.FIRST_LAUNCH, false)
                    _state.update {
                        it.copy(
                            firstLaunch = false
                        )
                    }
                }
            }

            is SettingsEvent.SetChangerToggle -> {
                viewModelScope.launch(Dispatchers.IO) {
                    when (event.toggle) {
                        true -> { settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_CHANGER, true) }
                        false -> { settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_CHANGER, false) }
                    }
                    _state.update {
                        it.copy(
                            enableChanger = event.toggle
                        )
                    }
                }
            }

            is SettingsEvent.Refresh -> {
                currentGetJob = viewModelScope.launch(Dispatchers.IO) {
                    val darkMode = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DARK_MODE_TYPE) }
                    val dynamicTheming = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DYNAMIC_THEME_TYPE) ?: false }
                    val wallpaperInterval = async { settingsDataStoreImpl.getInt(SettingsConstants.WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT }
                    val firstLaunch = async { settingsDataStoreImpl.getBoolean(SettingsConstants.FIRST_LAUNCH) ?: true }
                    val setLockWithHome = async { settingsDataStoreImpl.getBoolean(SettingsConstants.SET_LOCK_WITH_HOME) ?: false }
                    val lastSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.LAST_SET_TIME) }
                    val nextSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.NEXT_SET_TIME) }
                    val animate = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ANIMATE_TYPE) ?: true }
                    val enableChanger = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: false }

                    _state.update {
                        it.copy(
                            darkMode = darkMode.await(),
                            dynamicTheming = dynamicTheming.await(),
                            interval = wallpaperInterval.await(),
                            setLockWithHome = setLockWithHome.await(),
                            firstLaunch = firstLaunch.await(),
                            lastSetTime = lastSetTime.await(),
                            nextSetTime = nextSetTime.await(),
                            animate = animate.await(),
                            enableChanger = enableChanger.await()
                        )
                    }
                }
            }

            is SettingsEvent.SetDarkMode -> {
                viewModelScope.launch(Dispatchers.IO) {
                    when (event.darkMode) {
                        true -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DARK_MODE_TYPE, true) }
                        false -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DARK_MODE_TYPE, false) }
                        null -> { settingsDataStoreImpl.deleteBoolean(SettingsConstants.DARK_MODE_TYPE) }
                    }
                    _state.update {
                        it.copy(
                            darkMode = event.darkMode
                        )
                    }
                }
            }

            is SettingsEvent.SetDynamicTheming -> {
                viewModelScope.launch(Dispatchers.IO) {
                    when (event.dynamicTheming) {
                        true -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DYNAMIC_THEME_TYPE, true) }
                        false -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DYNAMIC_THEME_TYPE, false) }
                    }
                    _state.update {
                        it.copy(
                            dynamicTheming = event.dynamicTheming
                        )
                    }
                }
            }

            is SettingsEvent.SetAnimate -> {
                viewModelScope.launch(Dispatchers.IO) {
                    when (event.animate) {
                        true -> { settingsDataStoreImpl.putBoolean(SettingsConstants.ANIMATE_TYPE, true) }
                        false -> { settingsDataStoreImpl.putBoolean(SettingsConstants.ANIMATE_TYPE, false) }
                    }
                    _state.update {
                        it.copy(
                            animate = event.animate
                        )
                    }
                }
            }

            is SettingsEvent.SetWallpaperInterval -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    val time = LocalDateTime.now()
                    val formattedLastSetTime = time.format(formatter)
                    val formattedNextSetTime = time.plusMinutes(event.interval.toLong()).format(formatter)
                    settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, formattedLastSetTime)
                    settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, formattedNextSetTime)
                    settingsDataStoreImpl.putInt(SettingsConstants.WALLPAPER_CHANGE_INTERVAL, event.interval)
                    _state.update {
                        it.copy(
                            interval = event.interval,
                            lastSetTime = formattedLastSetTime,
                            nextSetTime = formattedNextSetTime
                        )
                    }
                }
            }

            is SettingsEvent.SetLockWithHome -> {
                viewModelScope.launch(Dispatchers.IO) {
                    when (event.lockWithHome) {
                        true -> { settingsDataStoreImpl.putBoolean(SettingsConstants.SET_LOCK_WITH_HOME, true) }
                        false -> { settingsDataStoreImpl.putBoolean(SettingsConstants.SET_LOCK_WITH_HOME, false) }
                    }
                    _state.update {
                        it.copy(
                            setLockWithHome = event.lockWithHome
                        )
                    }
                }
            }

            is SettingsEvent.Reset -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.DARK_MODE_TYPE)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.DYNAMIC_THEME_TYPE)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.FIRST_LAUNCH)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.SET_LOCK_WITH_HOME)
                    settingsDataStoreImpl.deleteString(SettingsConstants.LAST_SET_TIME)
                    settingsDataStoreImpl.deleteString(SettingsConstants.NEXT_SET_TIME)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.ANIMATE_TYPE)
                    settingsDataStoreImpl.deleteInt(SettingsConstants.WALLPAPER_CHANGE_INTERVAL)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.ENABLE_CHANGER)
                    _state.update {
                        it.copy(
                            darkMode = null,
                            dynamicTheming = false,
                            interval = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT,
                            setLockWithHome = false,
                            firstLaunch = true,
                            lastSetTime = null,
                            nextSetTime = null,
                            animate = true,
                            enableChanger = false
                        )
                    }
                }
            }
        }
    }
}
