package com.anthonyla.paperize.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.constants.PreferenceKeys
import com.anthonyla.paperize.domain.model.AppSettings
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.WallpaperEffects
import com.anthonyla.paperize.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.PREFERENCES_NAME
)

private val DARK_MODE = booleanPreferencesKey(PreferenceKeys.DARK_MODE)
private val DYNAMIC_THEMING = booleanPreferencesKey(PreferenceKeys.DYNAMIC_THEMING)
private val ANIMATE = booleanPreferencesKey(PreferenceKeys.ANIMATE)
private val FIRST_LAUNCH = booleanPreferencesKey(PreferenceKeys.FIRST_LAUNCH)
private val WALLPAPER_MODE = stringPreferencesKey(PreferenceKeys.WALLPAPER_MODE)
private val ENABLE_CHANGER = booleanPreferencesKey(PreferenceKeys.ENABLE_CHANGER)
private val SEPARATE_SCHEDULES = booleanPreferencesKey(PreferenceKeys.SEPARATE_SCHEDULES)
private val SHUFFLE_ENABLED = booleanPreferencesKey(PreferenceKeys.SHUFFLE_ENABLED)
private val HOME_ENABLED = booleanPreferencesKey(PreferenceKeys.HOME_ENABLED)
private val LOCK_ENABLED = booleanPreferencesKey(PreferenceKeys.LOCK_ENABLED)
private val HOME_ALBUM_ID = stringPreferencesKey(PreferenceKeys.HOME_ALBUM_ID)
private val LOCK_ALBUM_ID = stringPreferencesKey(PreferenceKeys.LOCK_ALBUM_ID)
private val LIVE_ALBUM_ID = stringPreferencesKey(PreferenceKeys.LIVE_ALBUM_ID)
private val HOME_INTERVAL_MINUTES = intPreferencesKey(PreferenceKeys.HOME_INTERVAL_MINUTES)
private val LOCK_INTERVAL_MINUTES = intPreferencesKey(PreferenceKeys.LOCK_INTERVAL_MINUTES)
private val LIVE_INTERVAL_MINUTES = intPreferencesKey(PreferenceKeys.LIVE_INTERVAL_MINUTES)
private val HOME_SCALING_TYPE = stringPreferencesKey(PreferenceKeys.HOME_SCALING_TYPE)
private val LOCK_SCALING_TYPE = stringPreferencesKey(PreferenceKeys.LOCK_SCALING_TYPE)
private val LIVE_SCALING_TYPE = stringPreferencesKey(PreferenceKeys.LIVE_SCALING_TYPE)
private val HOME_SCROLLING_ENABLED = booleanPreferencesKey(PreferenceKeys.HOME_SCROLLING_ENABLED)
private val ADAPTIVE_BRIGHTNESS = booleanPreferencesKey(PreferenceKeys.ADAPTIVE_BRIGHTNESS)

/** Effect keys are "<screen>_<field>", e.g. "home_enable_blur" or "live_parallax_intensity". */
private class EffectKeys(screen: String) {
    val enableBlur = booleanPreferencesKey("${screen}_enable_blur")
    val blur = intPreferencesKey("${screen}_blur")
    val enableDarken = booleanPreferencesKey("${screen}_enable_darken")
    val darken = intPreferencesKey("${screen}_darken")
    val enableVignette = booleanPreferencesKey("${screen}_enable_vignette")
    val vignette = intPreferencesKey("${screen}_vignette")
    val enableGrayscale = booleanPreferencesKey("${screen}_enable_grayscale")
    val grayscale = intPreferencesKey("${screen}_grayscale")
    val enableDoubleTap = booleanPreferencesKey("${screen}_enable_double_tap")
    val enableChangeOnScreenOff = booleanPreferencesKey("${screen}_enable_change_on_screen_off")
    val enableParallax = booleanPreferencesKey("${screen}_enable_parallax")
    val parallaxIntensity = intPreferencesKey("${screen}_parallax_intensity")
}

