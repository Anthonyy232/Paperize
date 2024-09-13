package com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anthonyla.paperize.core.Type
import com.anthonyla.paperize.feature.wallpaper.wallpaper_service.HomeWallpaperService
import com.anthonyla.paperize.feature.wallpaper.wallpaper_service.LockWallpaperService
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * This class is responsible for scheduling wallpaper alarms.
 * It uses the AlarmManager to schedule alarms for the [WallpaperReceiver].
 * It also uses [HomeWallpaperService] and [LockWallpaperService] to change the wallpaper due to receiver limitations.
 */
class WallpaperAlarmSchedulerImpl (
    private val context: Context,
): WallpaperAlarmScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules the wallpaper alarm based on the origin and changeImmediate
     */
    override fun scheduleWallpaperAlarm(
        wallpaperAlarmItem: WallpaperAlarmItem,
        origin: Int?,
        changeImmediate: Boolean,
        cancelImmediate: Boolean,
        setAlarm: Boolean,
    ) {
        // Cancel previous alarms before setting new ones to prevent stale alarms
        if (cancelImmediate) cancelWallpaperAlarm()
        else {
            when (origin) {
                Type.HOME.ordinal -> cancelAlarm(Type.HOME)
                Type.LOCK.ordinal -> cancelAlarm(Type.LOCK)
                null -> {
                    cancelAlarm(Type.HOME)
                    cancelAlarm(Type.LOCK)
                    cancelAlarm(Type.SINGLE)
                }
            }
        }

        // Schedule new wallpaper alarm
        if (wallpaperAlarmItem.scheduleSeparately) {
            when (origin) {
                Type.LOCK.ordinal -> {
                    if (changeImmediate) changeWallpaperImmediate(wallpaperAlarmItem, Type.LOCK)
                    if (setAlarm) scheduleWallpaper(wallpaperAlarmItem, Type.LOCK, origin)
                }
                Type.HOME.ordinal -> {
                    if (changeImmediate) changeWallpaperImmediate(wallpaperAlarmItem, Type.HOME)
                    if (setAlarm) scheduleWallpaper(wallpaperAlarmItem, Type.HOME, origin)
                }
                null -> {
                    if (changeImmediate) {
                        changeWallpaperImmediate(wallpaperAlarmItem, Type.LOCK)
                        changeWallpaperImmediate(wallpaperAlarmItem, Type.HOME)
                    }
                    if (setAlarm) {
                        scheduleWallpaper(wallpaperAlarmItem, Type.LOCK, Type.LOCK.ordinal)
                        scheduleWallpaper(wallpaperAlarmItem, Type.HOME, Type.HOME.ordinal)
                    }
                }
            }
        }
        else {
            if (changeImmediate) changeWallpaperImmediate(wallpaperAlarmItem, Type.SINGLE)
            if (setAlarm) scheduleWallpaper(wallpaperAlarmItem, Type.SINGLE)
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
            scheduleWallpaper(wallpaperAlarmItem, Type.SINGLE, null, true)
        }
    }

    /**
     * Update the wallpaper without scheduling alarms
     */
    override fun updateWallpaper(scheduleSeparately: Boolean, setHome: Boolean, setLock: Boolean) {
        if (scheduleSeparately) {
            updateWallpaper(Type.LOCK, setHome, setLock)
            updateWallpaper(Type.HOME, setHome, setLock)
        }
        else {
            updateWallpaper(Type.SINGLE, setHome, setLock)
        }
    }

    /**
     * Cancels all active alarms
     */
    override fun cancelWallpaperAlarm(cancelLock: Boolean, cancelHome: Boolean) {
        if (cancelLock) cancelAlarm(Type.LOCK)
        if (cancelHome) cancelAlarm(Type.HOME)
        if (cancelLock && cancelHome) {
            cancelAlarm(Type.SINGLE)
            cancelAlarm(Type.REFRESH)
        }
    }

    /**
     * Schedules the wallpaper alarm based on type and time
     */
    private fun scheduleWallpaper(wallpaperAlarmItem: WallpaperAlarmItem, type: Type, origin: Int? = null, update: Boolean = false) {
        val nextTime = when (type) {
            Type.LOCK -> LocalDateTime.now().plusMinutes(wallpaperAlarmItem.lockInterval.toLong())
            Type.HOME -> if (wallpaperAlarmItem.scheduleSeparately) LocalDateTime.now().plusMinutes(wallpaperAlarmItem.homeInterval.toLong()).plusSeconds(10) else LocalDateTime.now().plusMinutes(wallpaperAlarmItem.homeInterval.toLong())
            else -> LocalDateTime.now().plusMinutes(wallpaperAlarmItem.homeInterval.toLong())
        }
        val intent = Intent(context, WallpaperReceiver::class.java).apply {
            putExtra("homeInterval", wallpaperAlarmItem.homeInterval)
            putExtra("lockInterval", wallpaperAlarmItem.lockInterval)
            putExtra("scheduleSeparately", wallpaperAlarmItem.scheduleSeparately)
            putExtra("type", type.ordinal)
            putExtra("setHome", wallpaperAlarmItem.setHome)
            putExtra("setLock", wallpaperAlarmItem.setLock)
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
            when (type.ordinal) {
                Type.SINGLE.ordinal -> {
                    if (wallpaperAlarmItem.setLock) startService(context, LockWallpaperService::class.java, LockWallpaperService.Actions.REQUEUE.toString(), wallpaperAlarmItem.homeInterval, wallpaperAlarmItem.lockInterval, wallpaperAlarmItem.scheduleSeparately, Type.SINGLE.ordinal)
                    if (wallpaperAlarmItem.setHome) startService(context, HomeWallpaperService::class.java, HomeWallpaperService.Actions.REQUEUE.toString(), wallpaperAlarmItem.homeInterval, wallpaperAlarmItem.lockInterval, wallpaperAlarmItem.scheduleSeparately, Type.SINGLE.ordinal)
                }
                Type.HOME.ordinal -> {
                    startService(context, HomeWallpaperService::class.java, HomeWallpaperService.Actions.REQUEUE.toString(), wallpaperAlarmItem.homeInterval, wallpaperAlarmItem.lockInterval, wallpaperAlarmItem.scheduleSeparately, Type.HOME.ordinal)
                }
                Type.LOCK.ordinal -> {
                    startService(context, LockWallpaperService::class.java, LockWallpaperService.Actions.REQUEUE.toString(), wallpaperAlarmItem.homeInterval, wallpaperAlarmItem.lockInterval, wallpaperAlarmItem.scheduleSeparately, Type.LOCK.ordinal)
                }
            }
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
        when (type.ordinal) {
            Type.SINGLE.ordinal -> {
                if (wallpaperAlarmItem.setLock) startService(context, LockWallpaperService::class.java, LockWallpaperService.Actions.START.toString(), wallpaperAlarmItem.homeInterval, wallpaperAlarmItem.lockInterval, wallpaperAlarmItem.scheduleSeparately, Type.SINGLE.ordinal)
                if (wallpaperAlarmItem.setHome) startService(context, HomeWallpaperService::class.java, HomeWallpaperService.Actions.START.toString(), wallpaperAlarmItem.homeInterval, wallpaperAlarmItem.lockInterval, wallpaperAlarmItem.scheduleSeparately, Type.SINGLE.ordinal)
            }
            Type.LOCK.ordinal -> {
                startService(context, LockWallpaperService::class.java, LockWallpaperService.Actions.START.toString(), wallpaperAlarmItem.homeInterval, wallpaperAlarmItem.lockInterval, wallpaperAlarmItem.scheduleSeparately, Type.LOCK.ordinal)
            }
            Type.HOME.ordinal -> {
                startService(context, HomeWallpaperService::class.java, HomeWallpaperService.Actions.START.toString(), wallpaperAlarmItem.homeInterval, wallpaperAlarmItem.lockInterval, wallpaperAlarmItem.scheduleSeparately, Type.HOME.ordinal)
            }
        }
    }

    /**
     * Updates the wallpaper without changing the alarm
     */
    private fun updateWallpaper(type: Type, setHome: Boolean, setLock: Boolean) {
        when (type.ordinal) {
            Type.SINGLE.ordinal -> {
                if (setLock) startService(context, LockWallpaperService::class.java, LockWallpaperService.Actions.UPDATE.toString())
                if (setHome) startService(context, HomeWallpaperService::class.java, HomeWallpaperService.Actions.UPDATE.toString())
            }
            Type.LOCK.ordinal -> {
                startService(context, LockWallpaperService::class.java, LockWallpaperService.Actions.UPDATE.toString())
            }
            Type.HOME.ordinal -> {
                startService(context, HomeWallpaperService::class.java, HomeWallpaperService.Actions.UPDATE.toString())
            }
        }
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

    /**
     * Starts the service to change the wallpaper
     */
    private fun startService(context: Context, serviceClass: Class<*>, action: String, homeInterval: Int? = null, lockInterval: Int? = null, scheduleSeparately: Boolean? = null, type: Int? = null) {
        val serviceIntent = Intent(context, serviceClass).apply {
            this.action = action
            homeInterval?.let { putExtra("homeInterval", it) }
            lockInterval?.let { putExtra("lockInterval", it) }
            scheduleSeparately?.let { putExtra("scheduleSeparately", it) }
            type?.let { putExtra("type", it) }
        }
        context.startService(serviceIntent)
    }
}