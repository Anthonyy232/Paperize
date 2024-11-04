package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.core.SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState.EffectSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState.ScheduleSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState.ThemeSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState.WallpaperSettings
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
            val themeSettings = async { loadThemeSettings() }
            val wallpaperSettings = async { loadWallpaperSettings() }
            val scheduleSettings = async { loadScheduleSettings() }
            val effectSettings = async { loadEffectSettings() }
            _state.update {
                it.copy(
                    firstLaunch = firstLaunch.await(),
                    themeSettings = themeSettings.await(),
                    wallpaperSettings = wallpaperSettings.await(),
                    scheduleSettings = scheduleSettings.await(),
                    effectSettings = effectSettings.await()
                )
            }
            setKeepOnScreenCondition = false
        }
    }
    
    private suspend fun loadThemeSettings(): ThemeSettings {
        return ThemeSettings(
            darkMode = settingsDataStoreImpl.getBoolean(SettingsConstants.DARK_MODE_TYPE),
            amoledTheme = settingsDataStoreImpl.getBoolean(SettingsConstants.AMOLED_THEME_TYPE) ?: false,
            dynamicTheming = settingsDataStoreImpl.getBoolean(SettingsConstants.DYNAMIC_THEME_TYPE) ?: false,
            animate = settingsDataStoreImpl.getBoolean(SettingsConstants.ANIMATE_TYPE) ?: true
        )
    }
    
    private suspend fun loadWallpaperSettings(): WallpaperSettings {
        return WallpaperSettings(
            enableChanger = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: false,
            setHomeWallpaper = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_HOME_WALLPAPER) ?: false,
            setLockWallpaper = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_LOCK_WALLPAPER) ?: false,
            currentHomeWallpaper = settingsDataStoreImpl.getString(SettingsConstants.CURRENT_HOME_WALLPAPER),
            currentLockWallpaper = settingsDataStoreImpl.getString(SettingsConstants.CURRENT_LOCK_WALLPAPER),
            nextHomeWallpaper = settingsDataStoreImpl.getString(SettingsConstants.HOME_NEXT_SET_TIME),
            nextLockWallpaper = settingsDataStoreImpl.getString(SettingsConstants.LOCK_NEXT_SET_TIME),
            homeAlbumName = settingsDataStoreImpl.getString(SettingsConstants.HOME_ALBUM_NAME),
            lockAlbumName = settingsDataStoreImpl.getString(SettingsConstants.LOCK_ALBUM_NAME),
            wallpaperScaling = ScalingConstants.valueOf(
                settingsDataStoreImpl.getString(SettingsConstants.WALLPAPER_SCALING) 
                ?: ScalingConstants.FILL.name
            )
        )
    }
    
    private suspend fun loadScheduleSettings(): ScheduleSettings {
        return ScheduleSettings(
            scheduleSeparately = settingsDataStoreImpl.getBoolean(SettingsConstants.SCHEDULE_SEPARATELY) ?: false,
            homeInterval = settingsDataStoreImpl.getInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL) ?: WALLPAPER_CHANGE_INTERVAL_DEFAULT,
            lockInterval = settingsDataStoreImpl.getInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL) ?: WALLPAPER_CHANGE_INTERVAL_DEFAULT,
            lastSetTime = settingsDataStoreImpl.getString(SettingsConstants.LAST_SET_TIME),
            nextSetTime = settingsDataStoreImpl.getString(SettingsConstants.NEXT_SET_TIME),
            changeStartTime = settingsDataStoreImpl.getBoolean(SettingsConstants.CHANGE_START_TIME) ?: false,
            startTime = Pair(
                settingsDataStoreImpl.getInt(SettingsConstants.START_HOUR) ?: 0,
                settingsDataStoreImpl.getInt(SettingsConstants.START_MINUTE) ?: 0
            )
        )
    }
    
    private suspend fun loadEffectSettings(): EffectSettings {
        return EffectSettings(
            darken = settingsDataStoreImpl.getBoolean(SettingsConstants.DARKEN) ?: false,
            homeDarkenPercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_DARKEN_PERCENTAGE) ?: 100,
            lockDarkenPercentage = settingsDataStoreImpl.getInt(SettingsConstants.LOCK_DARKEN_PERCENTAGE) ?: 100,
            blur = settingsDataStoreImpl.getBoolean(SettingsConstants.BLUR) ?: false,
            homeBlurPercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_BLUR_PERCENTAGE) ?: 0,
            lockBlurPercentage = settingsDataStoreImpl.getInt(SettingsConstants.LOCK_BLUR_PERCENTAGE) ?: 0,
            vignette = settingsDataStoreImpl.getBoolean(SettingsConstants.VIGNETTE) ?: false,
            homeVignettePercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_VIGNETTE_PERCENTAGE) ?: 0,
            lockVignettePercentage = settingsDataStoreImpl.getInt(SettingsConstants.LOCK_VIGNETTE_PERCENTAGE) ?: 0,
            grayscale = settingsDataStoreImpl.getBoolean(SettingsConstants.GRAYSCALE) ?: false,
            homeGrayscalePercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_GRAYSCALE_PERCENTAGE) ?: 0,
            lockGrayscalePercentage = settingsDataStoreImpl.getInt(SettingsConstants.LOCK_GRAYSCALE_PERCENTAGE) ?: 0
        )
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetFirstLaunch -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.FIRST_LAUNCH, false)
                    _state.update {
                        it.copy(firstLaunch = false)
                    }
                }
            }

            is SettingsEvent.SetChangerToggle -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_CHANGER, event.toggle)
                    _state.update { currentState ->
                        currentState.copy(
                            wallpaperSettings = currentState.wallpaperSettings.copy(
                                enableChanger = event.toggle,
                                currentHomeWallpaper = if (!event.toggle) null else currentState.wallpaperSettings.currentHomeWallpaper,
                                currentLockWallpaper = if (!event.toggle) null else currentState.wallpaperSettings.currentLockWallpaper
                            )
                        )
                    }
                }
            }

            is SettingsEvent.Refresh -> {
                currentGetJob = viewModelScope.launch(Dispatchers.IO) {
                    val themeSettings = async { loadThemeSettings() }
                    val wallpaperSettings = async { loadWallpaperSettings() }
                    val scheduleSettings = async { loadScheduleSettings() }
                    val effectSettings = async { loadEffectSettings() }
                    val firstLaunch = async { settingsDataStoreImpl.getBoolean(SettingsConstants.FIRST_LAUNCH) ?: true }

                    _state.update {
                        it.copy(
                            firstLaunch = firstLaunch.await(),
                            themeSettings = themeSettings.await(),
                            wallpaperSettings = wallpaperSettings.await(),
                            scheduleSettings = scheduleSettings.await(),
                            effectSettings = effectSettings.await()
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
                            themeSettings = it.themeSettings.copy(
                                darkMode = event.darkMode
                            )
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
                            themeSettings = it.themeSettings.copy(
                                amoledTheme = event.amoledTheme,
                                dynamicTheming = if (event.amoledTheme) false else it.themeSettings.dynamicTheming
                            )
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
                            themeSettings = it.themeSettings.copy(
                                amoledTheme = if (event.dynamicTheming) false else it.themeSettings.amoledTheme,
                                dynamicTheming = event.dynamicTheming
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetAnimate -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ANIMATE_TYPE, event.animate)
                    _state.update {
                        it.copy(
                            themeSettings = it.themeSettings.copy(
                                animate = event.animate
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetHomeWallpaperInterval -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL, event.interval)
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    val currentTime = if (_state.value.scheduleSettings.changeStartTime) {
                        LocalDateTime.now().withHour(_state.value.scheduleSettings.startTime.first)
                            .withMinute(_state.value.scheduleSettings.startTime.second)
                    } else {
                        LocalDateTime.now()
                    }
                    val nextSetTime: String?
                    settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, currentTime.format(formatter))
                    if (_state.value.scheduleSettings.scheduleSeparately) {
                        val homeNextSetTime = currentTime.plusMinutes(event.interval.toLong())
                        val lockNextSetTime = currentTime.plusMinutes(_state.value.scheduleSettings.lockInterval.toLong())
                        nextSetTime = (if (homeNextSetTime!!.isBefore(lockNextSetTime)) homeNextSetTime else lockNextSetTime)!!.format(formatter)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                        settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, homeNextSetTime.toString())
                        settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, lockNextSetTime.toString())
                    }
                    else {
                        nextSetTime = currentTime.plusMinutes(event.interval.toLong()).format(formatter)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                        settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, currentTime.plusMinutes(event.interval.toLong()).toString())
                        settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, currentTime.plusMinutes(event.interval.toLong()).toString())
                    }
                    _state.update {
                        it.copy(
                            scheduleSettings = it.scheduleSettings.copy(
                                homeInterval = event.interval,
                                lastSetTime = currentTime.format(formatter),
                                nextSetTime = nextSetTime
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetLockWallpaperInterval -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL, event.interval)
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    val currentTime = if (_state.value.scheduleSettings.changeStartTime) {
                        LocalDateTime.now().withHour(_state.value.scheduleSettings.startTime.first)
                            .withMinute(_state.value.scheduleSettings.startTime.second)
                    } else {
                        LocalDateTime.now()
                    }
                    val nextSetTime: String?
                    settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, currentTime.format(formatter))
                    if (_state.value.scheduleSettings.scheduleSeparately) {
                        val nextSetTime1 = currentTime.plusMinutes(_state.value.scheduleSettings.homeInterval.toLong())
                        val nextSetTime2 = currentTime.plusMinutes(event.interval.toLong())
                        nextSetTime = (if (nextSetTime1!!.isBefore(nextSetTime2)) nextSetTime1 else nextSetTime2)!!.format(formatter)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                        settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, nextSetTime1.toString())
                        settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, nextSetTime2.toString())
                    }
                    else {
                        nextSetTime = currentTime.plusMinutes(event.interval.toLong()).format(formatter)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                        settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, currentTime.plusMinutes(event.interval.toLong()).toString())
                        settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, currentTime.plusMinutes(event.interval.toLong()).toString())
                    }
                    _state.update {
                        it.copy(
                            scheduleSettings = it.scheduleSettings.copy(
                                lockInterval = event.interval,
                                lastSetTime = currentTime.format(formatter),
                                nextSetTime = nextSetTime
                            )
                        )
                    }
                }
            }

            is SettingsEvent.RefreshNextSetTime -> {
                viewModelScope.launch {
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    val currentTime = if (_state.value.scheduleSettings.changeStartTime) {
                        var calculatedTime = LocalDateTime.now()
                            .withHour(_state.value.scheduleSettings.startTime.first)
                            .withMinute(_state.value.scheduleSettings.startTime.second)
                        if (calculatedTime.isBefore(LocalDateTime.now())) {
                            calculatedTime = calculatedTime.plusDays(1)
                        }
                        calculatedTime
                    } else {
                        LocalDateTime.now()
                    }
                    val nextSetTime = when {
                        _state.value.scheduleSettings.changeStartTime -> {
                            settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, currentTime.toString())
                            settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, currentTime.toString())
                            currentTime
                        }
                        _state.value.scheduleSettings.scheduleSeparately -> {
                            settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME,
                                currentTime.plusMinutes(_state.value.scheduleSettings.homeInterval.toLong()).toString())
                            settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME,
                                currentTime.plusMinutes(_state.value.scheduleSettings.lockInterval.toLong()).toString())
                            val nextSetTime1 = currentTime.plusMinutes(_state.value.scheduleSettings.homeInterval.toLong())
                            val nextSetTime2 = currentTime.plusMinutes(_state.value.scheduleSettings.lockInterval.toLong())
                            if (nextSetTime1.isBefore(nextSetTime2)) nextSetTime1 else nextSetTime2
                        }
                        else -> {
                            settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME,
                                currentTime.plusMinutes(_state.value.scheduleSettings.homeInterval.toLong()).toString())
                            settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME,
                                currentTime.plusMinutes(_state.value.scheduleSettings.lockInterval.toLong()).toString())
                            currentTime.plusMinutes(_state.value.scheduleSettings.homeInterval.toLong())
                        }
                    }.format(formatter)
                    _state.update {
                        it.copy(
                            scheduleSettings = it.scheduleSettings.copy(
                                lastSetTime = LocalDateTime.now().format(formatter),
                                nextSetTime = nextSetTime
                            )
                        )
                    }
                    settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, LocalDateTime.now().format(formatter))
                    settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                }
            }

            is SettingsEvent.SetHome -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_HOME_WALLPAPER, event.home)
                    _state.update {
                        it.copy(
                            wallpaperSettings = it.wallpaperSettings.copy(
                                setHomeWallpaper = event.home
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetLock -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_LOCK_WALLPAPER, event.lock)
                    _state.update {
                        it.copy(
                            wallpaperSettings = it.wallpaperSettings.copy(
                                setLockWallpaper = event.lock
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetDarken -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.DARKEN, event.darken)
                    _state.update {
                        it.copy(
                            effectSettings = it.effectSettings.copy(
                                darken = event.darken
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetWallpaperScaling -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putString(SettingsConstants.WALLPAPER_SCALING, event.scaling.name)
                    _state.update {
                        it.copy(
                            wallpaperSettings = it.wallpaperSettings.copy(
                                wallpaperScaling = event.scaling
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetScheduleSeparately -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.SCHEDULE_SEPARATELY, event.scheduleSeparately)
                    _state.update {
                        it.copy(
                            scheduleSettings = it.scheduleSettings.copy(
                                scheduleSeparately = event.scheduleSeparately
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetBlur -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.BLUR, event.blur)
                    _state.update {
                        it.copy(
                            effectSettings = it.effectSettings.copy(
                                blur = event.blur
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetDarkenPercentage -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (event.lockDarkenPercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.LOCK_DARKEN_PERCENTAGE, event.lockDarkenPercentage)
                    }
                    if (event.homeDarkenPercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.HOME_DARKEN_PERCENTAGE, event.homeDarkenPercentage)
                    }
                    _state.update {
                        it.copy(
                            effectSettings = it.effectSettings.copy(
                                homeDarkenPercentage = event.homeDarkenPercentage ?: it.effectSettings.homeDarkenPercentage,
                                lockDarkenPercentage = event.lockDarkenPercentage ?: it.effectSettings.lockDarkenPercentage
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetBlurPercentage -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (event.lockBlurPercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.LOCK_BLUR_PERCENTAGE, event.lockBlurPercentage)
                    }
                    if (event.homeBlurPercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.HOME_BLUR_PERCENTAGE, event.homeBlurPercentage)
                    }
                    _state.update {
                        it.copy(
                            effectSettings = it.effectSettings.copy(
                                homeBlurPercentage = event.homeBlurPercentage ?: it.effectSettings.homeBlurPercentage,
                                lockBlurPercentage = event.lockBlurPercentage ?: it.effectSettings.lockBlurPercentage
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetVignette -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.VIGNETTE, event.vignette)
                    _state.update {
                        it.copy(
                            effectSettings = it.effectSettings.copy(
                                vignette = event.vignette
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetVignettePercentage -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (event.lockVignettePercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.LOCK_VIGNETTE_PERCENTAGE, event.lockVignettePercentage)
                    }
                    if (event.homeVignettePercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.HOME_VIGNETTE_PERCENTAGE, event.homeVignettePercentage)
                    }
                    _state.update {
                        it.copy(
                            effectSettings = it.effectSettings.copy(
                                homeVignettePercentage = event.homeVignettePercentage ?: it.effectSettings.homeVignettePercentage,
                                lockVignettePercentage = event.lockVignettePercentage ?: it.effectSettings.lockVignettePercentage
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetGrayscale -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.GRAYSCALE, event.grayscale)
                    _state.update {
                        it.copy(
                            effectSettings = it.effectSettings.copy(
                                grayscale = event.grayscale
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetGrayscalePercentage -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (event.lockGrayscalePercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.LOCK_GRAYSCALE_PERCENTAGE, event.lockGrayscalePercentage)
                    }
                    if (event.homeGrayscalePercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.HOME_GRAYSCALE_PERCENTAGE, event.homeGrayscalePercentage)
                    }
                    _state.update {
                        it.copy(
                            effectSettings = it.effectSettings.copy(
                                homeGrayscalePercentage = event.homeGrayscalePercentage ?: it.effectSettings.homeGrayscalePercentage,
                                lockGrayscalePercentage = event.lockGrayscalePercentage ?: it.effectSettings.lockGrayscalePercentage
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetCurrentHomeWallpaper -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (event.currentHomeWallpaper != null) {
                        settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, event.currentHomeWallpaper)
                    }
                    _state.update {
                        it.copy(
                            wallpaperSettings = it.wallpaperSettings.copy(
                                currentHomeWallpaper = event.currentHomeWallpaper
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetCurrentLockWallpaper -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (event.currentLockWallpaper != null) {
                        settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, event.currentLockWallpaper)
                    }
                    _state.update {
                        it.copy(
                            wallpaperSettings = it.wallpaperSettings.copy(
                                currentLockWallpaper = event.currentLockWallpaper
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetCurrentWallpaper -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (event.currentLockWallpaper != null) {
                        settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, event.currentLockWallpaper)
                    }
                    if (event.currentHomeWallpaper != null) {
                        settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, event.currentHomeWallpaper)
                    }
                    _state.update {
                        it.copy(
                            wallpaperSettings = it.wallpaperSettings.copy(
                                currentLockWallpaper = event.currentLockWallpaper ?: it.wallpaperSettings.currentLockWallpaper,
                                currentHomeWallpaper = event.currentHomeWallpaper ?: it.wallpaperSettings.currentHomeWallpaper
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetAlbumName -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (event.homeAlbumName != null) {
                        settingsDataStoreImpl.putString(SettingsConstants.HOME_ALBUM_NAME, event.homeAlbumName)
                    }
                    if (event.lockAlbumName != null) {
                        settingsDataStoreImpl.putString(SettingsConstants.LOCK_ALBUM_NAME, event.lockAlbumName)
                    }
                    val enableChanger: Boolean = when {
                        event.homeAlbumName != null && event.lockAlbumName != null -> { true }
                        event.homeAlbumName != null && !_state.value.wallpaperSettings.lockAlbumName.isNullOrEmpty() -> { true }
                        event.lockAlbumName != null && !_state.value.wallpaperSettings.homeAlbumName.isNullOrEmpty() -> { true }
                        else -> { false }
                    }
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_CHANGER, enableChanger)
                    _state.update {
                        it.copy(
                            wallpaperSettings = it.wallpaperSettings.copy(
                                homeAlbumName = event.homeAlbumName ?: it.wallpaperSettings.homeAlbumName,
                                lockAlbumName = event.lockAlbumName ?: it.wallpaperSettings.lockAlbumName,
                                enableChanger = enableChanger
                            )
                        )
                    }
                }
            }

            is SettingsEvent.RemoveSelectedAlbumAsType -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (_state.value.wallpaperSettings.setLockWallpaper && _state.value.wallpaperSettings.setHomeWallpaper) {
                        if (event.removeLock) {
                            settingsDataStoreImpl.deleteString(SettingsConstants.LOCK_ALBUM_NAME)
                        }
                        if (event.removeHome) {
                            settingsDataStoreImpl.deleteString(SettingsConstants.HOME_ALBUM_NAME)
                        }
                        settingsDataStoreImpl.deleteString(SettingsConstants.CURRENT_LOCK_WALLPAPER)
                        settingsDataStoreImpl.deleteString(SettingsConstants.CURRENT_HOME_WALLPAPER)
                        settingsDataStoreImpl.deleteString(SettingsConstants.LAST_SET_TIME)
                        settingsDataStoreImpl.deleteString(SettingsConstants.NEXT_SET_TIME)
                        _state.update {
                            it.copy(
                                wallpaperSettings = it.wallpaperSettings.copy(
                                    homeAlbumName = if (event.removeHome) null else it.wallpaperSettings.homeAlbumName,
                                    lockAlbumName = if (event.removeLock) null else it.wallpaperSettings.lockAlbumName,
                                    currentHomeWallpaper = null,
                                    currentLockWallpaper = null,
                                    nextHomeWallpaper = null,
                                    nextLockWallpaper = null,
                                    enableChanger = false
                                ),
                                scheduleSettings = it.scheduleSettings.copy(
                                    lastSetTime = null,
                                    nextSetTime = null
                                )
                            )
                        }
                    }
                    else {
                        if (event.removeLock) {
                            settingsDataStoreImpl.deleteString(SettingsConstants.LOCK_ALBUM_NAME)
                            settingsDataStoreImpl.deleteString(SettingsConstants.CURRENT_LOCK_WALLPAPER)
                        }
                        if (event.removeHome) {
                            settingsDataStoreImpl.deleteString(SettingsConstants.HOME_ALBUM_NAME)
                            settingsDataStoreImpl.deleteString(SettingsConstants.CURRENT_HOME_WALLPAPER)
                        }
                        _state.update {
                            it.copy(
                                wallpaperSettings = it.wallpaperSettings.copy(
                                    homeAlbumName = if (event.removeHome) null else it.wallpaperSettings.homeAlbumName,
                                    lockAlbumName = if (event.removeLock) null else it.wallpaperSettings.lockAlbumName,
                                    currentHomeWallpaper = if (event.removeHome) null else it.wallpaperSettings.currentHomeWallpaper,
                                    currentLockWallpaper = if (event.removeLock) null else it.wallpaperSettings.currentLockWallpaper,
                                    enableChanger = false
                                )
                            )
                        }
                    }
                }
            }

            is SettingsEvent.RemoveSelectedAlbumAsName -> {
                viewModelScope.launch(Dispatchers.IO) {
                    var enableChanger = _state.value.wallpaperSettings.enableChanger
                    if (event.albumName == _state.value.wallpaperSettings.lockAlbumName) {
                        settingsDataStoreImpl.deleteString(SettingsConstants.LOCK_ALBUM_NAME)
                        settingsDataStoreImpl.deleteString(SettingsConstants.CURRENT_LOCK_WALLPAPER)
                        enableChanger = false
                    }
                    if (event.albumName == _state.value.wallpaperSettings.homeAlbumName) {
                        settingsDataStoreImpl.deleteString(SettingsConstants.HOME_ALBUM_NAME)
                        settingsDataStoreImpl.deleteString(SettingsConstants.CURRENT_HOME_WALLPAPER)
                        enableChanger = false
                    }
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_CHANGER, enableChanger)
                    _state.update {
                        it.copy(
                            wallpaperSettings = it.wallpaperSettings.copy(
                                homeAlbumName = if (event.albumName == _state.value.wallpaperSettings.homeAlbumName) null else it.wallpaperSettings.homeAlbumName,
                                lockAlbumName = if (event.albumName == _state.value.wallpaperSettings.lockAlbumName) null else it.wallpaperSettings.lockAlbumName,
                                currentHomeWallpaper = if (event.albumName == _state.value.wallpaperSettings.homeAlbumName) null else it.wallpaperSettings.currentHomeWallpaper,
                                currentLockWallpaper = if (event.albumName == _state.value.wallpaperSettings.lockAlbumName) null else it.wallpaperSettings.currentLockWallpaper,
                                enableChanger = enableChanger
                            )
                        )

                    }
                }
            }

            is SettingsEvent.SetNextHomeWallpaper -> {
                viewModelScope.launch(Dispatchers.IO) {
                    event.nextHomeWallpaper?.let {
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_HOME_WALLPAPER, it)
                    }
                    _state.update {
                        it.copy(
                            wallpaperSettings = it.wallpaperSettings.copy(
                                nextHomeWallpaper = event.nextHomeWallpaper
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetNextLockWallpaper -> {
                viewModelScope.launch(Dispatchers.IO) {
                    event.nextLockWallpaper?.let {
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_LOCK_WALLPAPER, it)
                    }
                    _state.update {
                        it.copy(
                            wallpaperSettings = it.wallpaperSettings.copy(
                                nextLockWallpaper = event.nextLockWallpaper
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetNextWallpaper -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (event.nextHomeWallpaper != null) {
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_HOME_WALLPAPER, event.nextHomeWallpaper)
                    }
                    if (event.nextLockWallpaper != null) {
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_LOCK_WALLPAPER, event.nextLockWallpaper)
                    }
                    _state.update {
                        it.copy(
                            wallpaperSettings = it.wallpaperSettings.copy(
                                nextHomeWallpaper = event.nextHomeWallpaper ?: it.wallpaperSettings.nextHomeWallpaper,
                                nextLockWallpaper = event.nextLockWallpaper ?: it.wallpaperSettings.nextLockWallpaper
                            )
                        )
                    }
                }
            }

            is SettingsEvent.RefreshNextWallpaper -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val nextHomeWallpaper = async { settingsDataStoreImpl.getString(SettingsConstants.NEXT_HOME_WALLPAPER) }
                    val nextLockWallpaper = async { settingsDataStoreImpl.getString(SettingsConstants.NEXT_LOCK_WALLPAPER) }
                    _state.update {
                        it.copy(
                            wallpaperSettings = it.wallpaperSettings.copy(
                                nextHomeWallpaper = nextHomeWallpaper.await(),
                                nextLockWallpaper = nextLockWallpaper.await(),
                                currentHomeWallpaper = nextHomeWallpaper.await(),
                                currentLockWallpaper = nextLockWallpaper.await()
                            )
                        )
                    }
                }
            }

            is SettingsEvent.SetChangeStartTime -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    val currentTime = if (event.changeStartTime) {
                        LocalDateTime.now()
                            .withHour(_state.value.scheduleSettings.startTime.first)
                            .withMinute(_state.value.scheduleSettings.startTime.second)
                            .let {
                                if (it.isBefore(LocalDateTime.now())) it.plusDays(1) else it
                            }
                    } else {
                        LocalDateTime.now()
                    }
                    val nextSetTime = when {
                        event.changeStartTime -> {
                            settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, currentTime.toString())
                            settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, currentTime.toString())
                            currentTime.format(formatter)
                        }
                        _state.value.scheduleSettings.scheduleSeparately -> {
                            val nextSetTime1 = currentTime.plusMinutes(_state.value.scheduleSettings.homeInterval.toLong())
                            val nextSetTime2 = currentTime.plusMinutes(_state.value.scheduleSettings.lockInterval.toLong())
                            val nextSetTime = if (nextSetTime1.isBefore(nextSetTime2)) nextSetTime1 else nextSetTime2
                            settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, nextSetTime1.toString())
                            settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, nextSetTime2.toString())
                            nextSetTime.format(formatter)
                        }
                        else -> {
                            val nextSetTime = currentTime.plusMinutes(_state.value.scheduleSettings.homeInterval.toLong())
                            settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, nextSetTime.toString())
                            settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, nextSetTime.toString())
                            nextSetTime.format(formatter)
                        }
                    }
                    _state.update {
                        it.copy(
                            scheduleSettings = it.scheduleSettings.copy(
                                changeStartTime = event.changeStartTime,
                                nextSetTime = nextSetTime
                            )
                        )
                    }
                    settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                    settingsDataStoreImpl.putBoolean(SettingsConstants.CHANGE_START_TIME, event.changeStartTime)
                }
            }

            is SettingsEvent.SetStartTime -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    val currentTime = LocalDateTime.now().withHour(event.hour).withMinute(event.minute).let {
                        if (it.isBefore(LocalDateTime.now())) it.plusDays(1) else it
                    }
                    val nextSetTime = currentTime.format(formatter)
                    _state.update {
                        it.copy(
                            scheduleSettings = it.scheduleSettings.copy(
                                startTime = Pair(event.hour, event.minute),
                                changeStartTime = true,
                                nextSetTime = nextSetTime
                            )
                        )
                    }
                    settingsDataStoreImpl.run {
                        putString(SettingsConstants.HOME_NEXT_SET_TIME, currentTime.toString())
                        putString(SettingsConstants.LOCK_NEXT_SET_TIME, currentTime.toString())
                        putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                        putInt(SettingsConstants.START_HOUR, event.hour)
                        putInt(SettingsConstants.START_MINUTE, event.minute)
                        putBoolean(SettingsConstants.CHANGE_START_TIME, true)
                    }
                }
            }

            is SettingsEvent.Reset -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val keysToDelete = listOf(
                        SettingsConstants.DARK_MODE_TYPE,
                        SettingsConstants.AMOLED_THEME_TYPE,
                        SettingsConstants.DYNAMIC_THEME_TYPE,
                        SettingsConstants.FIRST_LAUNCH,
                        SettingsConstants.LAST_SET_TIME,
                        SettingsConstants.NEXT_SET_TIME,
                        SettingsConstants.ANIMATE_TYPE,
                        SettingsConstants.ENABLE_CHANGER,
                        SettingsConstants.HOME_DARKEN_PERCENTAGE,
                        SettingsConstants.LOCK_DARKEN_PERCENTAGE,
                        SettingsConstants.DARKEN,
                        SettingsConstants.WALLPAPER_SCALING,
                        SettingsConstants.ENABLE_HOME_WALLPAPER,
                        SettingsConstants.ENABLE_LOCK_WALLPAPER,
                        SettingsConstants.CURRENT_HOME_WALLPAPER,
                        SettingsConstants.CURRENT_LOCK_WALLPAPER,
                        SettingsConstants.HOME_ALBUM_NAME,
                        SettingsConstants.LOCK_ALBUM_NAME,
                        SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL,
                        SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL,
                        SettingsConstants.SCHEDULE_SEPARATELY,
                        SettingsConstants.BLUR,
                        SettingsConstants.HOME_BLUR_PERCENTAGE,
                        SettingsConstants.LOCK_BLUR_PERCENTAGE,
                        SettingsConstants.FIRST_SET,
                        SettingsConstants.HOME_NEXT_SET_TIME,
                        SettingsConstants.LOCK_NEXT_SET_TIME,
                        SettingsConstants.NEXT_HOME_WALLPAPER,
                        SettingsConstants.NEXT_LOCK_WALLPAPER,
                        SettingsConstants.VIGNETTE,
                        SettingsConstants.HOME_VIGNETTE_PERCENTAGE,
                        SettingsConstants.LOCK_VIGNETTE_PERCENTAGE,
                        SettingsConstants.GRAYSCALE,
                        SettingsConstants.HOME_GRAYSCALE_PERCENTAGE,
                        SettingsConstants.LOCK_GRAYSCALE_PERCENTAGE,
                        SettingsConstants.CHANGE_START_TIME,
                        SettingsConstants.START_HOUR,
                        SettingsConstants.START_MINUTE
                    )
                    settingsDataStoreImpl.clear(keysToDelete)
                    _state.update {
                        it.copy(
                            firstLaunch = true,
                            themeSettings = ThemeSettings(),
                            scheduleSettings = ScheduleSettings(),
                            wallpaperSettings = WallpaperSettings(),
                            effectSettings = EffectSettings()
                        )
                    }
                }
            }
        }
    }
}
