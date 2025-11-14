package com.anthonyla.paperize.service.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for wallpaper change alarms
 */
@Singleton
class WallpaperAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "WallpaperAlarmScheduler"
    }

    /**
     * Schedule an alarm to change wallpaper
     *
     * @param screenType HOME, LOCK, or BOTH
     * @param intervalMinutes Interval between changes in minutes
     * @param startTime Optional start time (HH:mm format)
     */
    fun scheduleWallpaperChange(
        screenType: ScreenType,
        intervalMinutes: Int,
        startTime: String? = null
    ) {
        val triggerTime = calculateTriggerTime(intervalMinutes, startTime)

        val intent = Intent(context, WallpaperAlarmReceiver::class.java).apply {
            action = Constants.ACTION_CHANGE_WALLPAPER
            putExtra(Constants.EXTRA_SCREEN_TYPE, screenType.name)
        }

        val requestCode = when (screenType) {
            ScreenType.HOME -> Constants.ALARM_REQUEST_CODE_HOME
            ScreenType.LOCK -> Constants.ALARM_REQUEST_CODE_LOCK
            ScreenType.BOTH -> Constants.ALARM_REQUEST_CODE_HOME
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d(TAG, "Scheduled alarm for $screenType at ${triggerTime - System.currentTimeMillis()} ms from now")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for exact alarms, using inexact alarm", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /**
     * Cancel scheduled alarm
     */
    fun cancelWallpaperChange(screenType: ScreenType) {
        val intent = Intent(context, WallpaperAlarmReceiver::class.java)

        val requestCode = when (screenType) {
            ScreenType.HOME -> Constants.ALARM_REQUEST_CODE_HOME
            ScreenType.LOCK -> Constants.ALARM_REQUEST_CODE_LOCK
            ScreenType.BOTH -> Constants.ALARM_REQUEST_CODE_HOME
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            Log.d(TAG, "Cancelled alarm for $screenType")
        }
    }

    /**
     * Schedule daily refresh alarm
     */
    fun scheduleRefreshAlarm() {
        val intent = Intent(context, WallpaperAlarmReceiver::class.java).apply {
            action = Constants.ACTION_REFRESH_ALBUM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.ALARM_REQUEST_CODE_REFRESH,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule for midnight
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            // If midnight has passed, schedule for next day
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d(TAG, "Scheduled daily refresh alarm")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling refresh alarm", e)
        }
    }

    /**
     * Cancel all alarms
     */
    fun cancelAllAlarms() {
        cancelWallpaperChange(ScreenType.HOME)
        cancelWallpaperChange(ScreenType.LOCK)

        val refreshIntent = Intent(context, WallpaperAlarmReceiver::class.java)
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.ALARM_REQUEST_CODE_REFRESH,
            refreshIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        refreshPendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        Log.d(TAG, "Cancelled all alarms")
    }

    private fun calculateTriggerTime(intervalMinutes: Int, startTime: String?): Long {
        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance()

        if (startTime != null) {
            // Parse start time (HH:mm)
            val parts = startTime.split(":")
            if (parts.size == 2) {
                try {
                    val hour = parts[0].toInt()
                    val minute = parts[1].toInt()

                    trigger.set(Calendar.HOUR_OF_DAY, hour)
                    trigger.set(Calendar.MINUTE, minute)
                    trigger.set(Calendar.SECOND, 0)

                    // If start time has passed today, schedule for tomorrow
                    if (trigger.before(now)) {
                        trigger.add(Calendar.DAY_OF_MONTH, 1)
                    }

                    return trigger.timeInMillis
                } catch (e: NumberFormatException) {
                    Log.e(TAG, "Invalid start time format: $startTime", e)
                }
            }
        }

        // No start time, schedule based on interval
        trigger.add(Calendar.MINUTE, intervalMinutes)
        return trigger.timeInMillis
    }
}
