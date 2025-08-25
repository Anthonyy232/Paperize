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
import kotlinx.coroutines.flow.*
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
class SettingsViewModel @Inject constructor(
    private val settingsDataStoreImpl: SettingsDataStore
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsState()
    )
    var setKeepOnScreenCondition: Boolean = true

    init {
        viewModelScope.launch {
            combine(
                settingsDataStoreImpl.getBooleanFlow(SettingsConstants.FIRST_LAUNCH),
                loadThemeSettingsFlow(),
                loadWallpaperSettingsFlow(),
                loadScheduleSettingsFlow(),
                loadEffectSettingsFlow()
            ) { firstLaunch, themeSettings, wallpaperSettings, scheduleSettings, effectSettings ->
                    _state.update {
                    it.copy(
                        firstLaunch = firstLaunch ?: true,
                        initialized = true,
                        themeSettings = themeSettings,
                        wallpaperSettings = wallpaperSettings,
                        scheduleSettings = scheduleSettings,
                        effectSettings = effectSettings
                    )
                }
                setKeepOnScreenCondition = false
            }.collect()
        }
    }

    private fun loadThemeSettingsFlow(): Flow<ThemeSettings> = combine(
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.DARK_MODE_TYPE),
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.AMOLED_THEME_TYPE),
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.DYNAMIC_THEME_TYPE),
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.ANIMATE_TYPE)
    ) { darkMode, amoledTheme, dynamicTheming, animate ->
        ThemeSettings(
            darkMode = darkMode,
            amoledTheme = amoledTheme ?: false,
            dynamicTheming = dynamicTheming ?: false,
            animate = animate ?: true
        )
    }

    private fun loadWallpaperSettingsFlow(): Flow<WallpaperSettings> = combine(
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.ENABLE_CHANGER),
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.ENABLE_HOME_WALLPAPER),
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.ENABLE_LOCK_WALLPAPER),
        settingsDataStoreImpl.getStringFlow(SettingsConstants.CURRENT_HOME_WALLPAPER),
        settingsDataStoreImpl.getStringFlow(SettingsConstants.CURRENT_LOCK_WALLPAPER),
        settingsDataStoreImpl.getStringFlow(SettingsConstants.NEXT_HOME_WALLPAPER),
        settingsDataStoreImpl.getStringFlow(SettingsConstants.NEXT_LOCK_WALLPAPER),
        settingsDataStoreImpl.getStringFlow(SettingsConstants.HOME_ALBUM_NAME),
        settingsDataStoreImpl.getStringFlow(SettingsConstants.LOCK_ALBUM_NAME),
        settingsDataStoreImpl.getStringFlow(SettingsConstants.WALLPAPER_SCALING)
    ) { flows ->
        WallpaperSettings(
            enableChanger = flows[0] as Boolean? ?: false,
            setHomeWallpaper = flows[1] as Boolean? ?: false,
            setLockWallpaper = flows[2] as Boolean? ?: false,
            currentHomeWallpaper = flows[3] as String?,
            currentLockWallpaper = flows[4] as String?,
            nextHomeWallpaper = flows[5] as String?,
            nextLockWallpaper = flows[6] as String?,
            homeAlbumName = flows[7] as String?,
            lockAlbumName = flows[8] as String?,
            wallpaperScaling = ScalingConstants.valueOf((flows[9] as String?) ?: ScalingConstants.FILL.name)
        )
    }

    private fun loadScheduleSettingsFlow(): Flow<ScheduleSettings> = combine(
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.SCHEDULE_SEPARATELY),
        settingsDataStoreImpl.getIntFlow(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL),
        settingsDataStoreImpl.getIntFlow(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL),
        settingsDataStoreImpl.getStringFlow(SettingsConstants.LAST_SET_TIME),
        settingsDataStoreImpl.getStringFlow(SettingsConstants.NEXT_SET_TIME),
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.CHANGE_START_TIME),
        settingsDataStoreImpl.getIntFlow(SettingsConstants.START_HOUR),
        settingsDataStoreImpl.getIntFlow(SettingsConstants.START_MINUTE),
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.SHUFFLE),
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.REFRESH),
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.SKIP_LANDSCAPE),
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.SKIP_NON_INTERACTIVE)
    ) { flows ->
        ScheduleSettings(
            scheduleSeparately = flows[0] as Boolean? ?: false,
            homeInterval = flows[1] as Int? ?: WALLPAPER_CHANGE_INTERVAL_DEFAULT,
            lockInterval = flows[2] as Int? ?: WALLPAPER_CHANGE_INTERVAL_DEFAULT,
            lastSetTime = flows[3] as String?,
            nextSetTime = flows[4] as String?,
            changeStartTime = flows[5] as Boolean? ?: false,
            startTime = Pair(flows[6] as Int? ?: 0, flows[7] as Int? ?: 0),
            shuffle = flows[8] as Boolean? ?: true,
            refresh = flows[9] as Boolean? ?: true,
            skipLandscape = flows[10] as Boolean? ?: false,
            skipNonInteractive = flows[11] as Boolean? ?: false,
        )
    }

    private fun loadEffectSettingsFlow(): Flow<EffectSettings> = combine(
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.DARKEN),
        settingsDataStoreImpl.getIntFlow(SettingsConstants.HOME_DARKEN_PERCENTAGE),
        settingsDataStoreImpl.getIntFlow(SettingsConstants.LOCK_DARKEN_PERCENTAGE),
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.BLUR),
        settingsDataStoreImpl.getIntFlow(SettingsConstants.HOME_BLUR_PERCENTAGE),
        settingsDataStoreImpl.getIntFlow(SettingsConstants.LOCK_BLUR_PERCENTAGE),
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.VIGNETTE),
        settingsDataStoreImpl.getIntFlow(SettingsConstants.HOME_VIGNETTE_PERCENTAGE),
        settingsDataStoreImpl.getIntFlow(SettingsConstants.LOCK_VIGNETTE_PERCENTAGE),
        settingsDataStoreImpl.getBooleanFlow(SettingsConstants.GRAYSCALE),
        settingsDataStoreImpl.getIntFlow(SettingsConstants.HOME_GRAYSCALE_PERCENTAGE),
        settingsDataStoreImpl.getIntFlow(SettingsConstants.LOCK_GRAYSCALE_PERCENTAGE)
    ) { flows ->
        EffectSettings(
            darken = flows[0] as Boolean? ?: false,
            homeDarkenPercentage = flows[1] as Int? ?: 100,
            lockDarkenPercentage = flows[2] as Int? ?: 100,
            blur = flows[3] as Boolean? ?: false,
            homeBlurPercentage = flows[4] as Int? ?: 0,
            lockBlurPercentage = flows[5] as Int? ?: 0,
            vignette = flows[6] as Boolean? ?: false,
            homeVignettePercentage = flows[7] as Int? ?: 0,
            lockVignettePercentage = flows[8] as Int? ?: 0,
            grayscale = flows[9] as Boolean? ?: false,
            homeGrayscalePercentage = flows[10] as Int? ?: 0,
            lockGrayscalePercentage = flows[11] as Int? ?: 0
        )
    }

    private fun calculateNextSetTime(currentTime: LocalDateTime, scheduleSettings: ScheduleSettings): Triple<LocalDateTime, LocalDateTime, LocalDateTime> {
        val homeNextTime = when {
            scheduleSettings.changeStartTime -> currentTime
            else -> currentTime.plusMinutes(scheduleSettings.homeInterval.toLong())
        }
        
        val lockNextTime = when {
            scheduleSettings.changeStartTime -> currentTime
            else -> currentTime.plusMinutes(scheduleSettings.lockInterval.toLong())
        }
        
        val nextSetTime = when {
            scheduleSettings.changeStartTime -> currentTime
            scheduleSettings.scheduleSeparately -> if (homeNextTime.isBefore(lockNextTime)) homeNextTime else lockNextTime
            else -> homeNextTime
        }
        
        return Triple(nextSetTime, homeNextTime, lockNextTime)
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetFirstLaunch -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.FIRST_LAUNCH, false)
                }
            }

            is SettingsEvent.SetChangerToggle -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_CHANGER, event.toggle)
                }
            }

            is SettingsEvent.SetDarkMode -> {
                viewModelScope.launch {
                    when (event.darkMode) {
                        true -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DARK_MODE_TYPE, true) }
                        false -> { settingsDataStoreImpl.putBoolean(SettingsConstants.DARK_MODE_TYPE, false) }
                        null -> { settingsDataStoreImpl.deleteBoolean(SettingsConstants.DARK_MODE_TYPE) }
                    }
                }
            }

            is SettingsEvent.SetAmoledTheme -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.AMOLED_THEME_TYPE, event.amoledTheme)
                    if (event.amoledTheme) {
                        settingsDataStoreImpl.putBoolean(SettingsConstants.DYNAMIC_THEME_TYPE, false)
                    }
                }
            }

            is SettingsEvent.SetDynamicTheming -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.DYNAMIC_THEME_TYPE, event.dynamicTheming)
                    if (event.dynamicTheming) {
                        settingsDataStoreImpl.putBoolean(SettingsConstants.AMOLED_THEME_TYPE, false)
                    }
                }
            }

            is SettingsEvent.SetAnimate -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ANIMATE_TYPE, event.animate)
                }
            }

            is SettingsEvent.SetHomeWallpaperInterval -> {
                viewModelScope.launch {
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
                }
            }

            is SettingsEvent.SetLockWallpaperInterval -> {
                viewModelScope.launch {
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

                    val (nextSetTime, homeNextTime, lockNextTime) = calculateNextSetTime(
                        currentTime,
                        _state.value.scheduleSettings
                    )

                    settingsDataStoreImpl.run {
                        putString(SettingsConstants.LAST_SET_TIME, LocalDateTime.now().format(formatter))
                        putString(SettingsConstants.NEXT_SET_TIME, nextSetTime.format(formatter))
                        putString(SettingsConstants.HOME_NEXT_SET_TIME, homeNextTime.toString())
                        putString(SettingsConstants.LOCK_NEXT_SET_TIME, lockNextTime.toString())
                    }
                }
            }

            is SettingsEvent.SetHome -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_HOME_WALLPAPER, event.home)
                }
            }

            is SettingsEvent.SetLock -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_LOCK_WALLPAPER, event.lock)
                }
            }

            is SettingsEvent.SetDarken -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.DARKEN, event.darken)
                }
            }

            is SettingsEvent.SetWallpaperScaling -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putString(SettingsConstants.WALLPAPER_SCALING, event.scaling.name)
                }
            }

            is SettingsEvent.SetScheduleSeparately -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.SCHEDULE_SEPARATELY, event.scheduleSeparately)
                }
            }

            is SettingsEvent.SetBlur -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.BLUR, event.blur)
                }
            }

            is SettingsEvent.SetDarkenPercentage -> {
                viewModelScope.launch {
                    if (event.lockDarkenPercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.LOCK_DARKEN_PERCENTAGE, event.lockDarkenPercentage)
                    }
                    if (event.homeDarkenPercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.HOME_DARKEN_PERCENTAGE, event.homeDarkenPercentage)
                    }
                }
            }

            is SettingsEvent.SetBlurPercentage -> {
                viewModelScope.launch {
                    if (event.lockBlurPercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.LOCK_BLUR_PERCENTAGE, event.lockBlurPercentage)
                    }
                    if (event.homeBlurPercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.HOME_BLUR_PERCENTAGE, event.homeBlurPercentage)
                    }
                }
            }

            is SettingsEvent.SetVignette -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.VIGNETTE, event.vignette)
                }
            }

            is SettingsEvent.SetVignettePercentage -> {
                viewModelScope.launch {
                    if (event.lockVignettePercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.LOCK_VIGNETTE_PERCENTAGE, event.lockVignettePercentage)
                    }
                    if (event.homeVignettePercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.HOME_VIGNETTE_PERCENTAGE, event.homeVignettePercentage)
                    }
                }
            }

            is SettingsEvent.SetGrayscale -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.GRAYSCALE, event.grayscale)
                }
            }

            is SettingsEvent.SetGrayscalePercentage -> {
                viewModelScope.launch {
                    if (event.lockGrayscalePercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.LOCK_GRAYSCALE_PERCENTAGE, event.lockGrayscalePercentage)
                    }
                    if (event.homeGrayscalePercentage != null) {
                        settingsDataStoreImpl.putInt(SettingsConstants.HOME_GRAYSCALE_PERCENTAGE, event.homeGrayscalePercentage)
                    }
                }
            }

            is SettingsEvent.SetCurrentWallpaper -> {
                viewModelScope.launch {
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

            is SettingsEvent.SetAlbum -> {
                viewModelScope.launch {
                    event.lockAlbumName?.let { settingsDataStoreImpl.putString(SettingsConstants.LOCK_ALBUM_NAME, it) }
                    event.homeAlbumName?.let { settingsDataStoreImpl.putString(SettingsConstants.HOME_ALBUM_NAME, it) }
                    val enableChanger: Boolean = when {
                        event.homeAlbumName != null && event.lockAlbumName != null -> { true }
                        event.homeAlbumName != null && !_state.value.wallpaperSettings.lockAlbumName.isNullOrEmpty() -> { true }
                        event.lockAlbumName != null && !_state.value.wallpaperSettings.homeAlbumName.isNullOrEmpty() -> { true }
                        else -> { false }
                    }
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_CHANGER, enableChanger)
                }
            }

            is SettingsEvent.RemoveSelectedAlbumAsType -> {
                viewModelScope.launch {
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
                    }
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.ENABLE_CHANGER)
                }
            }

            is SettingsEvent.RemoveSelectedAlbumAsName -> {
                viewModelScope.launch {
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
                }
            }

            is SettingsEvent.SetChangeStartTime -> {
                viewModelScope.launch {
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
                    settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                    settingsDataStoreImpl.putBoolean(SettingsConstants.CHANGE_START_TIME, event.changeStartTime)
                }
            }

            is SettingsEvent.SetStartTime -> {
                viewModelScope.launch {
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    val currentTime = LocalDateTime.now().withHour(event.hour).withMinute(event.minute).let {
                        if (it.isBefore(LocalDateTime.now())) it.plusDays(1) else it
                    }
                    val nextSetTime = currentTime.format(formatter)
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

            is SettingsEvent.SetShuffle -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.SHUFFLE, event.shuffle)
                }
            }

            is SettingsEvent.SetRefresh -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.REFRESH, event.refresh)
                }
            }

            is SettingsEvent.SetSkipLandscape -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.SKIP_LANDSCAPE, event.skipLandscape)
                }
            }


            is SettingsEvent.SetSkipNonInteractive -> {
                viewModelScope.launch {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.SKIP_NON_INTERACTIVE, event.skipNonInteractive)
                }
            }

            is SettingsEvent.Reset -> {
                viewModelScope.launch {
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
                        SettingsConstants.START_MINUTE,
                        SettingsConstants.SHUFFLE,
                        SettingsConstants.REFRESH,
                        SettingsConstants.SKIP_LANDSCAPE,
                        SettingsConstants.SKIP_NON_INTERACTIVE
                    )
                    settingsDataStoreImpl.clear(keysToDelete)
                }
            }
        }
    }
}