private val HOME_EFFECTS = EffectKeys("home")
private val LOCK_EFFECTS = EffectKeys("lock")
private val LIVE_EFFECTS = EffectKeys("live")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {
    private val dataStore = context.dataStore

    override suspend fun getAppSettings(): AppSettings = getAppSettingsFlow().first()

    override fun getAppSettingsFlow(): Flow<AppSettings> = dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            darkMode = prefs[DARK_MODE],
            dynamicTheming = prefs[DYNAMIC_THEMING] ?: defaults.dynamicTheming,
            animate = prefs[ANIMATE] ?: defaults.animate,
            firstLaunch = prefs[FIRST_LAUNCH] ?: defaults.firstLaunch
        )
    }

    override suspend fun getWallpaperMode(): WallpaperMode = getWallpaperModeFlow().first()

    override fun getWallpaperModeFlow(): Flow<WallpaperMode> =
        dataStore.data.map { WallpaperMode.fromString(it[WALLPAPER_MODE]) }

    override suspend fun updateWallpaperMode(mode: WallpaperMode) {
        dataStore.edit { it[WALLPAPER_MODE] = mode.name }
    }

    override suspend fun getScheduleSettings(): ScheduleSettings = getScheduleSettingsFlow().first()

    override fun getScheduleSettingsFlow(): Flow<ScheduleSettings> = dataStore.data.map(::scheduleSettings)

    override suspend fun updateScheduleSettings(settings: ScheduleSettings) {
        updateScheduleSettings { settings }
    }

    override suspend fun updateScheduleSettings(transform: (ScheduleSettings) -> ScheduleSettings): ScheduleSettings {
        val updated = dataStore.edit { prefs ->
            val settings = transform(scheduleSettings(prefs))
            prefs[ENABLE_CHANGER] = settings.enableChanger
            prefs[SEPARATE_SCHEDULES] = settings.separateSchedules
            prefs[SHUFFLE_ENABLED] = settings.shuffleEnabled
            prefs[HOME_ENABLED] = settings.homeEnabled
            prefs[LOCK_ENABLED] = settings.lockEnabled
            prefs.setOrRemove(HOME_ALBUM_ID, settings.homeAlbumId)
            prefs.setOrRemove(LOCK_ALBUM_ID, settings.lockAlbumId)
            prefs.setOrRemove(LIVE_ALBUM_ID, settings.liveAlbumId)
            prefs[HOME_INTERVAL_MINUTES] = settings.homeIntervalMinutes
            prefs[LOCK_INTERVAL_MINUTES] = settings.lockIntervalMinutes
            prefs[LIVE_INTERVAL_MINUTES] = settings.liveIntervalMinutes
            prefs[HOME_SCALING_TYPE] = settings.homeScalingType.name
            prefs[LOCK_SCALING_TYPE] = settings.lockScalingType.name
            prefs[LIVE_SCALING_TYPE] = settings.liveScalingType.name
            prefs[HOME_SCROLLING_ENABLED] = settings.homeScrollingEnabled
            prefs.writeEffects(HOME_EFFECTS, settings.homeEffects)
            prefs.writeEffects(LOCK_EFFECTS, settings.lockEffects)
            prefs.writeEffects(LIVE_EFFECTS, settings.liveEffects)
            prefs[ADAPTIVE_BRIGHTNESS] = settings.adaptiveBrightness
        }
        return scheduleSettings(updated)
    }

    override suspend fun clearAllSettings() {
        dataStore.edit { it.clear() }
    }

    override suspend fun clearScheduleSettings() = updateScheduleSettings(ScheduleSettings())

    override suspend fun updateAlbumId(screen: ScreenType, albumId: String?) {
        dataStore.edit { prefs -> albumIdKeys(screen).forEach { prefs.setOrRemove(it, albumId) } }
    }

    override suspend fun clearAlbumSelectionsIfMatches(albumId: String): Boolean {
        var wasCleared = false
        dataStore.edit { prefs ->
            val targets = listOf(HOME_ALBUM_ID, LOCK_ALBUM_ID, LIVE_ALBUM_ID).filter { prefs[it] == albumId }
            targets.forEach { prefs.remove(it) }
            wasCleared = targets.isNotEmpty()
        }
        return wasCleared
    }

    override suspend fun clearEmptyAlbumSelection(albumId: String, screen: ScreenType) {
        dataStore.edit { prefs ->
            val targets = albumIdKeys(screen).filter { prefs[it] == albumId }
            if (targets.isEmpty()) return@edit
            targets.forEach { prefs.remove(it) }
            val active = if (WallpaperMode.fromString(prefs[WALLPAPER_MODE]) == WallpaperMode.LIVE) {
                prefs[LIVE_ALBUM_ID] != null
            } else {
                (prefs[HOME_ENABLED] == true && prefs[HOME_ALBUM_ID] != null) ||
                    (prefs[LOCK_ENABLED] == true && prefs[LOCK_ALBUM_ID] != null)
            }
            if (!active) prefs[ENABLE_CHANGER] = false
        }
    }

    override suspend fun updateDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE] = enabled }
    }

    override suspend fun updateDynamicTheming(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_THEMING] = enabled }
    }

    override suspend fun updateAnimate(enabled: Boolean) {
        dataStore.edit { it[ANIMATE] = enabled }
    }

    override suspend fun updateFirstLaunch(isFirstLaunch: Boolean) {
        dataStore.edit { it[FIRST_LAUNCH] = isFirstLaunch }
    }

    override suspend fun updateEnableChanger(enabled: Boolean) {
        dataStore.edit { it[ENABLE_CHANGER] = enabled }
    }

    private fun scheduleSettings(prefs: Preferences): ScheduleSettings {
        val defaults = ScheduleSettings()
        return ScheduleSettings(
            enableChanger = prefs[ENABLE_CHANGER] ?: defaults.enableChanger,
            separateSchedules = prefs[SEPARATE_SCHEDULES] ?: defaults.separateSchedules,
            shuffleEnabled = prefs[SHUFFLE_ENABLED] ?: defaults.shuffleEnabled,
            homeEnabled = prefs[HOME_ENABLED] ?: defaults.homeEnabled,
            lockEnabled = prefs[LOCK_ENABLED] ?: defaults.lockEnabled,
            homeAlbumId = prefs[HOME_ALBUM_ID],
            lockAlbumId = prefs[LOCK_ALBUM_ID],
            liveAlbumId = prefs[LIVE_ALBUM_ID],
            homeIntervalMinutes = prefs[HOME_INTERVAL_MINUTES] ?: defaults.homeIntervalMinutes,
            lockIntervalMinutes = prefs[LOCK_INTERVAL_MINUTES] ?: defaults.lockIntervalMinutes,
            liveIntervalMinutes = prefs[LIVE_INTERVAL_MINUTES] ?: defaults.liveIntervalMinutes,
            homeScalingType = ScalingType.fromString(prefs[HOME_SCALING_TYPE]),
            lockScalingType = ScalingType.fromString(prefs[LOCK_SCALING_TYPE]),
            liveScalingType = ScalingType.fromString(prefs[LIVE_SCALING_TYPE]),
            homeScrollingEnabled = prefs[HOME_SCROLLING_ENABLED] ?: defaults.homeScrollingEnabled,
            homeEffects = prefs.readEffects(HOME_EFFECTS),
            lockEffects = prefs.readEffects(LOCK_EFFECTS),
            liveEffects = prefs.readEffects(LIVE_EFFECTS),
            adaptiveBrightness = prefs[ADAPTIVE_BRIGHTNESS] ?: defaults.adaptiveBrightness
        )
    }

    private fun Preferences.readEffects(keys: EffectKeys): WallpaperEffects {
        val defaults = WallpaperEffects()
        return WallpaperEffects(
            enableBlur = this[keys.enableBlur] ?: defaults.enableBlur,
            blurPercentage = this[keys.blur] ?: defaults.blurPercentage,
            enableDarken = this[keys.enableDarken] ?: defaults.enableDarken,
            darkenPercentage = this[keys.darken] ?: defaults.darkenPercentage,
            enableVignette = this[keys.enableVignette] ?: defaults.enableVignette,
            vignettePercentage = this[keys.vignette] ?: defaults.vignettePercentage,
            enableGrayscale = this[keys.enableGrayscale] ?: defaults.enableGrayscale,
            grayscalePercentage = this[keys.grayscale] ?: defaults.grayscalePercentage,
            enableDoubleTap = this[keys.enableDoubleTap] ?: defaults.enableDoubleTap,
            enableChangeOnScreenOff = this[keys.enableChangeOnScreenOff] ?: defaults.enableChangeOnScreenOff,
            enableParallax = this[keys.enableParallax] ?: defaults.enableParallax,
            parallaxIntensity = this[keys.parallaxIntensity] ?: defaults.parallaxIntensity
        )
    }

    private fun MutablePreferences.writeEffects(keys: EffectKeys, effects: WallpaperEffects) {
        this[keys.enableBlur] = effects.enableBlur
        this[keys.blur] = effects.blurPercentage
        this[keys.enableDarken] = effects.enableDarken
        this[keys.darken] = effects.darkenPercentage
        this[keys.enableVignette] = effects.enableVignette
        this[keys.vignette] = effects.vignettePercentage
        this[keys.enableGrayscale] = effects.enableGrayscale
        this[keys.grayscale] = effects.grayscalePercentage
        this[keys.enableDoubleTap] = effects.enableDoubleTap
        this[keys.enableChangeOnScreenOff] = effects.enableChangeOnScreenOff
        this[keys.enableParallax] = effects.enableParallax
        this[keys.parallaxIntensity] = effects.parallaxIntensity
    }

    private fun MutablePreferences.setOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value != null) this[key] = value else remove(key)
    }

    private fun albumIdKeys(screen: ScreenType) = when (screen) {
        ScreenType.HOME -> listOf(HOME_ALBUM_ID)
        ScreenType.LOCK -> listOf(LOCK_ALBUM_ID)
        ScreenType.BOTH -> listOf(HOME_ALBUM_ID, LOCK_ALBUM_ID)
        ScreenType.LIVE -> listOf(LIVE_ALBUM_ID)
    }
}
