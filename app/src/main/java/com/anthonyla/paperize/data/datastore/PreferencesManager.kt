package com.anthonyla.paperize.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.constants.PreferenceKeys
import com.anthonyla.paperize.domain.model.AppSettings
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.WallpaperEffects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Context extension for DataStore
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.PREFERENCES_NAME
)

/**
 * PreferencesManager - Clean interface for DataStore operations
 *
 * Replaces the old SettingsDataStore with better organization and type safety
 */
@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore

    // ============ App Settings ============

    suspend fun getAppSettings(): AppSettings {
        val prefs = dataStore.data.first()
        return AppSettings(
            darkMode = prefs[booleanPreferencesKey(PreferenceKeys.DARK_MODE)] ?: false,
            amoledTheme = prefs[booleanPreferencesKey(PreferenceKeys.AMOLED_THEME)] ?: false,
            dynamicTheming = prefs[booleanPreferencesKey(PreferenceKeys.DYNAMIC_THEMING)] ?: true,
            animate = prefs[booleanPreferencesKey(PreferenceKeys.ANIMATE)] ?: true,
            firstLaunch = prefs[booleanPreferencesKey(PreferenceKeys.FIRST_LAUNCH)] ?: true,
            currentHomeWallpaperId = prefs[stringPreferencesKey(PreferenceKeys.CURRENT_HOME_WALLPAPER_ID)],
            currentLockWallpaperId = prefs[stringPreferencesKey(PreferenceKeys.CURRENT_LOCK_WALLPAPER_ID)]
        )
    }

    fun getAppSettingsFlow(): Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            darkMode = prefs[booleanPreferencesKey(PreferenceKeys.DARK_MODE)] ?: false,
            amoledTheme = prefs[booleanPreferencesKey(PreferenceKeys.AMOLED_THEME)] ?: false,
            dynamicTheming = prefs[booleanPreferencesKey(PreferenceKeys.DYNAMIC_THEMING)] ?: true,
            animate = prefs[booleanPreferencesKey(PreferenceKeys.ANIMATE)] ?: true,
            firstLaunch = prefs[booleanPreferencesKey(PreferenceKeys.FIRST_LAUNCH)] ?: true,
            currentHomeWallpaperId = prefs[stringPreferencesKey(PreferenceKeys.CURRENT_HOME_WALLPAPER_ID)],
            currentLockWallpaperId = prefs[stringPreferencesKey(PreferenceKeys.CURRENT_LOCK_WALLPAPER_ID)]
        )
    }

    suspend fun updateAppSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(PreferenceKeys.DARK_MODE)] = settings.darkMode
            prefs[booleanPreferencesKey(PreferenceKeys.AMOLED_THEME)] = settings.amoledTheme
            prefs[booleanPreferencesKey(PreferenceKeys.DYNAMIC_THEMING)] = settings.dynamicTheming
            prefs[booleanPreferencesKey(PreferenceKeys.ANIMATE)] = settings.animate
            prefs[booleanPreferencesKey(PreferenceKeys.FIRST_LAUNCH)] = settings.firstLaunch
            settings.currentHomeWallpaperId?.let {
                prefs[stringPreferencesKey(PreferenceKeys.CURRENT_HOME_WALLPAPER_ID)] = it
            }
            settings.currentLockWallpaperId?.let {
                prefs[stringPreferencesKey(PreferenceKeys.CURRENT_LOCK_WALLPAPER_ID)] = it
            }
        }
    }

    // ============ Schedule Settings ============

    suspend fun getScheduleSettings(): ScheduleSettings {
        val prefs = dataStore.data.first()
        return ScheduleSettings(
            enableChanger = prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_CHANGER)] ?: false,
            separateSchedules = prefs[booleanPreferencesKey(PreferenceKeys.SEPARATE_SCHEDULES)] ?: false,
            shuffleEnabled = prefs[booleanPreferencesKey(PreferenceKeys.SHUFFLE_ENABLED)] ?: false,
            homeIntervalMinutes = prefs[intPreferencesKey(PreferenceKeys.HOME_INTERVAL_MINUTES)]
                ?: Constants.DEFAULT_INTERVAL_MINUTES,
            lockIntervalMinutes = prefs[intPreferencesKey(PreferenceKeys.LOCK_INTERVAL_MINUTES)]
                ?: Constants.DEFAULT_INTERVAL_MINUTES,
            scheduleStartTime = prefs[stringPreferencesKey(PreferenceKeys.SCHEDULE_START_TIME)],
            useStartTime = prefs[booleanPreferencesKey(PreferenceKeys.USE_START_TIME)] ?: false,
            homeNextChangeTime = prefs[longPreferencesKey(PreferenceKeys.HOME_NEXT_CHANGE_TIME)],
            lockNextChangeTime = prefs[longPreferencesKey(PreferenceKeys.LOCK_NEXT_CHANGE_TIME)],
            skipLandscape = prefs[booleanPreferencesKey(PreferenceKeys.SKIP_LANDSCAPE)] ?: false,
            skipNonInteractive = prefs[booleanPreferencesKey(PreferenceKeys.SKIP_NON_INTERACTIVE)] ?: false,
            homeScalingType = ScalingType.fromString(prefs[stringPreferencesKey(PreferenceKeys.HOME_SCALING_TYPE)]),
            lockScalingType = ScalingType.fromString(prefs[stringPreferencesKey(PreferenceKeys.LOCK_SCALING_TYPE)]),
            homeEffects = WallpaperEffects(
                darkenPercentage = prefs[intPreferencesKey(PreferenceKeys.HOME_DARKEN)] ?: 0,
                blurPercentage = prefs[intPreferencesKey(PreferenceKeys.HOME_BLUR)] ?: 0,
                vignettePercentage = prefs[intPreferencesKey(PreferenceKeys.HOME_VIGNETTE)] ?: 0,
                enableGrayscale = prefs[booleanPreferencesKey(PreferenceKeys.HOME_GRAYSCALE)] ?: false
            ),
            lockEffects = WallpaperEffects(
                darkenPercentage = prefs[intPreferencesKey(PreferenceKeys.LOCK_DARKEN)] ?: 0,
                blurPercentage = prefs[intPreferencesKey(PreferenceKeys.LOCK_BLUR)] ?: 0,
                vignettePercentage = prefs[intPreferencesKey(PreferenceKeys.LOCK_VIGNETTE)] ?: 0,
                enableGrayscale = prefs[booleanPreferencesKey(PreferenceKeys.LOCK_GRAYSCALE)] ?: false
            )
        )
    }

    fun getScheduleSettingsFlow(): Flow<ScheduleSettings> = dataStore.data.map { prefs ->
        ScheduleSettings(
            enableChanger = prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_CHANGER)] ?: false,
            separateSchedules = prefs[booleanPreferencesKey(PreferenceKeys.SEPARATE_SCHEDULES)] ?: false,
            shuffleEnabled = prefs[booleanPreferencesKey(PreferenceKeys.SHUFFLE_ENABLED)] ?: false,
            homeIntervalMinutes = prefs[intPreferencesKey(PreferenceKeys.HOME_INTERVAL_MINUTES)]
                ?: Constants.DEFAULT_INTERVAL_MINUTES,
            lockIntervalMinutes = prefs[intPreferencesKey(PreferenceKeys.LOCK_INTERVAL_MINUTES)]
                ?: Constants.DEFAULT_INTERVAL_MINUTES,
            scheduleStartTime = prefs[stringPreferencesKey(PreferenceKeys.SCHEDULE_START_TIME)],
            useStartTime = prefs[booleanPreferencesKey(PreferenceKeys.USE_START_TIME)] ?: false,
            homeNextChangeTime = prefs[longPreferencesKey(PreferenceKeys.HOME_NEXT_CHANGE_TIME)],
            lockNextChangeTime = prefs[longPreferencesKey(PreferenceKeys.LOCK_NEXT_CHANGE_TIME)],
            skipLandscape = prefs[booleanPreferencesKey(PreferenceKeys.SKIP_LANDSCAPE)] ?: false,
            skipNonInteractive = prefs[booleanPreferencesKey(PreferenceKeys.SKIP_NON_INTERACTIVE)] ?: false,
            homeScalingType = ScalingType.fromString(prefs[stringPreferencesKey(PreferenceKeys.HOME_SCALING_TYPE)]),
            lockScalingType = ScalingType.fromString(prefs[stringPreferencesKey(PreferenceKeys.LOCK_SCALING_TYPE)]),
            homeEffects = WallpaperEffects(
                darkenPercentage = prefs[intPreferencesKey(PreferenceKeys.HOME_DARKEN)] ?: 0,
                blurPercentage = prefs[intPreferencesKey(PreferenceKeys.HOME_BLUR)] ?: 0,
                vignettePercentage = prefs[intPreferencesKey(PreferenceKeys.HOME_VIGNETTE)] ?: 0,
                enableGrayscale = prefs[booleanPreferencesKey(PreferenceKeys.HOME_GRAYSCALE)] ?: false
            ),
            lockEffects = WallpaperEffects(
                darkenPercentage = prefs[intPreferencesKey(PreferenceKeys.LOCK_DARKEN)] ?: 0,
                blurPercentage = prefs[intPreferencesKey(PreferenceKeys.LOCK_BLUR)] ?: 0,
                vignettePercentage = prefs[intPreferencesKey(PreferenceKeys.LOCK_VIGNETTE)] ?: 0,
                enableGrayscale = prefs[booleanPreferencesKey(PreferenceKeys.LOCK_GRAYSCALE)] ?: false
            )
        )
    }

    suspend fun updateScheduleSettings(settings: ScheduleSettings) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_CHANGER)] = settings.enableChanger
            prefs[booleanPreferencesKey(PreferenceKeys.SEPARATE_SCHEDULES)] = settings.separateSchedules
            prefs[booleanPreferencesKey(PreferenceKeys.SHUFFLE_ENABLED)] = settings.shuffleEnabled
            prefs[intPreferencesKey(PreferenceKeys.HOME_INTERVAL_MINUTES)] = settings.homeIntervalMinutes
            prefs[intPreferencesKey(PreferenceKeys.LOCK_INTERVAL_MINUTES)] = settings.lockIntervalMinutes
            settings.scheduleStartTime?.let {
                prefs[stringPreferencesKey(PreferenceKeys.SCHEDULE_START_TIME)] = it
            }
            prefs[booleanPreferencesKey(PreferenceKeys.USE_START_TIME)] = settings.useStartTime
            settings.homeNextChangeTime?.let {
                prefs[longPreferencesKey(PreferenceKeys.HOME_NEXT_CHANGE_TIME)] = it
            }
            settings.lockNextChangeTime?.let {
                prefs[longPreferencesKey(PreferenceKeys.LOCK_NEXT_CHANGE_TIME)] = it
            }
            prefs[booleanPreferencesKey(PreferenceKeys.SKIP_LANDSCAPE)] = settings.skipLandscape
            prefs[booleanPreferencesKey(PreferenceKeys.SKIP_NON_INTERACTIVE)] = settings.skipNonInteractive
            prefs[stringPreferencesKey(PreferenceKeys.HOME_SCALING_TYPE)] = settings.homeScalingType.name
            prefs[stringPreferencesKey(PreferenceKeys.LOCK_SCALING_TYPE)] = settings.lockScalingType.name

            // Home effects
            prefs[intPreferencesKey(PreferenceKeys.HOME_DARKEN)] = settings.homeEffects.darkenPercentage
            prefs[intPreferencesKey(PreferenceKeys.HOME_BLUR)] = settings.homeEffects.blurPercentage
            prefs[intPreferencesKey(PreferenceKeys.HOME_VIGNETTE)] = settings.homeEffects.vignettePercentage
            prefs[booleanPreferencesKey(PreferenceKeys.HOME_GRAYSCALE)] = settings.homeEffects.enableGrayscale

            // Lock effects
            prefs[intPreferencesKey(PreferenceKeys.LOCK_DARKEN)] = settings.lockEffects.darkenPercentage
            prefs[intPreferencesKey(PreferenceKeys.LOCK_BLUR)] = settings.lockEffects.blurPercentage
            prefs[intPreferencesKey(PreferenceKeys.LOCK_VIGNETTE)] = settings.lockEffects.vignettePercentage
            prefs[booleanPreferencesKey(PreferenceKeys.LOCK_GRAYSCALE)] = settings.lockEffects.enableGrayscale
        }
    }

    // ============ Individual Preference Operations ============

    suspend fun <T> setValue(key: String, value: T) {
        dataStore.edit { prefs ->
            when (value) {
                is Boolean -> prefs[booleanPreferencesKey(key)] = value
                is Int -> prefs[intPreferencesKey(key)] = value
                is Long -> prefs[longPreferencesKey(key)] = value
                is String -> prefs[stringPreferencesKey(key)] = value
                else -> throw IllegalArgumentException("Unsupported preference type")
            }
        }
    }

    suspend fun <T> getValue(key: String, defaultValue: T): T {
        val prefs = dataStore.data.first()
        @Suppress("UNCHECKED_CAST")
        return when (defaultValue) {
            is Boolean -> prefs[booleanPreferencesKey(key)] ?: defaultValue
            is Int -> prefs[intPreferencesKey(key)] ?: defaultValue
            is Long -> prefs[longPreferencesKey(key)] ?: defaultValue
            is String -> prefs[stringPreferencesKey(key)] ?: defaultValue
            else -> throw IllegalArgumentException("Unsupported preference type")
        } as T
    }

    fun <T> getValueFlow(key: String, defaultValue: T): Flow<T> = dataStore.data.map { prefs ->
        @Suppress("UNCHECKED_CAST")
        when (defaultValue) {
            is Boolean -> prefs[booleanPreferencesKey(key)] ?: defaultValue
            is Int -> prefs[intPreferencesKey(key)] ?: defaultValue
            is Long -> prefs[longPreferencesKey(key)] ?: defaultValue
            is String -> prefs[stringPreferencesKey(key)] ?: defaultValue
            else -> throw IllegalArgumentException("Unsupported preference type")
        } as T
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    // ============ Wallpaper Effects (Unified) ============

    suspend fun getWallpaperEffects(): WallpaperEffects {
        val prefs = dataStore.data.first()
        return WallpaperEffects(
            enableBlur = prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_BLUR)] ?: false,
            blurPercentage = prefs[intPreferencesKey(PreferenceKeys.BLUR_PERCENTAGE)] ?: 0,
            enableDarken = prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_DARKEN)] ?: false,
            darkenPercentage = prefs[intPreferencesKey(PreferenceKeys.DARKEN_PERCENTAGE)] ?: 0,
            enableVignette = prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_VIGNETTE)] ?: false,
            vignettePercentage = prefs[intPreferencesKey(PreferenceKeys.VIGNETTE_PERCENTAGE)] ?: 0,
            enableGrayscale = prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_GRAYSCALE)] ?: false,
            shuffle = prefs[booleanPreferencesKey(PreferenceKeys.SHUFFLE_ENABLED)] ?: false,
            skipLandscape = prefs[booleanPreferencesKey(PreferenceKeys.SKIP_LANDSCAPE)] ?: false,
            skipNonInteractive = prefs[booleanPreferencesKey(PreferenceKeys.SKIP_NON_INTERACTIVE)] ?: false
        )
    }

    fun getWallpaperEffectsFlow(): Flow<WallpaperEffects> = dataStore.data.map { prefs ->
        WallpaperEffects(
            enableBlur = prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_BLUR)] ?: false,
            blurPercentage = prefs[intPreferencesKey(PreferenceKeys.BLUR_PERCENTAGE)] ?: 0,
            enableDarken = prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_DARKEN)] ?: false,
            darkenPercentage = prefs[intPreferencesKey(PreferenceKeys.DARKEN_PERCENTAGE)] ?: 0,
            enableVignette = prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_VIGNETTE)] ?: false,
            vignettePercentage = prefs[intPreferencesKey(PreferenceKeys.VIGNETTE_PERCENTAGE)] ?: 0,
            enableGrayscale = prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_GRAYSCALE)] ?: false,
            shuffle = prefs[booleanPreferencesKey(PreferenceKeys.SHUFFLE_ENABLED)] ?: false,
            skipLandscape = prefs[booleanPreferencesKey(PreferenceKeys.SKIP_LANDSCAPE)] ?: false,
            skipNonInteractive = prefs[booleanPreferencesKey(PreferenceKeys.SKIP_NON_INTERACTIVE)] ?: false
        )
    }

    suspend fun updateWallpaperEffects(effects: WallpaperEffects) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_BLUR)] = effects.enableBlur
            prefs[intPreferencesKey(PreferenceKeys.BLUR_PERCENTAGE)] = effects.blurPercentage
            prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_DARKEN)] = effects.enableDarken
            prefs[intPreferencesKey(PreferenceKeys.DARKEN_PERCENTAGE)] = effects.darkenPercentage
            prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_VIGNETTE)] = effects.enableVignette
            prefs[intPreferencesKey(PreferenceKeys.VIGNETTE_PERCENTAGE)] = effects.vignettePercentage
            prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_GRAYSCALE)] = effects.enableGrayscale
            prefs[booleanPreferencesKey(PreferenceKeys.SHUFFLE_ENABLED)] = effects.shuffle
            prefs[booleanPreferencesKey(PreferenceKeys.SKIP_LANDSCAPE)] = effects.skipLandscape
            prefs[booleanPreferencesKey(PreferenceKeys.SKIP_NON_INTERACTIVE)] = effects.skipNonInteractive
        }
    }
}
