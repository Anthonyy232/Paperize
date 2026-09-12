package com.anthonyla.paperize.service.worker

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.model.ScheduleSettings
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class WallpaperSchedulerInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val workManager = WorkManager.getInstance(context) as WorkManagerImpl
    private val scheduler = WallpaperScheduler(context)
    private val targets = mapOf(
        ScreenType.HOME to Constants.WORK_NAME_HOME,
        ScreenType.LOCK to Constants.WORK_NAME_LOCK,
        ScreenType.BOTH to Constants.WORK_NAME_BOTH,
        ScreenType.LIVE to Constants.WORK_NAME_LIVE
    )

    @Before
    @After
    fun clearTestSchedules() {
        targets.values.forEach { name ->
            workManager.cancelUniqueWork(name).result.get(10, TimeUnit.SECONDS)
        }
    }

    @Test
    fun manualResetJobsRemainInTheirFirstPeriodUntilTheFullDelay() = runBlocking {
        val delayMillis = TimeUnit.MINUTES.toMillis(15)
        val jobs = targets.map { (screen, name) ->
            val before = System.currentTimeMillis()
            scheduler.scheduleWallpaperChange(screen, 15, resetInterval = true)
            val info = awaitEnqueued(name)
            assertTrue(info.nextScheduleTimeMillis >= before + delayMillis)
            info.id to before
        }

        // Checking only the next countdown can miss an immediate first run: after that run,
        // periodic work also shows a full interval. Inspect the persisted first-period state.
        SystemClock.sleep(2_000)
        jobs.forEach { (id, before) ->
            val spec = requireNotNull(workManager.workDatabase.workSpecDao().getWorkSpec(id.toString()))
            assertEquals(delayMillis, spec.intervalDuration)
            assertEquals(0, spec.periodCount)
            assertEquals(WorkInfo.State.ENQUEUED, spec.state)
            assertTrue(spec.calculateNextRunTime() >= before + delayMillis)
            assertTrue(spec.calculateNextRunTime() <= System.currentTimeMillis() + delayMillis)
        }
    }

    @Test
    fun settingsUpdatePreservesThePendingManualResetDelay() = runBlocking {
        scheduler.scheduleWallpaperChange(ScreenType.HOME, 60, resetInterval = true)
        val reset = awaitEnqueued(Constants.WORK_NAME_HOME)

        scheduler.scheduleWallpaperChange(ScreenType.HOME, 60)

        val updated = awaitEnqueued(Constants.WORK_NAME_HOME, minimumGeneration = reset.generation + 1)
        assertEquals(reset.id, updated.id)
        assertEquals(reset.nextScheduleTimeMillis, updated.nextScheduleTimeMillis)
        val spec = requireNotNull(workManager.workDatabase.workSpecDao().getWorkSpec(updated.id.toString()))
        assertEquals(0, spec.periodCount)
    }

    @Test
    fun repeatedHomeResetLeavesTheIndependentLockCountdownUntouched() = runBlocking {
        scheduler.scheduleWallpaperChange(ScreenType.HOME, 60, resetInterval = true)
        scheduler.scheduleWallpaperChange(ScreenType.LOCK, 90, resetInterval = true)
        val home = awaitEnqueued(Constants.WORK_NAME_HOME)
        val lock = awaitEnqueued(Constants.WORK_NAME_LOCK)
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

        val newHome = awaitEnqueued(Constants.WORK_NAME_HOME, minimumGeneration = home.generation + 1)
        val sameLock = awaitEnqueued(Constants.WORK_NAME_LOCK)
        assertEquals(home.id, newHome.id)
        assertTrue(newHome.nextScheduleTimeMillis >= before + TimeUnit.MINUTES.toMillis(60))
        assertEquals(lock.id, sameLock.id)
        assertEquals(lock.nextScheduleTimeMillis, sameLock.nextScheduleTimeMillis)
    }

    private suspend fun awaitEnqueued(
        name: String,
        minimumGeneration: Int = 0
    ): WorkInfo =
        withTimeout(10_000) {
            workManager.getWorkInfosForUniqueWorkFlow(name).first { infos ->
                infos.any {
                    it.state == WorkInfo.State.ENQUEUED &&
                        it.generation >= minimumGeneration
                }
            }.single {
                it.state == WorkInfo.State.ENQUEUED &&
                    it.generation >= minimumGeneration
            }
        }
}
