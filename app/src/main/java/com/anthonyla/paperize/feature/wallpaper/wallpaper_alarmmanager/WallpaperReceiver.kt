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
                val homeInterval = intent?.getIntExtra("homeInterval", WALLPAPER_CHANGE_INTERVAL_DEFAULT) ?: WALLPAPER_CHANGE_INTERVAL_DEFAULT
                val lockInterval = intent?.getIntExtra("lockInterval", WALLPAPER_CHANGE_INTERVAL_DEFAULT) ?: WALLPAPER_CHANGE_INTERVAL_DEFAULT
                val scheduleSeparately = intent?.getBooleanExtra("scheduleSeparately", false) ?: false
                val type = intent?.getIntExtra("type", Type.SINGLE.ordinal) ?: Type.SINGLE.ordinal
                val setHome = intent?.getBooleanExtra("setHome", false) ?: false
                val setLock = intent?.getBooleanExtra("setLock", false) ?: false

                when (type) {
                    Type.SINGLE.ordinal -> {
                        if (setLock) startService(context, LockWallpaperService::class.java, LockWallpaperService.Actions.START.toString(), homeInterval, lockInterval, scheduleSeparately, type)
                        if (setHome) startService(context, HomeWallpaperService::class.java, HomeWallpaperService.Actions.START.toString(), homeInterval, lockInterval, scheduleSeparately, type)
                    }
                    Type.HOME.ordinal -> {
                        startService(context, HomeWallpaperService::class.java, HomeWallpaperService.Actions.START.toString(), homeInterval, lockInterval, scheduleSeparately, type)
                    }
                    Type.LOCK.ordinal -> {
                        startService(context, LockWallpaperService::class.java, LockWallpaperService.Actions.START.toString(), homeInterval, lockInterval, scheduleSeparately, type)
                    }
                }

                // Schedule next alarm for next wallpaper change
                val origin = intent?.getIntExtra("origin", -1)?.takeIf { it != -1 }
                WallpaperAlarmSchedulerImpl(context).scheduleWallpaperAlarm(
                    WallpaperAlarmItem(
                        homeInterval = homeInterval,
                        lockInterval = lockInterval,
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