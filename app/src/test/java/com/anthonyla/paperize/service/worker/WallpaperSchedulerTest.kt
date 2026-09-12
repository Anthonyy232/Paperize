@file:Suppress("RestrictedApi")

package com.anthonyla.paperize.service.worker

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.model.ScheduleSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WallpaperSchedulerTest {
    private val context = mockk<Context>()
    private val workManager = mockk<WorkManager>()
    private val operation = mockk<Operation>()
    private val enqueued = mutableListOf<Triple<String, ExistingPeriodicWorkPolicy, PeriodicWorkRequest>>()
    private lateinit var scheduler: WallpaperScheduler

    @Before
    fun setUp() {
        mockkObject(WorkManager.Companion)
        mockkStatic(Log::class)
        every { WorkManager.getInstance(context) } returns workManager
        every { Log.d(any(), any()) } returns 0
        every { workManager.enqueueUniquePeriodicWork(any(), any(), any()) } answers {
            enqueued += Triple(firstArg(), secondArg(), thirdArg())
            operation
        }
        scheduler = WallpaperScheduler(context)
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `manual resets defer the next run for a full interval for every target`() {
        val intervalMillis = TimeUnit.MINUTES.toMillis(60)
        ScreenType.entries.forEach { screen ->
            val enqueueTime = System.currentTimeMillis()
            scheduler.scheduleWallpaperChange(screen, 60, resetInterval = true)

            val (_, policy, request) = enqueued.last()
            assertEquals(ExistingPeriodicWorkPolicy.UPDATE, policy)
            assertEquals(screen.name, request.workSpec.input.getString(Constants.EXTRA_SCREEN_TYPE))
            request.workSpec.lastEnqueueTime = enqueueTime
            val nextRun = request.workSpec.calculateNextRunTime()
            assertTrue(nextRun >= enqueueTime + intervalMillis)
            assertTrue(nextRun <= System.currentTimeMillis() + intervalMillis)
        }
    }

    @Test
    fun `ordinary scheduling retains update policy and immediate first activation`() {
        scheduler.scheduleWallpaperChange(ScreenType.HOME, 60)

        val (_, policy, request) = enqueued.single()
        assertEquals(ExistingPeriodicWorkPolicy.UPDATE, policy)
        assertEquals(0L, request.workSpec.initialDelay)
        assertEquals(Long.MAX_VALUE, request.workSpec.nextScheduleTimeOverride)
    }

    @Test
    fun `manual reset delay uses the same minimum as the periodic interval`() {
        val before = System.currentTimeMillis()
        scheduler.scheduleWallpaperChange(ScreenType.HOME, 1, resetInterval = true)

        val request = enqueued.single().third
        val minimumMillis = TimeUnit.MINUTES.toMillis(15)
        assertEquals(minimumMillis, request.workSpec.intervalDuration)
        assertTrue(request.workSpec.calculateNextRunTime() >= before + minimumMillis)
        assertTrue(request.workSpec.calculateNextRunTime() <= System.currentTimeMillis() + minimumMillis)
    }

    @Test
    fun `manual home reset delays only its active independent schedule`() {
        val before = System.currentTimeMillis()
        scheduler.resetAfterManualChange(
            ScreenType.HOME,
            ScheduleSettings(
                enableChanger = true,
                separateSchedules = true,
                homeEnabled = true,
                lockEnabled = true,
                homeAlbumId = "home",
                lockAlbumId = "lock",
                homeIntervalMinutes = 60,
                lockIntervalMinutes = 90
            ),
            WallpaperMode.STATIC
        )

        val (name, policy, request) = enqueued.single()
        assertEquals(Constants.WORK_NAME_HOME, name)
        assertEquals(ExistingPeriodicWorkPolicy.UPDATE, policy)
        assertTrue(request.workSpec.calculateNextRunTime() >= before + TimeUnit.MINUTES.toMillis(60))
    }
}
