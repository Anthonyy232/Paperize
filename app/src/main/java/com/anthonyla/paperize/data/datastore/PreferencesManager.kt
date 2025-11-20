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
            prefs[booleanPreferencesKey(PreferenceKeys.DARK_MODE)] = settings.darkMode ?: false
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
            homeEnabled = prefs[booleanPreferencesKey(PreferenceKeys.HOME_ENABLED)] ?: true,
            lockEnabled = prefs[booleanPreferencesKey(PreferenceKeys.LOCK_ENABLED)] ?: true,
            homeAlbumId = prefs[stringPreferencesKey(PreferenceKeys.HOME_ALBUM_ID)],
            lockAlbumId = prefs[stringPreferencesKey(PreferenceKeys.LOCK_ALBUM_ID)],
            homeIntervalMinutes = prefs[intPreferencesKey(PreferenceKeys.HOME_INTERVAL_MINUTES)]
                ?: Constants.DEFAULT_INTERVAL_MINUTES,
            lockIntervalMinutes = prefs[intPreferencesKey(PreferenceKeys.LOCK_INTERVAL_MINUTES)]
                ?: Constants.DEFAULT_INTERVAL_MINUTES,
            homeScalingType = ScalingType.fromString(prefs[stringPreferencesKey(PreferenceKeys.HOME_SCALING_TYPE)]),
            lockScalingType = ScalingType.fromString(prefs[stringPreferencesKey(PreferenceKeys.LOCK_SCALING_TYPE)]),
            homeEffects = WallpaperEffects(
                enableBlur = prefs[booleanPreferencesKey(PreferenceKeys.HOME_ENABLE_BLUR)] ?: false,
                blurPercentage = prefs[intPreferencesKey(PreferenceKeys.HOME_BLUR)] ?: 0,
                enableDarken = prefs[booleanPreferencesKey(PreferenceKeys.HOME_ENABLE_DARKEN)] ?: false,
                darkenPercentage = prefs[intPreferencesKey(PreferenceKeys.HOME_DARKEN)] ?: 0,
                enableVignette = prefs[booleanPreferencesKey(PreferenceKeys.HOME_ENABLE_VIGNETTE)] ?: false,
                vignettePercentage = prefs[intPreferencesKey(PreferenceKeys.HOME_VIGNETTE)] ?: 0,
                enableGrayscale = prefs[booleanPreferencesKey(PreferenceKeys.HOME_GRAYSCALE)] ?: false
            ),
            lockEffects = WallpaperEffects(
                enableBlur = prefs[booleanPreferencesKey(PreferenceKeys.LOCK_ENABLE_BLUR)] ?: false,
                blurPercentage = prefs[intPreferencesKey(PreferenceKeys.LOCK_BLUR)] ?: 0,
                enableDarken = prefs[booleanPreferencesKey(PreferenceKeys.LOCK_ENABLE_DARKEN)] ?: false,
                darkenPercentage = prefs[intPreferencesKey(PreferenceKeys.LOCK_DARKEN)] ?: 0,
                enableVignette = prefs[booleanPreferencesKey(PreferenceKeys.LOCK_ENABLE_VIGNETTE)] ?: false,
                vignettePercentage = prefs[intPreferencesKey(PreferenceKeys.LOCK_VIGNETTE)] ?: 0,
                enableGrayscale = prefs[booleanPreferencesKey(PreferenceKeys.LOCK_GRAYSCALE)] ?: false
            ),
            adaptiveBrightness = prefs[booleanPreferencesKey(PreferenceKeys.ADAPTIVE_BRIGHTNESS)] ?: false
        )
    }

    fun getScheduleSettingsFlow(): Flow<ScheduleSettings> = dataStore.data.map { prefs ->
        ScheduleSettings(
            enableChanger = prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_CHANGER)] ?: false,
            separateSchedules = prefs[booleanPreferencesKey(PreferenceKeys.SEPARATE_SCHEDULES)] ?: false,
            shuffleEnabled = prefs[booleanPreferencesKey(PreferenceKeys.SHUFFLE_ENABLED)] ?: false,
            homeEnabled = prefs[booleanPreferencesKey(PreferenceKeys.HOME_ENABLED)] ?: true,
            lockEnabled = prefs[booleanPreferencesKey(PreferenceKeys.LOCK_ENABLED)] ?: true,
            homeAlbumId = prefs[stringPreferencesKey(PreferenceKeys.HOME_ALBUM_ID)],
            lockAlbumId = prefs[stringPreferencesKey(PreferenceKeys.LOCK_ALBUM_ID)],
            homeIntervalMinutes = prefs[intPreferencesKey(PreferenceKeys.HOME_INTERVAL_MINUTES)]
                ?: Constants.DEFAULT_INTERVAL_MINUTES,
            lockIntervalMinutes = prefs[intPreferencesKey(PreferenceKeys.LOCK_INTERVAL_MINUTES)]
                ?: Constants.DEFAULT_INTERVAL_MINUTES,
            homeScalingType = ScalingType.fromString(prefs[stringPreferencesKey(PreferenceKeys.HOME_SCALING_TYPE)]),
            lockScalingType = ScalingType.fromString(prefs[stringPreferencesKey(PreferenceKeys.LOCK_SCALING_TYPE)]),
            homeEffects = WallpaperEffects(
                enableBlur = prefs[booleanPreferencesKey(PreferenceKeys.HOME_ENABLE_BLUR)] ?: false,
                blurPercentage = prefs[intPreferencesKey(PreferenceKeys.HOME_BLUR)] ?: 0,
                enableDarken = prefs[booleanPreferencesKey(PreferenceKeys.HOME_ENABLE_DARKEN)] ?: false,
                darkenPercentage = prefs[intPreferencesKey(PreferenceKeys.HOME_DARKEN)] ?: 0,
                enableVignette = prefs[booleanPreferencesKey(PreferenceKeys.HOME_ENABLE_VIGNETTE)] ?: false,
                vignettePercentage = prefs[intPreferencesKey(PreferenceKeys.HOME_VIGNETTE)] ?: 0,
                enableGrayscale = prefs[booleanPreferencesKey(PreferenceKeys.HOME_GRAYSCALE)] ?: false
            ),
            lockEffects = WallpaperEffects(
                enableBlur = prefs[booleanPreferencesKey(PreferenceKeys.LOCK_ENABLE_BLUR)] ?: false,
                blurPercentage = prefs[intPreferencesKey(PreferenceKeys.LOCK_BLUR)] ?: 0,
                enableDarken = prefs[booleanPreferencesKey(PreferenceKeys.LOCK_ENABLE_DARKEN)] ?: false,
                darkenPercentage = prefs[intPreferencesKey(PreferenceKeys.LOCK_DARKEN)] ?: 0,
                enableVignette = prefs[booleanPreferencesKey(PreferenceKeys.LOCK_ENABLE_VIGNETTE)] ?: false,
                vignettePercentage = prefs[intPreferencesKey(PreferenceKeys.LOCK_VIGNETTE)] ?: 0,
                enableGrayscale = prefs[booleanPreferencesKey(PreferenceKeys.LOCK_GRAYSCALE)] ?: false
            ),
            adaptiveBrightness = prefs[booleanPreferencesKey(PreferenceKeys.ADAPTIVE_BRIGHTNESS)] ?: false
        )
    }

    suspend fun updateScheduleSettings(settings: ScheduleSettings) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_CHANGER)] = settings.enableChanger
            prefs[booleanPreferencesKey(PreferenceKeys.SEPARATE_SCHEDULES)] = settings.separateSchedules
            prefs[booleanPreferencesKey(PreferenceKeys.SHUFFLE_ENABLED)] = settings.shuffleEnabled
            prefs[booleanPreferencesKey(PreferenceKeys.HOME_ENABLED)] = settings.homeEnabled
            prefs[booleanPreferencesKey(PreferenceKeys.LOCK_ENABLED)] = settings.lockEnabled
            if (settings.homeAlbumId != null) {
                prefs[stringPreferencesKey(PreferenceKeys.HOME_ALBUM_ID)] = settings.homeAlbumId
            } else {
                prefs.remove(stringPreferencesKey(PreferenceKeys.HOME_ALBUM_ID))
            }
            if (settings.lockAlbumId != null) {
                prefs[stringPreferencesKey(PreferenceKeys.LOCK_ALBUM_ID)] = settings.lockAlbumId
            } else {
                prefs.remove(stringPreferencesKey(PreferenceKeys.LOCK_ALBUM_ID))
            }
            prefs[intPreferencesKey(PreferenceKeys.HOME_INTERVAL_MINUTES)] = settings.homeIntervalMinutes
            prefs[intPreferencesKey(PreferenceKeys.LOCK_INTERVAL_MINUTES)] = settings.lockIntervalMinutes
            prefs[stringPreferencesKey(PreferenceKeys.HOME_SCALING_TYPE)] = settings.homeScalingType.name
            prefs[stringPreferencesKey(PreferenceKeys.LOCK_SCALING_TYPE)] = settings.lockScalingType.name

            // Home effects
            prefs[booleanPreferencesKey(PreferenceKeys.HOME_ENABLE_BLUR)] = settings.homeEffects.enableBlur
            prefs[intPreferencesKey(PreferenceKeys.HOME_BLUR)] = settings.homeEffects.blurPercentage
            prefs[booleanPreferencesKey(PreferenceKeys.HOME_ENABLE_DARKEN)] = settings.homeEffects.enableDarken
            prefs[intPreferencesKey(PreferenceKeys.HOME_DARKEN)] = settings.homeEffects.darkenPercentage
            prefs[booleanPreferencesKey(PreferenceKeys.HOME_ENABLE_VIGNETTE)] = settings.homeEffects.enableVignette
            prefs[intPreferencesKey(PreferenceKeys.HOME_VIGNETTE)] = settings.homeEffects.vignettePercentage
            prefs[booleanPreferencesKey(PreferenceKeys.HOME_GRAYSCALE)] = settings.homeEffects.enableGrayscale

            // Lock effects
            prefs[booleanPreferencesKey(PreferenceKeys.LOCK_ENABLE_BLUR)] = settings.lockEffects.enableBlur
            prefs[intPreferencesKey(PreferenceKeys.LOCK_BLUR)] = settings.lockEffects.blurPercentage
            prefs[booleanPreferencesKey(PreferenceKeys.LOCK_ENABLE_DARKEN)] = settings.lockEffects.enableDarken
            prefs[intPreferencesKey(PreferenceKeys.LOCK_DARKEN)] = settings.lockEffects.darkenPercentage
            prefs[booleanPreferencesKey(PreferenceKeys.LOCK_ENABLE_VIGNETTE)] = settings.lockEffects.enableVignette
            prefs[intPreferencesKey(PreferenceKeys.LOCK_VIGNETTE)] = settings.lockEffects.vignettePercentage
            prefs[booleanPreferencesKey(PreferenceKeys.LOCK_GRAYSCALE)] = settings.lockEffects.enableGrayscale

            // Adaptive brightness
            prefs[booleanPreferencesKey(PreferenceKeys.ADAPTIVE_BRIGHTNESS)] = settings.adaptiveBrightness
        }
    }

    // ============ Atomic Album Selection Operations ============

    /**
     * Atomically update home album ID without race conditions
     * This prevents lost updates when both home and lock albums are selected simultaneously
     */
    suspend fun updateHomeAlbumId(albumId: String?) {
        dataStore.edit { prefs ->
            if (albumId != null) {
                prefs[stringPreferencesKey(PreferenceKeys.HOME_ALBUM_ID)] = albumId
            } else {
                prefs.remove(stringPreferencesKey(PreferenceKeys.HOME_ALBUM_ID))
            }
        }
    }

    /**
     * Atomically update lock album ID without race conditions
     * This prevents lost updates when both home and lock albums are selected simultaneously
     */
    suspend fun updateLockAlbumId(albumId: String?) {
        dataStore.edit { prefs ->
            if (albumId != null) {
                prefs[stringPreferencesKey(PreferenceKeys.LOCK_ALBUM_ID)] = albumId
            } else {
                prefs.remove(stringPreferencesKey(PreferenceKeys.LOCK_ALBUM_ID))
            }
        }
    }

    /**
     * Atomically clear album selections if they match the given album ID
     * Used when deleting an album to prevent race conditions
     * Returns true if any selections were cleared
     */
    suspend fun clearAlbumSelectionsIfMatches(albumId: String): Boolean {
        var wasCleared = false
        dataStore.edit { prefs ->
            val homeAlbumId = prefs[stringPreferencesKey(PreferenceKeys.HOME_ALBUM_ID)]
            val lockAlbumId = prefs[stringPreferencesKey(PreferenceKeys.LOCK_ALBUM_ID)]

            // Clear home album if it matches
            if (homeAlbumId == albumId) {
                prefs.remove(stringPreferencesKey(PreferenceKeys.HOME_ALBUM_ID))
                wasCleared = true
            }

            // Clear lock album if it matches
            if (lockAlbumId == albumId) {
                prefs.remove(stringPreferencesKey(PreferenceKeys.LOCK_ALBUM_ID))
                wasCleared = true
            }
        }
        return wasCleared
    }

    // ============ Atomic AppSettings Operations ============

    /**
     * Atomically update dark mode setting without race conditions
     */
    suspend fun updateDarkMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(PreferenceKeys.DARK_MODE)] = enabled
        }
    }

    /**
     * Atomically update AMOLED theme setting without race conditions
     */
    suspend fun updateAmoledTheme(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(PreferenceKeys.AMOLED_THEME)] = enabled
        }
    }

    /**
     * Atomically update dynamic theming setting without race conditions
     */
    suspend fun updateDynamicTheming(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(PreferenceKeys.DYNAMIC_THEMING)] = enabled
        }
    }

    /**
     * Atomically update animate setting without race conditions
     */
    suspend fun updateAnimate(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(PreferenceKeys.ANIMATE)] = enabled
        }
    }

    /**
     * Atomically update first launch setting without race conditions
     */
    suspend fun updateFirstLaunch(isFirstLaunch: Boolean) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(PreferenceKeys.FIRST_LAUNCH)] = isFirstLaunch
        }
    }

    // ============ Atomic ScheduleSettings Operations ============

    /**
     * Atomically update enableChanger setting without race conditions
     */
    suspend fun updateEnableChanger(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(PreferenceKeys.ENABLE_CHANGER)] = enabled
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
}
