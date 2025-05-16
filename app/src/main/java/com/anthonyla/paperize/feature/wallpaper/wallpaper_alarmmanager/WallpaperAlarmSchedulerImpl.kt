package com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.anthonyla.paperize.core.Type
import com.anthonyla.paperize.feature.wallpaper.wallpaper_service.HomeWallpaperService
import com.anthonyla.paperize.feature.wallpaper.wallpaper_service.LockWallpaperService
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

data class ServiceConfig(
    val homeInterval: Int? = null,
    val lockInterval: Int? = null,
    val scheduleSeparately: Boolean? = null,
    val type: Int? = null
)

sealed class WallpaperAction {
    data object START : WallpaperAction()
    data object UPDATE : WallpaperAction()
}

/**
 * This class is responsible for scheduling wallpaper alarms.
 * It uses the AlarmManager to schedule alarms for the [WallpaperReceiver].
 * It also uses [HomeWallpaperService] and [LockWallpaperService] to change the wallpaper due to receiver limitations.
 */
class WallpaperAlarmSchedulerImpl @Inject constructor(
    private val context: Context
): WallpaperAlarmScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules the wallpaper alarm based on the origin and changeImmediate
     */
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override suspend fun scheduleWallpaperAlarm(
        wallpaperAlarmItem: WallpaperAlarmItem,
        origin: Int?,
        changeImmediate: Boolean,
        cancelImmediate: Boolean,
        setAlarm: Boolean,
        firstLaunch: Boolean,
        homeNextTime: String?,
        lockNextTime: String?,
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
                    if (setAlarm) scheduleWallpaper(wallpaperAlarmItem, Type.LOCK, origin, firstLaunch, homeNextTime, lockNextTime)
                }
                Type.HOME.ordinal -> {
                    if (changeImmediate) changeWallpaperImmediate(wallpaperAlarmItem, Type.HOME)
                    if (setAlarm) scheduleWallpaper(wallpaperAlarmItem, Type.HOME, origin, firstLaunch, homeNextTime, lockNextTime)
                }
                null -> {
                    if (changeImmediate) {
                        changeWallpaperImmediate(wallpaperAlarmItem, Type.LOCK)
                        changeWallpaperImmediate(wallpaperAlarmItem, Type.HOME)
                    }
                    if (setAlarm) {
                        scheduleWallpaper(wallpaperAlarmItem, Type.LOCK, Type.LOCK.ordinal, firstLaunch, homeNextTime, lockNextTime)
                        scheduleWallpaper(wallpaperAlarmItem, Type.HOME, Type.HOME.ordinal, firstLaunch, homeNextTime, lockNextTime)
                    }
                }
            }
        }
        else {
            if (changeImmediate) changeWallpaperImmediate(wallpaperAlarmItem, Type.SINGLE)
            if (setAlarm) scheduleWallpaper(wallpaperAlarmItem, Type.SINGLE, null, firstLaunch, homeNextTime, lockNextTime)
        }
    }

    /**
     * Update the alarms
     */
    override fun updateWallpaperAlarm(wallpaperAlarmItem: WallpaperAlarmItem, firstLaunch: Boolean) {
        cancelWallpaperAlarm()
        if (wallpaperAlarmItem.scheduleSeparately) {
            scheduleWallpaper(wallpaperAlarmItem, Type.LOCK, null, firstLaunch, "", "")
            scheduleWallpaper(wallpaperAlarmItem, Type.HOME, null, firstLaunch, "", "")
        }
        else {
            scheduleWallpaper(wallpaperAlarmItem, Type.SINGLE, null, firstLaunch, "", "")
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
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private fun scheduleWallpaper(wallpaperAlarmItem: WallpaperAlarmItem, type: Type, origin: Int? = null, firstLaunch: Boolean = false, homeNextTime: String? = null, lockNextTime: String? = null) {
        try {
            val nextTime = calculateNextAlarmTime(wallpaperAlarmItem, type, firstLaunch, homeNextTime ?: "", lockNextTime ?: "")
            val intent = createWallpaperIntent(wallpaperAlarmItem, type, origin)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    cancelWallpaperAlarm()
                    return
                }
            }
            
            scheduleExactAlarm(nextTime, intent)
        } catch (e: Exception) {
            Log.e("WallpaperAlarmScheduler", "Error scheduling wallpaper alarm: ${e.message}")
            cancelWallpaperAlarm()
        }
    }

    private fun calculateNextAlarmTime(wallpaperAlarmItem: WallpaperAlarmItem, type: Type, firstLaunch: Boolean, homeNextTime: String, lockNextTime: String): LocalDateTime {
        val startTime = LocalDateTime.now()

        if (firstLaunch) {
            if (wallpaperAlarmItem.changeStartTime) {
                return startTime.withHour(wallpaperAlarmItem.startTime.first)
                    .withMinute(wallpaperAlarmItem.startTime.second)
                    .let { if (it.isBefore(startTime)) it.plusDays(1) else it }
                    .withSecond(0)
                    .withNano(0)
            }
            return when {
                type == Type.LOCK && wallpaperAlarmItem.scheduleSeparately ->
                    startTime.plusMinutes(wallpaperAlarmItem.lockInterval.toLong()).withSecond(0).withNano(0)
                type == Type.HOME && wallpaperAlarmItem.scheduleSeparately ->
                    startTime.plusMinutes(wallpaperAlarmItem.homeInterval.toLong()).withSecond(0).withNano(0).plusSeconds(10)
                else -> startTime.plusMinutes(wallpaperAlarmItem.homeInterval.toLong()).withSecond(0).withNano(0)
            }
        }
        else {
            fun calculateFutureTime(baseTime: LocalDateTime, intervalMinutes: Long, addSeconds: Int = 0, depth: Int = 0): LocalDateTime {
                if (depth > 100) {
                    return startTime.plusMinutes(intervalMinutes)
                }

                var nextTime = baseTime.plusMinutes(intervalMinutes).withSecond(0).withNano(0)
                if (addSeconds > 0) nextTime = nextTime.plusSeconds(addSeconds.toLong())

                return if (nextTime.isBefore(startTime)) {
                    calculateFutureTime(nextTime, intervalMinutes, addSeconds, depth + 1)
                } else nextTime
            }

            val nextTime = when {
                type == Type.LOCK && wallpaperAlarmItem.scheduleSeparately -> {
                    val baseTime = try {
                        if (lockNextTime.isEmpty()) startTime else LocalDateTime.parse(lockNextTime)
                    } catch (e: Exception) {
                        startTime
                    }
                    calculateFutureTime(baseTime, wallpaperAlarmItem.lockInterval.toLong())
                }
                type == Type.HOME && wallpaperAlarmItem.scheduleSeparately -> {
                    val baseTime = try {
                        if (homeNextTime.isEmpty()) startTime else LocalDateTime.parse(homeNextTime)
                    } catch (e: Exception) {
                        startTime
                    }
                    calculateFutureTime(baseTime, wallpaperAlarmItem.homeInterval.toLong(), 10)
                }
                else -> {
                    val baseTime = try {
                        if (homeNextTime.isEmpty()) startTime else LocalDateTime.parse(homeNextTime)
                    } catch (e: Exception) {
                        startTime
                    }
                    calculateFutureTime(baseTime, wallpaperAlarmItem.homeInterval.toLong())
                }
            }
            return nextTime.withSecond(0).withNano(0)
        }
    }

    private fun createWallpaperIntent(wallpaperAlarmItem: WallpaperAlarmItem, type: Type, origin: Int?): Intent {
        return Intent(context, WallpaperReceiver::class.java).apply {
            putExtra("homeInterval", wallpaperAlarmItem.homeInterval)
            putExtra("lockInterval", wallpaperAlarmItem.lockInterval)
            putExtra("setHome", wallpaperAlarmItem.setHome)
            putExtra("setLock", wallpaperAlarmItem.setLock)
            putExtra("scheduleSeparately", wallpaperAlarmItem.scheduleSeparately)
            putExtra("origin", origin)
            putExtra("type", type.ordinal)
            putExtra("changeStartTime", wallpaperAlarmItem.changeStartTime)
        }
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private fun scheduleExactAlarm(nextTime: LocalDateTime, intent: Intent) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            intent.getIntExtra("type", 0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
            pendingIntent
        )
    }

    /**
     * Schedules a refresh alarm every 24hrs
     */
    fun scheduleRefresh(refresh: Boolean = true) {
        cancelAlarm(Type.REFRESH)
        if (refresh) {
            val intent = Intent(context, WallpaperReceiver::class.java).apply {
                putExtra("refresh", true)
            }
            val nextMidnight = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                nextMidnight.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                AlarmManager.INTERVAL_DAY,
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
                if (wallpaperAlarmItem.setLock) startService(context, LockWallpaperService::class.java, WallpaperAction.START, ServiceConfig(wallpaperAlarmItem.homeInterval, wallpaperAlarmItem.lockInterval, wallpaperAlarmItem.scheduleSeparately, Type.SINGLE.ordinal))
                if (wallpaperAlarmItem.setHome) startService(context, HomeWallpaperService::class.java, WallpaperAction.START, ServiceConfig(wallpaperAlarmItem.homeInterval, wallpaperAlarmItem.lockInterval, wallpaperAlarmItem.scheduleSeparately, Type.SINGLE.ordinal))
            }
            Type.LOCK.ordinal -> {
                startService(context, LockWallpaperService::class.java, WallpaperAction.START, ServiceConfig(wallpaperAlarmItem.homeInterval, wallpaperAlarmItem.lockInterval, wallpaperAlarmItem.scheduleSeparately, Type.LOCK.ordinal))
            }
            Type.HOME.ordinal -> {
                startService(context, HomeWallpaperService::class.java, WallpaperAction.START, ServiceConfig(wallpaperAlarmItem.homeInterval, wallpaperAlarmItem.lockInterval, wallpaperAlarmItem.scheduleSeparately, Type.HOME.ordinal))
            }
        }
    }

    /**
     * Updates the wallpaper without changing the alarm
     */
    private fun updateWallpaper(type: Type, setHome: Boolean, setLock: Boolean) {
        when (type.ordinal) {
            Type.SINGLE.ordinal -> {
                if (setLock) startService(context, LockWallpaperService::class.java, WallpaperAction.UPDATE)
                if (setHome) startService(context, HomeWallpaperService::class.java, WallpaperAction.UPDATE)
            }
            Type.LOCK.ordinal -> {
                startService(context, LockWallpaperService::class.java, WallpaperAction.UPDATE)
            }
            Type.HOME.ordinal -> {
                startService(context, HomeWallpaperService::class.java, WallpaperAction.UPDATE)
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
    private fun startService(
        context: Context, 
        serviceClass: Class<*>, 
        action: WallpaperAction,
        config: ServiceConfig? = null
    ) {
        val serviceIntent = Intent(context, serviceClass).apply {
            this.action = action::class.java.simpleName
            config?.let {
                it.homeInterval?.let { interval -> putExtra("homeInterval", interval) }
                it.lockInterval?.let { interval -> putExtra("lockInterval", interval) }
                it.scheduleSeparately?.let { separate -> putExtra("scheduleSeparately", separate) }
                it.type?.let { type -> putExtra("type", type) }
            }
        }
        context.startService(serviceIntent)
    }
}