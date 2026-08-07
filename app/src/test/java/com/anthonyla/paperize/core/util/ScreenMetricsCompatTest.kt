package com.anthonyla.paperize.core.util

import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.ScreenType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenMetricsCompatTest {

    @Test
    fun `largest internal display wins even when current window is folded`() {
        val result = selectLargestDisplayDimensions(
            listOf(
                1080 to 2092,
                1840 to 2208,
                920 to 1104
            )
        )

        assertEquals(1840 to 2208, result)
    }

    @Test
    fun `selection uses pixel area instead of only one dimension`() {
        val result = selectLargestDisplayDimensions(
            listOf(
                2400 to 800,
                1800 to 1400
            )
        )

        assertEquals(1800 to 1400, result)
    }

    @Test
    fun `invalid dimensions are ignored`() {
        val result = selectLargestDisplayDimensions(
            listOf(
                0 to 2208,
                -1 to 1080
            )
        )

        assertNull(result)
    }

    @Test
    fun `natural display mode wins while current window is landscape`() {
        val result = selectWallpaperDisplayDimensions(
            currentWindow = 2400 to 1080,
            supportedModes = listOf(1080 to 2400)
        )

        assertEquals(1080 to 2400, result)
    }

    @Test
    fun `current window is used when display modes are unavailable`() {
        val result = selectWallpaperDisplayDimensions(
            currentWindow = 1920 to 1080,
            supportedModes = emptyList()
        )

        assertEquals(1920 to 1080, result)
    }

    @Test
    fun `home fill preserves overflow only when scrolling is enabled`() {
        assertFalse(usesLauncherManagedScrolling(ScreenType.HOME, ScalingType.FILL, false))
        assertTrue(usesLauncherManagedScrolling(ScreenType.HOME, ScalingType.FILL, true))
        assertTrue(usesLauncherManagedScrolling(ScreenType.BOTH, ScalingType.FILL, true))
    }

    @Test
    fun `non-fill and lock rendering use exact canvas`() {
        assertFalse(usesLauncherManagedScrolling(ScreenType.HOME, ScalingType.FIT, true))
        assertFalse(usesLauncherManagedScrolling(ScreenType.HOME, ScalingType.STRETCH, true))
        assertFalse(usesLauncherManagedScrolling(ScreenType.HOME, ScalingType.NONE, true))
        assertFalse(usesLauncherManagedScrolling(ScreenType.LOCK, ScalingType.FILL, true))
    }
}
