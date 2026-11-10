package com.anthonyla.paperize.core.util

import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.ScreenType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplaySizeTest {

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
    fun `only home fill with scrolling enabled preserves overflow`() {
        assertTrue(usesLauncherManagedScrolling(ScreenType.HOME, ScalingType.FILL, true))
        assertTrue(usesLauncherManagedScrolling(ScreenType.BOTH, ScalingType.FILL, true))
        assertFalse(usesLauncherManagedScrolling(ScreenType.HOME, ScalingType.FILL, false))
        assertFalse(usesLauncherManagedScrolling(ScreenType.HOME, ScalingType.FIT, true))
        assertFalse(usesLauncherManagedScrolling(ScreenType.HOME, ScalingType.STRETCH, true))
        assertFalse(usesLauncherManagedScrolling(ScreenType.HOME, ScalingType.NONE, true))
        assertFalse(usesLauncherManagedScrolling(ScreenType.LOCK, ScalingType.FILL, true))
    }
}
