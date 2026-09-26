package com.anthonyla.paperize.data.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.WallpaperEffects
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Test

class PreferencesManagerInstrumentedTest {
    @Test fun concurrentScheduleEditsReadTheLatestValueWithoutLosingSelections() = runBlocking {
        val preferences = PreferencesManager(ApplicationProvider.getApplicationContext<Context>())
        val previousSettings = preferences.getScheduleSettings()
        try {
            val initial = ScheduleSettings(homeAlbumId = "home", homeIntervalMinutes = 30)
            preferences.updateScheduleSettings(initial)
            coroutineScope {
                repeat(20) {
                    launch {
                        preferences.updateScheduleSettings { current ->
                            current.copy(homeIntervalMinutes = current.homeIntervalMinutes + 1)
                        }
                    }
                }
                launch { preferences.updateHomeAlbumId("new-home") }
            }
            assertEquals(initial.copy(homeAlbumId = "new-home", homeIntervalMinutes = 50), preferences.getScheduleSettings())
        } finally {
            preferences.updateScheduleSettings(previousSettings)
        }
    }

    @Test fun resettingSchedulesPreservesAppearanceAndWallpaperMode() = runBlocking {
        val preferences = PreferencesManager(ApplicationProvider.getApplicationContext<Context>())
        val previousSettings = preferences.getScheduleSettings()
        val previousMode = preferences.getWallpaperMode()
        val appearance = preferences.getAppSettings()
        try {
            preferences.updateWallpaperMode(WallpaperMode.LIVE)
            val effects = WallpaperEffects(
                enableBlur = true, blurPercentage = 30, enableDarken = true, darkenPercentage = 20,
                enableVignette = true, vignettePercentage = 40, enableGrayscale = true, grayscalePercentage = 50,
                enableDoubleTap = true, enableChangeOnScreenOff = true, enableParallax = true, parallaxIntensity = 60
            )
            preferences.updateScheduleSettings(ScheduleSettings(
                enableChanger = true, separateSchedules = true, shuffleEnabled = true,
                homeEnabled = true, lockEnabled = true, homeAlbumId = "home", lockAlbumId = "lock", liveAlbumId = "live",
                homeIntervalMinutes = 25, lockIntervalMinutes = 35, liveIntervalMinutes = 45,
                homeScalingType = ScalingType.FIT, lockScalingType = ScalingType.STRETCH, liveScalingType = ScalingType.NONE,
                homeScrollingEnabled = true, homeEffects = effects, lockEffects = effects, liveEffects = effects,
                adaptiveBrightness = true
            ))
            preferences.clearScheduleSettings()
            assertEquals(ScheduleSettings(), preferences.getScheduleSettings())
            assertEquals(appearance, preferences.getAppSettings())
            assertEquals(WallpaperMode.LIVE, preferences.getWallpaperMode())
        } finally {
            preferences.updateScheduleSettings(previousSettings)
            preferences.updateWallpaperMode(previousMode)
        }
    }

    @Test fun clearingAnEmptyAlbumPreservesNewSelectionsAndOtherSettings() = runBlocking {
        val preferences = PreferencesManager(ApplicationProvider.getApplicationContext<Context>())
        val previousSettings = preferences.getScheduleSettings()
        val previousMode = preferences.getWallpaperMode()
        try {
            preferences.updateWallpaperMode(WallpaperMode.STATIC)
            val settings = ScheduleSettings(enableChanger = true, homeEnabled = true, lockEnabled = true,
                homeAlbumId = "new-home", lockAlbumId = "lock", homeIntervalMinutes = 75)
            preferences.updateScheduleSettings(settings)
            preferences.clearEmptyAlbumSelection("old-home", ScreenType.HOME)
            assertEquals(settings, preferences.getScheduleSettings())
            preferences.clearEmptyAlbumSelection("new-home", ScreenType.HOME)
            assertEquals(settings.copy(homeAlbumId = null), preferences.getScheduleSettings())
            preferences.clearEmptyAlbumSelection("lock", ScreenType.LOCK)
            assertEquals(settings.copy(homeAlbumId = null, lockAlbumId = null, enableChanger = false), preferences.getScheduleSettings())
        } finally {
            preferences.updateScheduleSettings(previousSettings)
            preferences.updateWallpaperMode(previousMode)
        }
    }
}
