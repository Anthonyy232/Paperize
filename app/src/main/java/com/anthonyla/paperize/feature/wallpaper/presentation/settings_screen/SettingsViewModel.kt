package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.ScalingConstants
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
    var setKeepOnScreenCondition: Boolean = true

    init {
        currentGetJob = viewModelScope.launch(Dispatchers.IO) {
            val firstLaunch = async { settingsDataStoreImpl.getBoolean(SettingsConstants.FIRST_LAUNCH) ?: true }
            val darkMode = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DARK_MODE_TYPE) }
            val amoledTheme = async { settingsDataStoreImpl.getBoolean(SettingsConstants.AMOLED_THEME_TYPE) ?: false }
            val dynamicTheming = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DYNAMIC_THEME_TYPE) ?: false }
            val enableChanger = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: true }
            val setHomeWallpaper = async { settingsDataStoreImpl.getBoolean(SettingsConstants.HOME_WALLPAPER) ?: false }
            val setLockWallpaper = async { settingsDataStoreImpl.getBoolean(SettingsConstants.LOCK_WALLPAPER) ?: false }
            val homeWallpaperInterval = async { settingsDataStoreImpl.getInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT }
            val lockWallpaperInterval = async { settingsDataStoreImpl.getInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT }
            val lastSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.LAST_SET_TIME) }
            val nextSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.NEXT_SET_TIME) }
            val animate = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ANIMATE_TYPE) ?: true }
            val darkenPercentage = async { settingsDataStoreImpl.getInt(SettingsConstants.DARKEN_PERCENTAGE) ?: 100 }
            val darken = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DARKEN) ?: false }
            val wallpaperScaling = async { ScalingConstants.valueOf(settingsDataStoreImpl.getString(SettingsConstants.WALLPAPER_SCALING) ?: ScalingConstants.FILL.name) }
            val scheduleSeparately = async { settingsDataStoreImpl.getBoolean(SettingsConstants.SCHEDULE_SEPARATELY) ?: false }
            val blur = async { settingsDataStoreImpl.getBoolean(SettingsConstants.BLUR) ?: false }
            val blurPercentage = async { settingsDataStoreImpl.getInt(SettingsConstants.BLUR_PERCENTAGE) ?: 0 }

            _state.update {
                it.copy(
                    darkMode = darkMode.await(),
                    amoledTheme = amoledTheme.await(),
                    dynamicTheming = dynamicTheming.await(),
                    homeInterval = homeWallpaperInterval.await(),
                    lockInterval = lockWallpaperInterval.await(),
                    firstLaunch = firstLaunch.await(),
                    lastSetTime = lastSetTime.await(),
                    nextSetTime = nextSetTime.await(),
                    animate = animate.await(),
                    enableChanger = enableChanger.await(),
                    darkenPercentage = darkenPercentage.await(),
                    darken = darken.await(),
                    wallpaperScaling = wallpaperScaling.await(),
                    setHomeWallpaper = setHomeWallpaper.await(),
                    setLockWallpaper = setLockWallpaper.await(),
                    scheduleSeparately = scheduleSeparately.await(),
                    blur = blur.await(),
                    blurPercentage = blurPercentage.await()
                )
            }
            setKeepOnScreenCondition = false
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
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_CHANGER, event.toggle)
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
                    val amoledTheme = async { settingsDataStoreImpl.getBoolean(SettingsConstants.AMOLED_THEME_TYPE) ?: false }
                    val dynamicTheming = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DYNAMIC_THEME_TYPE) ?: false }
                    val firstLaunch = async { settingsDataStoreImpl.getBoolean(SettingsConstants.FIRST_LAUNCH) ?: true }
                    val lastSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.LAST_SET_TIME) }
                    val nextSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.NEXT_SET_TIME) }
                    val animate = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ANIMATE_TYPE) ?: true }
                    val enableChanger = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: true }
                    val darkenPercentage = async { settingsDataStoreImpl.getInt(SettingsConstants.DARKEN_PERCENTAGE) ?: 0 }
                    val darken = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DARKEN) ?: false }
                    val setHomeWallpaper = async { settingsDataStoreImpl.getBoolean(SettingsConstants.HOME_WALLPAPER) ?: false }
                    val setLockWallpaper = async { settingsDataStoreImpl.getBoolean(SettingsConstants.LOCK_WALLPAPER) ?: false }
                    val homeWallpaperInterval = async { settingsDataStoreImpl.getInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT }
                    val lockWallpaperInterval = async { settingsDataStoreImpl.getInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT }
                    val scheduleSeparately = async { settingsDataStoreImpl.getBoolean(SettingsConstants.SCHEDULE_SEPARATELY) ?: false }
                    val blur = async { settingsDataStoreImpl.getBoolean(SettingsConstants.BLUR) ?: false }
                    val blurPercentage = async { settingsDataStoreImpl.getInt(SettingsConstants.BLUR_PERCENTAGE) ?: 0 }
                    _state.update {
                        it.copy(
                            darkMode = darkMode.await(),
                            amoledTheme = amoledTheme.await(),
                            dynamicTheming = dynamicTheming.await(),
                            firstLaunch = firstLaunch.await(),
                            lastSetTime = lastSetTime.await(),
                            nextSetTime = nextSetTime.await(),
                            animate = animate.await(),
                            enableChanger = enableChanger.await(),
                            darkenPercentage = darkenPercentage.await(),
                            darken = darken.await(),
                            setHomeWallpaper = setHomeWallpaper.await(),
                            setLockWallpaper = setLockWallpaper.await(),
                            lockInterval = lockWallpaperInterval.await(),
                            homeInterval = homeWallpaperInterval.await(),
                            scheduleSeparately = scheduleSeparately.await(),
                            blur = blur.await(),
                            blurPercentage = blurPercentage.await(),
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

            is SettingsEvent.SetAmoledTheme -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.AMOLED_THEME_TYPE, event.amoledTheme)
                    if (event.amoledTheme) {
                        settingsDataStoreImpl.putBoolean(SettingsConstants.DYNAMIC_THEME_TYPE, false)
                    }
                    _state.update {
                        it.copy(
                            amoledTheme = event.amoledTheme,
                            dynamicTheming = if (event.amoledTheme) false else it.dynamicTheming
                        )
                    }
                }
            }

            is SettingsEvent.SetDynamicTheming -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.DYNAMIC_THEME_TYPE, event.dynamicTheming)
                    if (event.dynamicTheming) {
                        settingsDataStoreImpl.putBoolean(SettingsConstants.AMOLED_THEME_TYPE, false)
                    }
                    _state.update {
                        it.copy(
                            amoledTheme = if (event.dynamicTheming) false else it.amoledTheme,
                            dynamicTheming = event.dynamicTheming
                        )
                    }
                }
            }

            is SettingsEvent.SetAnimate -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ANIMATE_TYPE, event.animate)
                    _state.update {
                        it.copy(
                            animate = event.animate
                        )
                    }
                }
            }

            is SettingsEvent.SetHomeWallpaperInterval -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL, event.interval)
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    val currentTime = LocalDateTime.now()
                    val nextSetTime: String?
                    settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, currentTime.format(formatter))
                    if (_state.value.scheduleSeparately) {
                        val nextSetTime1 = currentTime.plusMinutes(event.interval.toLong())
                        val nextSetTime2 = currentTime.plusMinutes(_state.value.lockInterval.toLong())
                        nextSetTime = (if (nextSetTime1!!.isBefore(nextSetTime2)) nextSetTime1 else nextSetTime2)!!.format(formatter)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME_1, nextSetTime1.toString())
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME_2, nextSetTime2.toString())
                    }
                    else {
                        nextSetTime = currentTime.plusMinutes(event.interval.toLong()).format(formatter)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME_1, currentTime.plusMinutes(event.interval.toLong()).toString())
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME_2, currentTime.plusMinutes(event.interval.toLong()).toString())
                    }
                    _state.update {
                        it.copy(
                            homeInterval = event.interval,
                            lastSetTime = currentTime.format(formatter),
                            nextSetTime = nextSetTime
                        )
                    }
                }
            }

            is SettingsEvent.SetLockWallpaperInterval -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL, event.interval)
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    val currentTime = LocalDateTime.now()
                    val nextSetTime: String?
                    settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, currentTime.format(formatter))
                    if (_state.value.scheduleSeparately) {
                        val nextSetTime1 = currentTime.plusMinutes(_state.value.homeInterval.toLong())
                        val nextSetTime2 = currentTime.plusMinutes(event.interval.toLong())
                        nextSetTime = (if (nextSetTime1!!.isBefore(nextSetTime2)) nextSetTime1 else nextSetTime2)!!.format(formatter)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME_1, nextSetTime1.toString())
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME_2, nextSetTime2.toString())
                    }
                    else {
                        nextSetTime = currentTime.plusMinutes(event.interval.toLong()).format(formatter)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME_1, currentTime.plusMinutes(event.interval.toLong()).toString())
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME_2, currentTime.plusMinutes(event.interval.toLong()).toString())
                    }
                    _state.update {
                        it.copy(
                            lockInterval = event.interval,
                            lastSetTime = currentTime.format(formatter),
                            nextSetTime = nextSetTime
                        )
                    }
                }
            }

            is SettingsEvent.RefreshNextSetTime -> {
                viewModelScope.launch {
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    val currentTime = LocalDateTime.now()
                    val nextSetTime: String?
                    if (_state.value.scheduleSeparately) {
                        val nextSetTime1 = currentTime.plusMinutes(_state.value.homeInterval.toLong())
                        val nextSetTime2 = currentTime.plusMinutes(_state.value.lockInterval.toLong())
                        nextSetTime = (if (nextSetTime1!!.isBefore(nextSetTime2)) nextSetTime1 else nextSetTime2)!!.format(formatter)
                    }
                    else {
                        nextSetTime = currentTime.plusMinutes(_state.value.homeInterval.toLong()).format(formatter)
                    }
                    _state.update {
                        it.copy(
                            lastSetTime = currentTime.format(formatter),
                            nextSetTime = nextSetTime
                        )
                    }
                }
            }

            is SettingsEvent.SetHome -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.HOME_WALLPAPER, event.home)
                    _state.update {
                        it.copy(
                            setHomeWallpaper = event.home
                        )
                    }
                }
            }

            is SettingsEvent.SetLock -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.LOCK_WALLPAPER, event.lock)
                    _state.update {
                        it.copy(
                            setLockWallpaper = event.lock
                        )
                    }
                }
            }

            is SettingsEvent.SetDarkenPercentage -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putInt(SettingsConstants.DARKEN_PERCENTAGE, event.darkenPercentage)
                    _state.update {
                        it.copy(
                            darkenPercentage = event.darkenPercentage
                        )
                    }
                }
            }

            is SettingsEvent.SetDarken -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.DARKEN, event.darken)
                    _state.update {
                        it.copy(
                            darken = event.darken
                        )
                    }
                }
            }

            is SettingsEvent.SetWallpaperScaling -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putString(SettingsConstants.WALLPAPER_SCALING, event.scaling.name)
                    _state.update {
                        it.copy(
                            wallpaperScaling = event.scaling
                        )
                    }
                }
            }

            is SettingsEvent.SetScheduleSeparately -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.SCHEDULE_SEPARATELY, event.scheduleSeparately)
                    _state.update {
                        it.copy(
                            scheduleSeparately = event.scheduleSeparately
                        )
                    }
                }
            }

            is SettingsEvent.SetBlur -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.BLUR, event.blur)
                    _state.update {
                        it.copy(
                            blur = event.blur
                        )
                    }
                }
            }

            is SettingsEvent.SetBlurPercentage -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putInt(SettingsConstants.BLUR_PERCENTAGE, event.blurPercentage)
                    _state.update {
                        it.copy(
                            blurPercentage = event.blurPercentage
                        )
                    }
                }
            }

            is SettingsEvent.Reset -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.DARK_MODE_TYPE)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.AMOLED_THEME_TYPE)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.DYNAMIC_THEME_TYPE)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.FIRST_LAUNCH)
                    settingsDataStoreImpl.deleteString(SettingsConstants.LAST_SET_TIME)
                    settingsDataStoreImpl.deleteString(SettingsConstants.NEXT_SET_TIME)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.ANIMATE_TYPE)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.ENABLE_CHANGER)
                    settingsDataStoreImpl.deleteInt(SettingsConstants.DARKEN_PERCENTAGE)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.DARKEN)
                    settingsDataStoreImpl.deleteString(SettingsConstants.WALLPAPER_SCALING)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.HOME_WALLPAPER)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.LOCK_WALLPAPER)
                    settingsDataStoreImpl.deleteInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL)
                    settingsDataStoreImpl.deleteInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.SCHEDULE_SEPARATELY)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.BLUR)
                    settingsDataStoreImpl.deleteInt(SettingsConstants.BLUR_PERCENTAGE)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.FIRST_SET)
                    settingsDataStoreImpl.deleteString(SettingsConstants.NEXT_SET_TIME_1)
                    settingsDataStoreImpl.deleteString(SettingsConstants.NEXT_SET_TIME_2)

                    _state.update {
                        it.copy(
                            darkMode = null,
                            amoledTheme = false,
                            dynamicTheming = false,
                            firstLaunch = true,
                            lastSetTime = null,
                            nextSetTime = null,
                            animate = true,
                            enableChanger = true,
                            darkenPercentage = 100,
                            darken = false,
                            wallpaperScaling = ScalingConstants.FILL,
                            setHomeWallpaper = false,
                            setLockWallpaper = false,
                            lockInterval = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT,
                            homeInterval = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT,
                            scheduleSeparately = false,
                            blur = false,
                            blurPercentage = 0
                        )
                    }
                }
            }
        }
    }
}
