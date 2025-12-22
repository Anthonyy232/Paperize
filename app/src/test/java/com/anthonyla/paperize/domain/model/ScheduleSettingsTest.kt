package com.anthonyla.paperize.domain.model

import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.constants.Constants
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ScheduleSettings domain model
 */
class ScheduleSettingsTest {

    // ============================================================
    // Test: validate()
    // ============================================================

    @Test
    fun `validate coerces interval below minimum to minimum`() {
        val settings = ScheduleSettings(
            homeIntervalMinutes = 5,  // Below MIN_INTERVAL_MINUTES (15)
            lockIntervalMinutes = 10,
            liveIntervalMinutes = 1
        )
        
        val validated = settings.validate()
        
        assertEquals(Constants.MIN_INTERVAL_MINUTES, validated.homeIntervalMinutes)
        assertEquals(Constants.MIN_INTERVAL_MINUTES, validated.lockIntervalMinutes)
        assertEquals(Constants.MIN_INTERVAL_MINUTES, validated.liveIntervalMinutes)
    }

    @Test
    fun `validate keeps valid intervals unchanged`() {
        val settings = ScheduleSettings(
            homeIntervalMinutes = 60,
            lockIntervalMinutes = 120,
            liveIntervalMinutes = 30
        )
        
        val validated = settings.validate()
        
        assertEquals(60, validated.homeIntervalMinutes)
        assertEquals(120, validated.lockIntervalMinutes)
        assertEquals(30, validated.liveIntervalMinutes)
    }

    @Test
    fun `validate validates effect percentages`() {
        val effects = WallpaperEffects(
            darkenPercentage = 150,  // Over 100
            blurPercentage = -10     // Negative
        )
        val settings = ScheduleSettings(homeEffects = effects)
        
        val validated = settings.validate()
        
        assertEquals(100, validated.homeEffects.darkenPercentage)
        assertEquals(0, validated.homeEffects.blurPercentage)
    }

    // ============================================================
    // Test: hasSchedulingChanges()
    // ============================================================

    @Test
    fun `hasSchedulingChanges returns false for identical settings`() {
        val settings1 = ScheduleSettings.default()
        val settings2 = ScheduleSettings.default()
        
        assertFalse(settings1.hasSchedulingChanges(settings2))
    }

    @Test
    fun `hasSchedulingChanges returns true when homeEnabled changes`() {
        val settings1 = ScheduleSettings(homeEnabled = true)
        val settings2 = ScheduleSettings(homeEnabled = false)
        
        assertTrue(settings1.hasSchedulingChanges(settings2))
    }

    @Test
    fun `hasSchedulingChanges returns true when lockEnabled changes`() {
        val settings1 = ScheduleSettings(lockEnabled = true)
        val settings2 = ScheduleSettings(lockEnabled = false)
        
        assertTrue(settings1.hasSchedulingChanges(settings2))
    }

    @Test
    fun `hasSchedulingChanges returns true when homeIntervalMinutes changes`() {
        val settings1 = ScheduleSettings(homeIntervalMinutes = 60)
        val settings2 = ScheduleSettings(homeIntervalMinutes = 120)
        
        assertTrue(settings1.hasSchedulingChanges(settings2))
    }

    @Test
    fun `hasSchedulingChanges returns true when lockIntervalMinutes changes`() {
        val settings1 = ScheduleSettings(lockIntervalMinutes = 60)
        val settings2 = ScheduleSettings(lockIntervalMinutes = 30)
        
        assertTrue(settings1.hasSchedulingChanges(settings2))
    }

    @Test
    fun `hasSchedulingChanges returns true when separateSchedules changes`() {
        val settings1 = ScheduleSettings(separateSchedules = true)
        val settings2 = ScheduleSettings(separateSchedules = false)
        
        assertTrue(settings1.hasSchedulingChanges(settings2))
    }

    @Test
    fun `hasSchedulingChanges returns true when liveIntervalMinutes changes`() {
        val settings1 = ScheduleSettings(liveIntervalMinutes = 60)
        val settings2 = ScheduleSettings(liveIntervalMinutes = 120)
        
        assertTrue(settings1.hasSchedulingChanges(settings2))
    }

