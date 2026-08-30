package com.anthonyla.paperize.service.worker

import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.domain.model.ScheduleSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperSchedulingPolicyTest {
    @Test
    fun `manual synchronized change resets the shared job`() {
        val settings = ScheduleSettings(
            enableChanger = true,
            homeEnabled = true,
            lockEnabled = true,
            homeAlbumId = "album",
            lockAlbumId = "album"
        )

        assertEquals(
            setOf(ScreenType.BOTH),
            scheduledScreensToReset(ScreenType.HOME, settings, WallpaperMode.STATIC)
        )
    }

    @Test
    fun `manual both change resets each independent job`() {
        val settings = ScheduleSettings(
            enableChanger = true,
            separateSchedules = true,
            homeEnabled = true,
            lockEnabled = true,
            homeAlbumId = "home",
            lockAlbumId = "lock"
        )

        assertEquals(
            setOf(ScreenType.HOME, ScreenType.LOCK),
            scheduledScreensToReset(ScreenType.BOTH, settings, WallpaperMode.STATIC)
        )
    }

    @Test
    fun `manual change does not create a disabled schedule`() {
        assertEquals(
            emptySet<ScreenType>(),
            scheduledScreensToReset(
                ScreenType.BOTH,
                ScheduleSettings(enableChanger = false),
                WallpaperMode.STATIC
            )
        )
    }

    @Test
    fun `short live interval belongs to visible engine instead of WorkManager`() {
        val settings = ScheduleSettings(
            enableChanger = true,
            liveAlbumId = "live",
            liveIntervalMinutes = 14
        )

        assertEquals(
            emptySet<ScreenType>(),
            scheduledScreensToReset(ScreenType.LIVE, settings, WallpaperMode.LIVE)
        )
        assertEquals(
            setOf(ScreenType.LIVE),
            scheduledScreensToReset(
                ScreenType.LIVE,
                settings.copy(liveIntervalMinutes = 15),
                WallpaperMode.LIVE
            )
        )
    }
}
