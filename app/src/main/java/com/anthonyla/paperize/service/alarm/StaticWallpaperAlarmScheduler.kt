package com.anthonyla.paperize.service.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives static wallpaper rotation at intervals shorter than WorkManager's 15-minute floor.
 *
 * WorkManager periodic work is still used at and above that floor: it survives reboots and is
 * retried by the framework. Below it, an inexact alarm is the only mechanism Android allows, so
 * [com.anthonyla.paperize.service.worker.WallpaperScheduler] keeps the two mutually exclusive.
 *
 * The interval travels inside the [PendingIntent] so the receiver can re-arm the next alarm
 * without reading DataStore on the broadcast thread.
 */
@Singleton
class StaticWallpaperAlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val alarmManager by lazy { context.getSystemService(AlarmManager::class.java) }

    /** Replace any pending alarm for [screenType] with one that fires [intervalMinutes] from now. */
    fun schedule(screenType: ScreenType, intervalMinutes: Int) {
        val interval = intervalMinutes.coerceAtLeast(Constants.MIN_STATIC_INTERVAL_MINUTES)
        setAlarm(screenType, interval, System.currentTimeMillis() + interval * MINUTE_MILLIS)
        Log.d(TAG, "Scheduled $screenType alarm in $interval minutes")
    }

    fun cancel(screenType: ScreenType) {
        val existing = pendingIntent(screenType, 0, PendingIntent.FLAG_NO_CREATE)
        if (existing == null) {
            Log.d(TAG, "No $screenType alarm to cancel")
            return
        }
        alarmManager.cancel(existing)
        existing.cancel()
        Log.d(TAG, "Cancelled $screenType alarm")
    }

    fun cancelAll() = ScreenType.entries.forEach(::cancel)

    /** True when an alarm for [screenType] is currently registered with the platform. */
    fun isScheduled(screenType: ScreenType): Boolean =
        pendingIntent(screenType, 0, PendingIntent.FLAG_NO_CREATE) != null

    private fun setAlarm(screenType: ScreenType, intervalMinutes: Int, triggerAt: Long) {
        val operation = pendingIntent(
            screenType,
            intervalMinutes,
            PendingIntent.FLAG_UPDATE_CURRENT
        ) ?: return
        // Inexact alarms need no special permission and still fire while the device is idle.
        // The platform throttles them to roughly one per nine minutes in deep doze, which is why
        // the visible change cadence is exact and only the doze-time cadence is relaxed.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
    }

    private fun pendingIntent(screenType: ScreenType, intervalMinutes: Int, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            requestCode(screenType),
            Intent(context, StaticWallpaperAlarmReceiver::class.java)
                .setAction(Constants.ACTION_STATIC_ALARM)
                .putExtra(Constants.EXTRA_SCREEN_TYPE, screenType.name)
                .putExtra(Constants.EXTRA_INTERVAL_MINUTES, intervalMinutes),
            flags or PendingIntent.FLAG_IMMUTABLE
        )

    /** Stable per-screen request codes so cancelling one screen never clears another. */
    private fun requestCode(screenType: ScreenType): Int = when (screenType) {
        ScreenType.HOME -> REQUEST_CODE_HOME
        ScreenType.LOCK -> REQUEST_CODE_LOCK
        ScreenType.BOTH -> REQUEST_CODE_BOTH
        ScreenType.LIVE -> REQUEST_CODE_LIVE
    }

    private companion object {
        const val TAG = "StaticWallpaperAlarm"
        const val MINUTE_MILLIS = 60_000L
        const val REQUEST_CODE_HOME = 1001
        const val REQUEST_CODE_LOCK = 1002
        const val REQUEST_CODE_BOTH = 1003
        const val REQUEST_CODE_LIVE = 1004
    }
}

/** True when [intervalMinutes] is too short for WorkManager periodic work and needs an alarm. */
fun usesStaticAlarm(intervalMinutes: Int): Boolean =
    intervalMinutes in Constants.MIN_STATIC_INTERVAL_MINUTES until
        Constants.MIN_WORKMANAGER_INTERVAL_MINUTES
