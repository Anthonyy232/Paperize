package com.anthonyla.paperize.domain.model

import com.anthonyla.paperize.core.ScalingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleSettingsTest {
    @Test
    fun `validation applies distinct static and live minimums and validates all screens`() {
        val effects = WallpaperEffects(darkenPercentage = 150, blurPercentage = -10)
        val settings = ScheduleSettings(
            homeIntervalMinutes = 5,
            lockIntervalMinutes = 120,
            liveIntervalMinutes = 0,
            homeEffects = effects,
            lockEffects = effects,
            liveEffects = effects
        )
        val validEffects = effects.copy(darkenPercentage = 100, blurPercentage = 0)
        assertEquals(
            settings.copy(
                homeIntervalMinutes = 15,
                liveIntervalMinutes = 1,
                homeEffects = validEffects,
                lockEffects = validEffects,
                liveEffects = validEffects
            ),
            settings.validate()
        )
    }

    @Test
    fun `visible live timer is used only below WorkManager minimum`() {
        assertFalse(usesVisibleLiveTimer(0))
        assertTrue(usesVisibleLiveTimer(1))
        assertTrue(usesVisibleLiveTimer(14))
        assertFalse(usesVisibleLiveTimer(15))
    }

    @Test
    fun `schedule edits require rescheduling without reapplying display effects`() {
        val current = ScheduleSettings()
        listOf(
            current.copy(enableChanger = true),
            current.copy(homeAlbumId = "home"),
            current.copy(lockAlbumId = "lock"),
            current.copy(liveAlbumId = "live"),
            current.copy(homeEnabled = true),
            current.copy(lockEnabled = true),
            current.copy(homeIntervalMinutes = 120),
            current.copy(lockIntervalMinutes = 120),
            current.copy(liveIntervalMinutes = 120),
            current.copy(separateSchedules = true)
        ).forEach { edited ->
            assertTrue("Scheduling: $edited", current.hasSchedulingChanges(edited))
            assertFalse("Display: $edited", current.hasDisplayChanges(edited))
        }
    }

    @Test
    fun `display edits require reapplication without resetting schedules`() {
        val current = ScheduleSettings()
        val effects = WallpaperEffects(enableBlur = true)
        listOf(
            current.copy(homeScalingType = ScalingType.FIT),
            current.copy(lockScalingType = ScalingType.STRETCH),
            current.copy(liveScalingType = ScalingType.NONE),
            current.copy(homeScrollingEnabled = true),
            current.copy(homeEffects = effects),
            current.copy(lockEffects = effects),
            current.copy(liveEffects = effects),
            current.copy(adaptiveBrightness = true)
        ).forEach { edited ->
            assertTrue("Display: $edited", current.hasDisplayChanges(edited))
            assertFalse("Scheduling: $edited", current.hasSchedulingChanges(edited))
        }
        assertFalse(current.hasSchedulingChanges(current))
        assertFalse(current.hasDisplayChanges(current))
    }
}
