package com.anthonyla.paperize.service.worker

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.usesVisibleLiveTimer
import com.anthonyla.paperize.service.alarm.StaticWallpaperAlarmScheduler
import com.anthonyla.paperize.service.alarm.usesStaticAlarm
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class WallpaperScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val alarmScheduler: StaticWallpaperAlarmScheduler
) {
    // Lazy: the Application injects this before WorkManager can read its Hilt worker factory.
    private val workManager by lazy { WorkManager.getInstance(context) }
    private val mutex = Mutex()

    /**
     * Static intervals below [Constants.MIN_WORKMANAGER_INTERVAL_MINUTES] are driven by an alarm,
     * because WorkManager rejects shorter periodic work outright. Everything else stays on
     * WorkManager so it keeps the framework's reboot and retry handling.
     */
    fun scheduleWallpaperChange(
        screenType: ScreenType,
        intervalMinutes: Int,
        resetInterval: Boolean = false,
        onlyIfNotScheduled: Boolean = false
    ) {
        if (screenType != ScreenType.LIVE && usesStaticAlarm(intervalMinutes)) {
            // The alarm path owns this screen, so any leftover periodic work must go: two active
            // schedules would change the wallpaper at double the requested rate.
            workManager.cancelUniqueWork(workName(screenType))
            if (!onlyIfNotScheduled || !alarmScheduler.isScheduled(screenType)) {
                alarmScheduler.schedule(screenType, intervalMinutes)
            }
            Log.d(TAG, "Scheduled $screenType alarm every $intervalMinutes minutes")
            return
        }

        alarmScheduler.cancel(screenType)
        val interval = intervalMinutes.toLong()
            .coerceAtLeast(Constants.MIN_WORKMANAGER_INTERVAL_MINUTES.toLong())
        val workRequest = PeriodicWorkRequestBuilder<WallpaperChangeWorker>(interval, TimeUnit.MINUTES)
            .setInputData(workDataOf(Constants.EXTRA_SCREEN_TYPE to screenType.name))
            .apply {
                // New periodic work otherwise runs immediately. Unlike an initial delay, this
                // one-run deadline also survives unrelated UPDATE requests from settings edits.
                if (resetInterval) {
                    setNextScheduleTimeOverride(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(interval))
                }
            }
            .build()

        // Preserve the existing work and explicitly move only the next run after a manual change.
        // Replacing periodic work starts a new first period, which can otherwise run immediately.
        workManager.enqueueUniquePeriodicWork(workName(screenType), policy(onlyIfNotScheduled), workRequest)
        Log.d(TAG, "Scheduled $screenType wallpaper change every $interval minutes")
    }

    /** Reset only the automatic schedules affected by a successful manual change. */
    fun resetAfterManualChange(screenType: ScreenType, settings: ScheduleSettings, wallpaperMode: WallpaperMode) {
        scheduledScreensToReset(screenType, settings, wallpaperMode).forEach { scheduledScreen ->
            scheduleWallpaperChange(scheduledScreen, settings.intervalMinutes(scheduledScreen), resetInterval = true)
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
            val handledByEngine = screen == ScreenType.LIVE && usesVisibleLiveTimer(interval)
            if (screen in targets && interval > 0 && !handledByEngine) {
                scheduleWallpaperChange(screen, interval, onlyIfNotScheduled = onlyIfNotScheduled)
            } else {
                workManager.cancelUniqueWork(workName(screen))
                alarmScheduler.cancel(screen)
            }
        }
        if (enabled) scheduleDailyAlbumRefresh(onlyIfNotScheduled) else workManager.cancelUniqueWork(Constants.WORK_NAME_REFRESH)
    }

    fun cancelAllWallpaperChanges() {
        ScreenType.entries.forEach { workManager.cancelUniqueWork(workName(it)) }
        alarmScheduler.cancelAll()
        workManager.cancelUniqueWork(Constants.WORK_NAME_REFRESH)
    }

    /**
     * Run one rotation now, as expedited one-time work.
     *
     * The short-interval alarm fires while the app is in the background, where starting
     * [com.anthonyla.paperize.service.wallpaper.WallpaperChangeService] is rejected on Android 12+
     * and a broadcast receiver has only seconds of lifetime. Expedited work is the supported
     * background path: it runs immediately, gets minutes instead of seconds, and reuses the
     * worker's retry handling plus the change lock that serializes it against every other
     * rotation.
     */
    fun changeNow(screenType: ScreenType) {
        workManager.enqueueUniqueWork(
            alarmChangeWorkName(screenType),
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WallpaperChangeWorker>()
                .setInputData(workDataOf(Constants.EXTRA_SCREEN_TYPE to screenType.name))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        )
        Log.d(TAG, "Enqueued expedited $screenType wallpaper change")
    }

    /** True when [screenType] should currently be driven by the short-interval alarm path. */
    fun isStaticAlarmScheduled(settings: ScheduleSettings, screenType: ScreenType): Boolean {
        if (!settings.enableChanger) return false
        if (screenType == ScreenType.LIVE) return false
        val interval = settings.intervalMinutes(screenType)
        return interval > 0 && usesStaticAlarm(interval)
    }

    /** Rescan folders now; KEEP coalesces rapid foreground transitions into one scan. */
    fun refreshAlbumsNow() {
        workManager.enqueueUniqueWork(
            FOREGROUND_REFRESH_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<AlbumRefreshWorker>().build()
        )
    }

    /** Refresh daily, with the first run at the next 3 AM. */
    private fun scheduleDailyAlbumRefresh(onlyIfNotScheduled: Boolean) {
        val now = ZonedDateTime.now()
        val today3am = now.toLocalDate().atTime(3, 0).atZone(now.zone)
        val next3am = if (today3am.isAfter(now)) today3am else today3am.plusDays(1)
        val workRequest = PeriodicWorkRequestBuilder<AlbumRefreshWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next3am))
            .build()
        workManager.enqueueUniquePeriodicWork(Constants.WORK_NAME_REFRESH, policy(onlyIfNotScheduled), workRequest)
    }

    private fun policy(onlyIfNotScheduled: Boolean) =
        if (onlyIfNotScheduled) ExistingPeriodicWorkPolicy.KEEP else ExistingPeriodicWorkPolicy.UPDATE

    private fun workName(screenType: ScreenType): String = when (screenType) {
        ScreenType.HOME -> Constants.WORK_NAME_HOME
        ScreenType.LOCK -> Constants.WORK_NAME_LOCK
        ScreenType.BOTH -> Constants.WORK_NAME_BOTH
        ScreenType.LIVE -> Constants.WORK_NAME_LIVE
    }

    private fun alarmChangeWorkName(screenType: ScreenType): String = "alarm_change_${screenType.name.lowercase()}"

    private companion object {
        const val TAG = "WallpaperScheduler"
        const val FOREGROUND_REFRESH_WORK_NAME = "album_refresh_on_foreground"
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
            // Below the WorkManager floor the visible engine owns the cadence, so there is no
            // scheduled job to defer; the engine restarts its own timer instead.
            ScreenType.LIVE -> manualScreen == ScreenType.LIVE &&
                settings.liveIntervalMinutes >= Constants.MIN_WORKMANAGER_INTERVAL_MINUTES
            ScreenType.BOTH -> manualScreen != ScreenType.LIVE
            else -> manualScreen == screen || manualScreen == ScreenType.BOTH
        }
    }
}
