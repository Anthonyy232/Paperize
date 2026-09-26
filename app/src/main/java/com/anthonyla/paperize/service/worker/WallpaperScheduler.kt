package com.anthonyla.paperize.service.worker

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.model.ScheduleSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class WallpaperScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    private val mutex = Mutex()

    companion object {
        private const val TAG = "WallpaperScheduler"
    }

    /**
     * Schedule periodic wallpaper change
     *
     * @param screenType HOME, LOCK, or BOTH (for synchronized schedules)
     * @param intervalMinutes Interval between changes (minimum 15 minutes for WorkManager)
     */
    fun scheduleWallpaperChange(
        screenType: ScreenType,
        intervalMinutes: Int,
        resetInterval: Boolean = false,
        onlyIfNotScheduled: Boolean = false
    ) {

        val adjustedInterval = intervalMinutes.toLong().coerceAtLeast(Constants.MIN_INTERVAL_MINUTES.toLong())
        val workName = getWorkName(screenType)

        val inputData = Data.Builder()
            .putString(Constants.EXTRA_SCREEN_TYPE, screenType.name)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WallpaperChangeWorker>(
            adjustedInterval,
            TimeUnit.MINUTES
        )
            .setInputData(inputData)
            .addTag(getWorkTag(screenType))
            .apply {
                // New periodic work otherwise runs immediately. Unlike an initial delay, this
                // one-run deadline also survives unrelated UPDATE requests from settings edits.
                if (resetInterval) {
                    setNextScheduleTimeOverride(
                        System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(adjustedInterval)
                    )
                }
            }
            .build()

        // Preserve the existing work and explicitly move only the next run after a manual change.
        // Replacing periodic work starts a new first period, which can otherwise run immediately.
        workManager.enqueueUniquePeriodicWork(
            workName,
            if (onlyIfNotScheduled) ExistingPeriodicWorkPolicy.KEEP else ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Log.d(TAG, "Scheduled $screenType wallpaper change every $adjustedInterval minutes")
    }

    /** Reset only the automatic schedules affected by a successful manual change. */
    fun resetAfterManualChange(
        screenType: ScreenType,
        settings: ScheduleSettings,
        wallpaperMode: WallpaperMode
    ) {
        scheduledScreensToReset(screenType, settings, wallpaperMode).forEach { scheduledScreen ->
            scheduleWallpaperChange(
                screenType = scheduledScreen,
                intervalMinutes = settings.intervalMinutes(scheduledScreen),
                resetInterval = true
            )
        }
    }

    /** Reconcile all jobs from the same policy on startup, settings edits and album selection. */
    suspend fun updateSchedules(
        settings: ScheduleSettings,
        mode: WallpaperMode,
        onlyIfNotScheduled: Boolean = false
    ) = mutex.withLock {
        val enabled = settings.enableChanger && settings.hasRequiredAlbums(mode)
        val targets = if (enabled) settings.activeScreens(mode) else emptySet()
        for (screen in ScreenType.entries) {
            val interval = settings.intervalMinutes(screen)
            val handledByEngine = screen == ScreenType.LIVE && interval < Constants.MIN_INTERVAL_MINUTES
            if (screen in targets && interval > 0 && !handledByEngine) {
                scheduleWallpaperChange(screen, interval, onlyIfNotScheduled = onlyIfNotScheduled)
            } else {
                cancelWallpaperChange(screen)
            }
        }
        if (enabled) scheduleAlbumRefresh(onlyIfNotScheduled) else cancelAlbumRefresh()
    }

    fun cancelWallpaperChange(screenType: ScreenType) {
        val workName = getWorkName(screenType)
        workManager.cancelUniqueWork(workName)
        Log.d(TAG, "Cancelled $screenType wallpaper change schedule")
    }

    fun cancelAllWallpaperChanges() {
        workManager.cancelUniqueWork(Constants.WORK_NAME_HOME)
        workManager.cancelUniqueWork(Constants.WORK_NAME_LOCK)
        workManager.cancelUniqueWork(Constants.WORK_NAME_BOTH)
        workManager.cancelUniqueWork(Constants.WORK_NAME_LIVE)
        cancelAlbumRefresh()
        Log.d(TAG, "Cancelled all wallpaper change schedules")
    }

    /** Refresh daily, with the first run targeting 3 AM. */
    fun scheduleAlbumRefresh(onlyIfNotScheduled: Boolean = false) {
        val calendar = Calendar.getInstance()
        val nowMillis = calendar.timeInMillis
        
        if (calendar.get(Calendar.HOUR_OF_DAY) >= 3) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        calendar.set(Calendar.HOUR_OF_DAY, 3)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val initialDelay = calendar.timeInMillis - nowMillis

        val workRequest = PeriodicWorkRequestBuilder<AlbumRefreshWorker>(
            1,
            TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(Constants.WORK_TAG_REFRESH)
            .build()

        workManager.enqueueUniquePeriodicWork(
            Constants.WORK_NAME_REFRESH,
            if (onlyIfNotScheduled) ExistingPeriodicWorkPolicy.KEEP else ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Log.d(TAG, "Scheduled daily album refresh")
    }

    fun cancelAlbumRefresh() {
        workManager.cancelUniqueWork(Constants.WORK_NAME_REFRESH)
        Log.d(TAG, "Cancelled daily album refresh")
    }

    private fun getWorkName(screenType: ScreenType): String {
        return when (screenType) {
            ScreenType.HOME -> Constants.WORK_NAME_HOME
            ScreenType.LOCK -> Constants.WORK_NAME_LOCK
            ScreenType.BOTH -> Constants.WORK_NAME_BOTH
            ScreenType.LIVE -> Constants.WORK_NAME_LIVE
        }
    }

    private fun getWorkTag(screenType: ScreenType): String {
        return when (screenType) {
            ScreenType.HOME -> Constants.WORK_TAG_HOME
            ScreenType.LOCK -> Constants.WORK_TAG_LOCK
            ScreenType.BOTH -> Constants.WORK_TAG_BOTH
            ScreenType.LIVE -> Constants.WORK_TAG_LIVE
        }
    }

}

internal fun scheduledScreensToReset(
    manualScreen: ScreenType,
    settings: ScheduleSettings,
    wallpaperMode: WallpaperMode
): Set<ScreenType> {
    if (!settings.enableChanger || !settings.hasRequiredAlbums(wallpaperMode)) return emptySet()
    return settings.activeScreens(wallpaperMode).filterTo(mutableSetOf()) { screen ->
        when (screen) {
            ScreenType.LIVE -> manualScreen == ScreenType.LIVE &&
                settings.liveIntervalMinutes >= Constants.MIN_INTERVAL_MINUTES
            ScreenType.BOTH -> manualScreen != ScreenType.LIVE
            else -> manualScreen == screen || manualScreen == ScreenType.BOTH
        }
    }
}
