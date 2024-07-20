package com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anthonyla.paperize.core.Type
import com.anthonyla.paperize.feature.wallpaper.wallpaper_service.WallpaperService1
import com.anthonyla.paperize.feature.wallpaper.wallpaper_service.WallpaperService2
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * This class is responsible for scheduling wallpaper alarms.
 * It uses the AlarmManager to schedule alarms for the WallpaperReceiver.
 * It also uses the WallpaperService to change the wallpaper due to receiver limitations.
 */
class WallpaperAlarmSchedulerImpl (
    private val context: Context,
): WallpaperAlarmScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules the wallpaper alarm based on the origin and changeImmediate
     */
    override fun scheduleWallpaperAlarm(wallpaperAlarmItem: WallpaperAlarmItem, origin: Int?, changeImmediate: Boolean, cancelImmediate: Boolean) {
        if (cancelImmediate) cancelWallpaperAlarm()
        else {
            when(origin) {
                Type.HOME.ordinal -> cancelAlarm(Type.HOME)
                Type.LOCK.ordinal -> cancelAlarm(Type.LOCK)
                null -> {
                    cancelAlarm(Type.HOME)
                    cancelAlarm(Type.LOCK)
                    cancelAlarm(Type.BOTH)
                }
            }
        }
        if (wallpaperAlarmItem.scheduleSeparately) {
            when (origin) {
                Type.LOCK.ordinal -> {
                    if (changeImmediate) changeWallpaperImmediate(wallpaperAlarmItem, Type.LOCK)
                    scheduleWallpaper(wallpaperAlarmItem, Type.LOCK, origin)
                }
                Type.HOME.ordinal -> {
                    if (changeImmediate) changeWallpaperImmediate(wallpaperAlarmItem, Type.HOME)
                    scheduleWallpaper(wallpaperAlarmItem, Type.HOME, origin)
                }
                null -> {
                    if (changeImmediate) {
                        changeWallpaperImmediate(wallpaperAlarmItem, Type.LOCK)
                        changeWallpaperImmediate(wallpaperAlarmItem, Type.HOME)
                    }
                    scheduleWallpaper(wallpaperAlarmItem, Type.LOCK, Type.LOCK.ordinal)
                    scheduleWallpaper(wallpaperAlarmItem, Type.HOME, Type.HOME.ordinal)
                }
            }
        }
        else {
            if (changeImmediate) changeWallpaperImmediate(wallpaperAlarmItem, Type.BOTH)
            scheduleWallpaper(wallpaperAlarmItem, Type.BOTH)
        }
    }

    /**
     * Update the alarms
     */
    override fun updateWallpaperAlarm(wallpaperAlarmItem: WallpaperAlarmItem) {
        cancelWallpaperAlarm()
        if (wallpaperAlarmItem.scheduleSeparately) {
            scheduleWallpaper(wallpaperAlarmItem, Type.LOCK, null, true)
            scheduleWallpaper(wallpaperAlarmItem, Type.HOME, null, true)
        }
        else {
            scheduleWallpaper(wallpaperAlarmItem, Type.BOTH, null, true)
        }
    }

    /**
     * Update the wallpaper without scheduling alarms
     */
    override fun updateWallpaper(scheduleSeparately: Boolean) {
        if (scheduleSeparately) {
            updateWallpaper(Type.LOCK)
            updateWallpaper(Type.HOME)
        }
        else {
            updateWallpaper(Type.BOTH)
        }
    }

    /**
     * Cancels all active alarms
     */
    override fun cancelWallpaperAlarm() {
        cancelAlarm(Type.HOME)
        cancelAlarm(Type.LOCK)
        cancelAlarm(Type.BOTH)
        cancelAlarm(Type.REFRESH)
    }

    /**
     * Schedules the wallpaper alarm based on type and time
     */
    private fun scheduleWallpaper(wallpaperAlarmItem: WallpaperAlarmItem, type: Type, origin: Int? = null, update: Boolean = false) {
        val nextTime = when (type) {
            Type.HOME, Type.BOTH -> LocalDateTime.now().plusMinutes(wallpaperAlarmItem.timeInMinutes1.toLong())
            Type.LOCK -> LocalDateTime.now().plusMinutes(wallpaperAlarmItem.timeInMinutes2.toLong())
            else -> LocalDateTime.now()
        }
        val intent = Intent(context, WallpaperReceiver::class.java).apply {
            putExtra("timeInMinutes1", wallpaperAlarmItem.timeInMinutes1)
            putExtra("timeInMinutes2", wallpaperAlarmItem.timeInMinutes2)
            putExtra("scheduleSeparately", wallpaperAlarmItem.scheduleSeparately)
            putExtra("type", type.ordinal)
            putExtra("origin", origin)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                cancelWallpaperAlarm()
            }
            else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                        PendingIntent.getBroadcast(
                            context,
                            type.ordinal,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                        )
                    )
            }
        }
        else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                PendingIntent.getBroadcast(
                    context,
                    type.ordinal,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            )
        }
        if (update) {
            val serviceIntent = Intent().apply {
                putExtra("timeInMinutes1", wallpaperAlarmItem.timeInMinutes1)
                putExtra("timeInMinutes2", wallpaperAlarmItem.timeInMinutes2)
                putExtra("scheduleSeparately", wallpaperAlarmItem.scheduleSeparately)
                putExtra("type", type.ordinal)
            }
            if (type == Type.BOTH || type == Type.HOME) {
                serviceIntent.setClass(context, WallpaperService1::class.java).apply {
                    action = WallpaperService1.Actions.REQUEUE.toString()
                }
            } else {
                serviceIntent.setClass(context, WallpaperService2::class.java).apply {
                    action = WallpaperService2.Actions.REQUEUE.toString()
                }
            }
            context.startService(serviceIntent)
        }
    }

    /**
     * Schedules a refresh alarm every 24hrs
     */
    fun scheduleRefresh() {
        cancelAlarm(Type.REFRESH)
        val intent = Intent(context, WallpaperReceiver::class.java).apply {
            putExtra("refresh", true)
        }
        val nextMidnight = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                cancelWallpaperAlarm()
            }
            else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextMidnight.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                    PendingIntent.getBroadcast(
                        context,
                        Type.REFRESH.ordinal,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                )
            }
        }
        else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                nextMidnight.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                PendingIntent.getBroadcast(
                    context,
                    Type.REFRESH.ordinal,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            )
        }
    }

    /**
     * Use the service to change the wallpaper
     */
    private fun changeWallpaperImmediate(wallpaperAlarmItem: WallpaperAlarmItem, type: Type) {
        val serviceIntent = Intent().apply {
            putExtra("timeInMinutes1", wallpaperAlarmItem.timeInMinutes1)
            putExtra("timeInMinutes2", wallpaperAlarmItem.timeInMinutes2)
            putExtra("scheduleSeparately", wallpaperAlarmItem.scheduleSeparately)
            putExtra("type", type.ordinal)
        }
        if (type == Type.BOTH || type == Type.HOME) {
            serviceIntent.setClass(context, WallpaperService1::class.java).apply {
                action = WallpaperService1.Actions.START.toString()
            }
        } else {
            serviceIntent.setClass(context, WallpaperService2::class.java).apply {
                action = WallpaperService2.Actions.START.toString()
            }
        }
        context.startService(serviceIntent)
    }

    /**
     * Updates the wallpaper without changing the alarm
     */
    private fun updateWallpaper(type: Type) {
        val serviceIntent = Intent()
        if (type == Type.BOTH || type == Type.HOME) {
            serviceIntent.setClass(context, WallpaperService1::class.java).apply {
                action = WallpaperService1.Actions.UPDATE.toString()
            }
        } else {
            serviceIntent.setClass(context, WallpaperService2::class.java).apply {
                action = WallpaperService2.Actions.UPDATE.toString()
            }
        }
        context.startService(serviceIntent)
    }

    /**
     * Cancels the alarm based on type
     */
    private fun cancelAlarm(type: Type) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type.ordinal,
            Intent(context, WallpaperReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}