    @Test
    fun `hasSchedulingChanges returns false when only display settings change`() {
        val settings1 = ScheduleSettings(
            homeScalingType = ScalingType.FILL,
            adaptiveBrightness = false
        )
        val settings2 = ScheduleSettings(
            homeScalingType = ScalingType.FIT,
            adaptiveBrightness = true
        )
        
        assertFalse(settings1.hasSchedulingChanges(settings2))
    }

    // ============================================================
    // Test: hasDisplayChanges()
    // ============================================================

    @Test
    fun `hasDisplayChanges returns false for identical settings`() {
        val settings1 = ScheduleSettings.default()
        val settings2 = ScheduleSettings.default()
        
        assertFalse(settings1.hasDisplayChanges(settings2))
    }

    @Test
    fun `hasDisplayChanges returns true when homeScalingType changes`() {
        val settings1 = ScheduleSettings(homeScalingType = ScalingType.FILL)
        val settings2 = ScheduleSettings(homeScalingType = ScalingType.FIT)
        
        assertTrue(settings1.hasDisplayChanges(settings2))
    }

    @Test
    fun `hasDisplayChanges returns true when lockScalingType changes`() {
        val settings1 = ScheduleSettings(lockScalingType = ScalingType.FILL)
        val settings2 = ScheduleSettings(lockScalingType = ScalingType.STRETCH)
        
        assertTrue(settings1.hasDisplayChanges(settings2))
    }

    @Test
    fun `hasDisplayChanges returns true when homeEffects change`() {
        val settings1 = ScheduleSettings(homeEffects = WallpaperEffects(enableBlur = false))
        val settings2 = ScheduleSettings(homeEffects = WallpaperEffects(enableBlur = true))
        
        assertTrue(settings1.hasDisplayChanges(settings2))
    }

    @Test
    fun `hasDisplayChanges returns true when lockEffects change`() {
        val settings1 = ScheduleSettings(lockEffects = WallpaperEffects(darkenPercentage = 0))
        val settings2 = ScheduleSettings(lockEffects = WallpaperEffects(darkenPercentage = 50))
        
        assertTrue(settings1.hasDisplayChanges(settings2))
    }

    @Test
    fun `hasDisplayChanges returns true when liveEffects change`() {
        val settings1 = ScheduleSettings(liveEffects = WallpaperEffects.none())
        val settings2 = ScheduleSettings(liveEffects = WallpaperEffects(enableVignette = true))
        
        assertTrue(settings1.hasDisplayChanges(settings2))
    }

    @Test
    fun `hasDisplayChanges returns true when liveScalingType changes`() {
        val settings1 = ScheduleSettings(liveScalingType = ScalingType.FILL)
        val settings2 = ScheduleSettings(liveScalingType = ScalingType.NONE)
        
        assertTrue(settings1.hasDisplayChanges(settings2))
    }

    @Test
    fun `hasDisplayChanges returns true when adaptiveBrightness changes`() {
        val settings1 = ScheduleSettings(adaptiveBrightness = false)
        val settings2 = ScheduleSettings(adaptiveBrightness = true)
        
        assertTrue(settings1.hasDisplayChanges(settings2))
    }

    @Test
    fun `hasDisplayChanges returns false when only scheduling settings change`() {
        val settings1 = ScheduleSettings(
            homeEnabled = true,
            homeIntervalMinutes = 60
        )
        val settings2 = ScheduleSettings(
            homeEnabled = false,
            homeIntervalMinutes = 120
        )
        
        assertFalse(settings1.hasDisplayChanges(settings2))
    }

    // ============================================================
    // Test: Companion object
    // ============================================================

    @Test
    fun `default factory creates settings with default values`() {
        val settings = ScheduleSettings.default()
        
        assertFalse(settings.enableChanger)
        assertFalse(settings.separateSchedules)
        assertFalse(settings.shuffleEnabled)
        assertFalse(settings.homeEnabled)
        assertFalse(settings.lockEnabled)
        assertNull(settings.homeAlbumId)
        assertNull(settings.lockAlbumId)
        assertEquals(Constants.DEFAULT_INTERVAL_MINUTES, settings.homeIntervalMinutes)
        assertEquals(Constants.DEFAULT_INTERVAL_MINUTES, settings.lockIntervalMinutes)
    }
}
