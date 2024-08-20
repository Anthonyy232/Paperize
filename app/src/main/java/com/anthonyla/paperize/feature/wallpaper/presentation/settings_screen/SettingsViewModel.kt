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
            val enableChanger = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: false }
            val setHomeWallpaper = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_HOME_WALLPAPER) ?: false }
            val setLockWallpaper = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_LOCK_WALLPAPER) ?: false }
            val setCurrentHomeWallpaper = async { settingsDataStoreImpl.getString(SettingsConstants.CURRENT_HOME_WALLPAPER) }
            val setCurrentLockWallpaper = async { settingsDataStoreImpl.getString(SettingsConstants.CURRENT_LOCK_WALLPAPER) }
            val homeWallpaperInterval = async { settingsDataStoreImpl.getInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT }
            val lockWallpaperInterval = async { settingsDataStoreImpl.getInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT }
            val homeAlbumName = async { settingsDataStoreImpl.getString(SettingsConstants.HOME_ALBUM_NAME) }
            val lockAlbumName = async { settingsDataStoreImpl.getString(SettingsConstants.LOCK_ALBUM_NAME) }
            val lastSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.LAST_SET_TIME) }
            val nextSetTime = async { settingsDataStoreImpl.getString(SettingsConstants.NEXT_SET_TIME) }
            val animate = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ANIMATE_TYPE) ?: true }
            val homeDarkenPercentage = async { settingsDataStoreImpl.getInt(SettingsConstants.HOME_DARKEN_PERCENTAGE) ?: 100 }
            val lockDarkenPercentage = async { settingsDataStoreImpl.getInt(SettingsConstants.LOCK_DARKEN_PERCENTAGE) ?: 100 }
            val darken = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DARKEN) ?: false }
            val wallpaperScaling = async { settingsDataStoreImpl.getBoolean(SettingsConstants.WALLPAPER_SCALING) ?: false }
            val wallpaperScalingMode = async { ScalingConstants.valueOf(settingsDataStoreImpl.getString(SettingsConstants.WALLPAPER_SCALING_MODE) ?: ScalingConstants.FILL.name) }
            val scheduleSeparately = async { settingsDataStoreImpl.getBoolean(SettingsConstants.SCHEDULE_SEPARATELY) ?: false }
            val blur = async { settingsDataStoreImpl.getBoolean(SettingsConstants.BLUR) ?: false }
            val homeBlurPercentage = async { settingsDataStoreImpl.getInt(SettingsConstants.HOME_BLUR_PERCENTAGE) ?: 0 }
            val lockBlurPercentage = async { settingsDataStoreImpl.getInt(SettingsConstants.LOCK_BLUR_PERCENTAGE) ?: 0 }
            val nextHomeWallpaper = async { settingsDataStoreImpl.getString(SettingsConstants.HOME_NEXT_SET_TIME) }
            val nextLockWallpaper = async { settingsDataStoreImpl.getString(SettingsConstants.LOCK_NEXT_SET_TIME) }

            _state.update {
                it.copy(
                    darkMode = darkMode.await(),
                    amoledTheme = amoledTheme.await(),
                    dynamicTheming = dynamicTheming.await(),
                    homeInterval = homeWallpaperInterval.await(),
                    lockInterval = lockWallpaperInterval.await(),
                    homeAlbumName = homeAlbumName.await(),
                    lockAlbumName = lockAlbumName.await(),
                    firstLaunch = firstLaunch.await(),
                    lastSetTime = lastSetTime.await(),
                    nextSetTime = nextSetTime.await(),
                    animate = animate.await(),
                    enableChanger = enableChanger.await(),
                    homeDarkenPercentage = homeDarkenPercentage.await(),
                    lockDarkenPercentage = lockDarkenPercentage.await(),
                    darken = darken.await(),
                    wallpaperScaling = wallpaperScaling.await(),
                    wallpaperScalingMode = wallpaperScalingMode.await(),
                    setHomeWallpaper = setHomeWallpaper.await(),
                    setLockWallpaper = setLockWallpaper.await(),
                    currentHomeWallpaper = setCurrentHomeWallpaper.await(),
                    currentLockWallpaper = setCurrentLockWallpaper.await(),
                    scheduleSeparately = scheduleSeparately.await(),
                    blur = blur.await(),
                    homeBlurPercentage = homeBlurPercentage.await(),
                    lockBlurPercentage = lockBlurPercentage.await(),
                    nextHomeWallpaper = nextHomeWallpaper.await(),
                    nextLockWallpaper = nextLockWallpaper.await()
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
                    val enableChanger = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: false }
                    val homeDarkenPercentage = async { settingsDataStoreImpl.getInt(SettingsConstants.HOME_DARKEN_PERCENTAGE) ?: 0 }
                    val lockDarkenPercentage = async { settingsDataStoreImpl.getInt(SettingsConstants.LOCK_DARKEN_PERCENTAGE) ?: 0 }
                    val darken = async { settingsDataStoreImpl.getBoolean(SettingsConstants.DARKEN) ?: false }
                    val setHomeWallpaper = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_HOME_WALLPAPER) ?: false }
                    val setLockWallpaper = async { settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_LOCK_WALLPAPER) ?: false }
                    val homeAlbumName = async { settingsDataStoreImpl.getString(SettingsConstants.HOME_ALBUM_NAME) }
                    val lockAlbumName = async { settingsDataStoreImpl.getString(SettingsConstants.LOCK_ALBUM_NAME) }
                    val currentHomeWallpaper = async { settingsDataStoreImpl.getString(SettingsConstants.CURRENT_HOME_WALLPAPER) }
                    val currentLockWallpaper = async { settingsDataStoreImpl.getString(SettingsConstants.CURRENT_LOCK_WALLPAPER) }
                    val homeWallpaperInterval = async { settingsDataStoreImpl.getInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT }
                    val lockWallpaperInterval = async { settingsDataStoreImpl.getInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT }
                    val scheduleSeparately = async { settingsDataStoreImpl.getBoolean(SettingsConstants.SCHEDULE_SEPARATELY) ?: false }
                    val blur = async { settingsDataStoreImpl.getBoolean(SettingsConstants.BLUR) ?: false }
                    val homeBlurPercentage = async { settingsDataStoreImpl.getInt(SettingsConstants.HOME_BLUR_PERCENTAGE) ?: 0 }
                    val lockBlurPercentage = async { settingsDataStoreImpl.getInt(SettingsConstants.LOCK_BLUR_PERCENTAGE) ?: 0 }
                    val nextHomeWallpaper = async { settingsDataStoreImpl.getString(SettingsConstants.HOME_NEXT_SET_TIME) }
                    val nextLockWallpaper = async { settingsDataStoreImpl.getString(SettingsConstants.LOCK_NEXT_SET_TIME) }
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
                            homeDarkenPercentage = homeDarkenPercentage.await(),
                            lockDarkenPercentage = lockDarkenPercentage.await(),
                            darken = darken.await(),
                            setHomeWallpaper = setHomeWallpaper.await(),
                            setLockWallpaper = setLockWallpaper.await(),
                            homeAlbumName = homeAlbumName.await(),
                            lockAlbumName = lockAlbumName.await(),
                            currentHomeWallpaper = currentHomeWallpaper.await(),
                            currentLockWallpaper = currentLockWallpaper.await(),
                            lockInterval = lockWallpaperInterval.await(),
                            homeInterval = homeWallpaperInterval.await(),
                            scheduleSeparately = scheduleSeparately.await(),
                            blur = blur.await(),
                            homeBlurPercentage = homeBlurPercentage.await(),
                            lockBlurPercentage = lockBlurPercentage.await(),
                            nextHomeWallpaper = nextHomeWallpaper.await(),
                            nextLockWallpaper = nextLockWallpaper.await()
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
                        val homeNextSetTime = currentTime.plusMinutes(event.interval.toLong())
                        val lockNextSetTime = currentTime.plusMinutes(_state.value.lockInterval.toLong())
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
                    settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, currentTime.format(formatter))
                    if (_state.value.scheduleSeparately) {
                        val nextSetTime1 = currentTime.plusMinutes(_state.value.homeInterval.toLong())
                        val nextSetTime2 = currentTime.plusMinutes(_state.value.lockInterval.toLong())
                        nextSetTime = (if (nextSetTime1!!.isBefore(nextSetTime2)) nextSetTime1 else nextSetTime2)!!.format(formatter)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                        settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, nextSetTime1.toString())
                        settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, nextSetTime2.toString())
                    }
                    else {
                        nextSetTime = currentTime.plusMinutes(_state.value.homeInterval.toLong()).format(formatter)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime)
                        settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, currentTime.plusMinutes(_state.value.homeInterval.toLong()).toString())
                        settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, currentTime.plusMinutes(_state.value.lockInterval.toLong()).toString())
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
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_HOME_WALLPAPER, event.home)
                    _state.update {
                        it.copy(
                            setHomeWallpaper = event.home
                        )
                    }
                }
            }

            is SettingsEvent.SetLock -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_LOCK_WALLPAPER, event.lock)
                    _state.update {
                        it.copy(
                            setLockWallpaper = event.lock
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
                    settingsDataStoreImpl.putBoolean(SettingsConstants.WALLPAPER_SCALING, event.scaling)
                    _state.update {
                        it.copy(
                            wallpaperScaling = event.scaling
                        )
                    }
                }
            }

            is SettingsEvent.SetWallpaperScalingMode -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsDataStoreImpl.putString(SettingsConstants.WALLPAPER_SCALING_MODE, event.scalingMode.name)
                    _state.update {
                        it.copy(
                            wallpaperScalingMode = event.scalingMode
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
                            homeDarkenPercentage = event.homeDarkenPercentage ?: it.homeDarkenPercentage,
                            lockDarkenPercentage = event.lockDarkenPercentage ?: it.lockDarkenPercentage
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
                            homeBlurPercentage = event.homeBlurPercentage ?: it.homeBlurPercentage,
                            lockBlurPercentage = event.lockBlurPercentage ?: it.lockBlurPercentage
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
                            currentHomeWallpaper = event.currentHomeWallpaper
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
                            currentLockWallpaper = event.currentLockWallpaper
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
                            currentLockWallpaper = event.currentLockWallpaper ?: it.currentLockWallpaper,
                            currentHomeWallpaper = event.currentHomeWallpaper ?: it.currentHomeWallpaper
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
                        event.homeAlbumName != null && !_state.value.lockAlbumName.isNullOrEmpty() -> { true }
                        event.lockAlbumName != null && !_state.value.homeAlbumName.isNullOrEmpty() -> { true }
                        else -> { false }
                    }
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_CHANGER, enableChanger)
                    _state.update {
                        it.copy(
                            homeAlbumName = event.homeAlbumName ?: it.homeAlbumName,
                            lockAlbumName = event.lockAlbumName ?: it.lockAlbumName,
                            enableChanger = enableChanger
                        )
                    }
                }
            }

            is SettingsEvent.RemoveSelectedAlbumAsType -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (_state.value.setLockWallpaper && _state.value.setHomeWallpaper) {
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
                                homeAlbumName = if (event.removeHome) null else it.homeAlbumName,
                                lockAlbumName = if (event.removeLock) null else it.lockAlbumName,
                                currentHomeWallpaper = null,
                                currentLockWallpaper = null,
                                nextHomeWallpaper = null,
                                nextLockWallpaper = null,
                                enableChanger = false,
                                lastSetTime = null,
                                nextSetTime = null
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
                                homeAlbumName = if (event.removeHome) null else it.homeAlbumName,
                                lockAlbumName = if (event.removeLock) null else it.lockAlbumName,
                                currentHomeWallpaper = if (event.removeHome) null else it.currentHomeWallpaper,
                                currentLockWallpaper = if (event.removeLock) null else it.currentLockWallpaper,
                                enableChanger = false
                            )
                        }
                    }
                }
            }

            is SettingsEvent.RemoveSelectedAlbumAsName -> {
                viewModelScope.launch(Dispatchers.IO) {
                    var enableChanger = _state.value.enableChanger
                    if (event.albumName == _state.value.lockAlbumName) {
                        settingsDataStoreImpl.deleteString(SettingsConstants.LOCK_ALBUM_NAME)
                        settingsDataStoreImpl.deleteString(SettingsConstants.CURRENT_LOCK_WALLPAPER)
                        enableChanger = false
                    }
                    if (event.albumName == _state.value.homeAlbumName) {
                        settingsDataStoreImpl.deleteString(SettingsConstants.HOME_ALBUM_NAME)
                        settingsDataStoreImpl.deleteString(SettingsConstants.CURRENT_HOME_WALLPAPER)
                        enableChanger = false
                    }
                    settingsDataStoreImpl.putBoolean(SettingsConstants.ENABLE_CHANGER, enableChanger)
                    _state.update {
                        it.copy(
                            homeAlbumName = if (event.albumName == _state.value.homeAlbumName) null else it.homeAlbumName,
                            lockAlbumName = if (event.albumName == _state.value.lockAlbumName) null else it.lockAlbumName,
                            currentHomeWallpaper = if (event.albumName == _state.value.homeAlbumName) null else it.currentHomeWallpaper,
                            currentLockWallpaper = if (event.albumName == _state.value.lockAlbumName) null else it.currentLockWallpaper,
                            enableChanger = enableChanger
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
                            nextHomeWallpaper = event.nextHomeWallpaper
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
                            nextLockWallpaper = event.nextLockWallpaper
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
                            nextHomeWallpaper = event.nextHomeWallpaper ?: it.nextHomeWallpaper,
                            nextLockWallpaper = event.nextLockWallpaper ?: it.nextLockWallpaper
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
                            nextHomeWallpaper = nextHomeWallpaper.await(),
                            nextLockWallpaper = nextLockWallpaper.await(),
                            currentHomeWallpaper = nextHomeWallpaper.await(),
                            currentLockWallpaper = nextLockWallpaper.await()
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
                    settingsDataStoreImpl.deleteInt(SettingsConstants.HOME_DARKEN_PERCENTAGE)
                    settingsDataStoreImpl.deleteInt(SettingsConstants.LOCK_DARKEN_PERCENTAGE)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.DARKEN)
                    settingsDataStoreImpl.deleteString(SettingsConstants.WALLPAPER_SCALING)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.ENABLE_HOME_WALLPAPER)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.ENABLE_LOCK_WALLPAPER)
                    settingsDataStoreImpl.deleteString(SettingsConstants.CURRENT_HOME_WALLPAPER)
                    settingsDataStoreImpl.deleteString(SettingsConstants.CURRENT_LOCK_WALLPAPER)
                    settingsDataStoreImpl.deleteString(SettingsConstants.HOME_ALBUM_NAME)
                    settingsDataStoreImpl.deleteString(SettingsConstants.LOCK_ALBUM_NAME)
                    settingsDataStoreImpl.deleteInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL)
                    settingsDataStoreImpl.deleteInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.SCHEDULE_SEPARATELY)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.BLUR)
                    settingsDataStoreImpl.deleteInt(SettingsConstants.HOME_BLUR_PERCENTAGE)
                    settingsDataStoreImpl.deleteInt(SettingsConstants.LOCK_BLUR_PERCENTAGE)
                    settingsDataStoreImpl.deleteBoolean(SettingsConstants.FIRST_SET)
                    settingsDataStoreImpl.deleteString(SettingsConstants.HOME_NEXT_SET_TIME)
                    settingsDataStoreImpl.deleteString(SettingsConstants.LOCK_NEXT_SET_TIME)
                    settingsDataStoreImpl.deleteString(SettingsConstants.NEXT_HOME_WALLPAPER)
                    settingsDataStoreImpl.deleteString(SettingsConstants.NEXT_LOCK_WALLPAPER)

                    _state.update {
                        it.copy(
                            darkMode = null,
                            amoledTheme = false,
                            dynamicTheming = false,
                            firstLaunch = true,
                            lastSetTime = null,
                            nextSetTime = null,
                            animate = true,
                            enableChanger = false,
                            homeDarkenPercentage = 100,
                            lockDarkenPercentage = 100,
                            homeBlurPercentage = 0,
                            lockBlurPercentage = 0,
                            darken = false,
                            wallpaperScaling = true,
                            wallpaperScalingMode = ScalingConstants.FILL,
                            setHomeWallpaper = false,
                            setLockWallpaper = false,
                            currentHomeWallpaper = null,
                            currentLockWallpaper = null,
                            lockInterval = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT,
                            homeInterval = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT,
                            homeAlbumName = null,
                            lockAlbumName = null,
                            scheduleSeparately = false,
                            blur = false,
                            nextHomeWallpaper = null,
                            nextLockWallpaper = null
                        )
                    }
                }
            }
        }
    }
}
