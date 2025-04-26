package com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.core.SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
import com.anthonyla.paperize.core.Type
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.wallpaper_service.HomeWallpaperService
import com.anthonyla.paperize.feature.wallpaper.wallpaper_service.LockWallpaperService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receiver for the alarm manager to change wallpaper
 */
@AndroidEntryPoint
class WallpaperReceiver : BroadcastReceiver() {
    @Inject
    lateinit var settingsDataStoreImpl: SettingsDataStore
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val refresh = intent?.getBooleanExtra("refresh", false)
            if (refresh == true) {
                val serviceIntent = Intent(context, HomeWallpaperService::class.java).apply {
                    action = HomeWallpaperService.Actions.REFRESH.toString()
                }
                context.startService(serviceIntent)
            }
            else {
                val homeInterval = intent?.getIntExtra("homeInterval", WALLPAPER_CHANGE_INTERVAL_DEFAULT) ?: WALLPAPER_CHANGE_INTERVAL_DEFAULT
                val lockInterval = intent?.getIntExtra("lockInterval", WALLPAPER_CHANGE_INTERVAL_DEFAULT) ?: WALLPAPER_CHANGE_INTERVAL_DEFAULT
                val scheduleSeparately = intent?.getBooleanExtra("scheduleSeparately", false) ?: false
                val type = intent?.getIntExtra("type", Type.SINGLE.ordinal) ?: Type.SINGLE.ordinal
                val setHome = intent?.getBooleanExtra("setHome", false) ?: false
                val setLock = intent?.getBooleanExtra("setLock", false) ?: false
                val changeStartTime = intent?.getBooleanExtra("changeStartTime", false) ?: false
                val startTime = intent?.getIntArrayExtra("startTime") ?: intArrayOf(0, 0)

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
                CoroutineScope(Dispatchers.IO).launch {
                    WallpaperAlarmSchedulerImpl(context).scheduleWallpaperAlarm(
                        wallpaperAlarmItem = WallpaperAlarmItem(
                            homeInterval = homeInterval,
                            lockInterval = lockInterval,
                            scheduleSeparately = scheduleSeparately,
                            setHome = setHome,
                            setLock = setLock,
                            changeStartTime = changeStartTime,
                            startTime = Pair(startTime[0], startTime[1]),
                        ),
                        origin = origin,
                        homeNextTime = settingsDataStoreImpl.getString(SettingsConstants.HOME_NEXT_SET_TIME),
                        lockNextTime = settingsDataStoreImpl.getString(SettingsConstants.LOCK_NEXT_SET_TIME),
                    )
                }
            }
        }
    }

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