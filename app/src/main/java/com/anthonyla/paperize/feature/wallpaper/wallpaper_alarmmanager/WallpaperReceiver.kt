package com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anthonyla.paperize.core.SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
import com.anthonyla.paperize.core.Type
import com.anthonyla.paperize.feature.wallpaper.wallpaper_service.HomeWallpaperService
import com.anthonyla.paperize.feature.wallpaper.wallpaper_service.LockWallpaperService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Receiver for the alarm manager to change wallpaper
 */
@AndroidEntryPoint
class WallpaperReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            if (intent?.getBooleanExtra("refresh", false) == true) {
                val serviceIntent = Intent(context, HomeWallpaperService::class.java).apply {
                    action = HomeWallpaperService.Actions.REFRESH.toString()
                }
                context.startService(serviceIntent)
                WallpaperAlarmSchedulerImpl(context).scheduleRefresh()
            }
            else {
                val timeInMinutes1 = intent?.getIntExtra("timeInMinutes1", WALLPAPER_CHANGE_INTERVAL_DEFAULT) ?: WALLPAPER_CHANGE_INTERVAL_DEFAULT
                val timeInMinutes2 = intent?.getIntExtra("timeInMinutes2", WALLPAPER_CHANGE_INTERVAL_DEFAULT) ?: WALLPAPER_CHANGE_INTERVAL_DEFAULT
                val scheduleSeparately = intent?.getBooleanExtra("scheduleSeparately", false) ?: false
                val type = intent?.getIntExtra("type", Type.SINGLE.ordinal) ?: Type.SINGLE.ordinal
                val setHome = intent?.getBooleanExtra("setHome", false) ?: false
                val setLock = intent?.getBooleanExtra("setLock", false) ?: false

                when (type) {
                    Type.SINGLE.ordinal -> {
                        if (setLock) startService(context, LockWallpaperService::class.java, LockWallpaperService.Actions.START.toString(), timeInMinutes1, timeInMinutes2, scheduleSeparately, type)
                        if (setHome) startService(context, HomeWallpaperService::class.java, HomeWallpaperService.Actions.START.toString(), timeInMinutes1, timeInMinutes2, scheduleSeparately, type)
                    }
                    Type.HOME.ordinal -> {
                        startService(context, HomeWallpaperService::class.java, HomeWallpaperService.Actions.START.toString(), timeInMinutes1, timeInMinutes2, scheduleSeparately, type)
                    }
                    Type.LOCK.ordinal -> {
                        startService(context, LockWallpaperService::class.java, LockWallpaperService.Actions.START.toString(), timeInMinutes1, timeInMinutes2, scheduleSeparately, type)
                    }
                }

                // Schedule next alarm for next wallpaper change
                val origin = intent?.getIntExtra("origin", -1)?.takeIf { it != -1 }
                WallpaperAlarmSchedulerImpl(context).scheduleWallpaperAlarm(
                    WallpaperAlarmItem(
                        homeInterval = timeInMinutes1,
                        lockInterval = timeInMinutes2,
                        scheduleSeparately = scheduleSeparately,
                        setHome = setHome,
                        setLock = setLock
                    ),
                    origin
                )
            }
        }
    }

    /**
     * Starts the service to change the wallpaper
     */
    private fun startService(context: Context, serviceClass: Class<*>, action: String, timeInMinutes1: Int? = null, timeInMinutes2: Int? = null, scheduleSeparately: Boolean? = null, type: Int? = null) {
        val serviceIntent = Intent(context, serviceClass).apply {
            this.action = action
            timeInMinutes1?.let { putExtra("timeInMinutes1", it) }
            timeInMinutes2?.let { putExtra("timeInMinutes2", it) }
            scheduleSeparately?.let { putExtra("scheduleSeparately", it) }
            type?.let { putExtra("type", it) }
        }
        context.startService(serviceIntent)
    }
}