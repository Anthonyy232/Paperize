package com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@AndroidEntryPoint
class WallpaperReceiver : BroadcastReceiver() {
    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    companion object {
        private const val TAG = "WallpaperReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive triggered.")
        if (context != null && intent != null) {
            val refresh = intent.getBooleanExtra("refresh", false)
            if (refresh) {
                val serviceIntent = Intent(context, HomeWallpaperService::class.java).apply {
                    action = HomeWallpaperService.Actions.REFRESH.toString()
                }
                context.startForegroundService(serviceIntent)
            } else {
                Log.d(TAG, "Regular wallpaper change alarm received.")
                val homeInterval = intent.getIntExtra("homeInterval", WALLPAPER_CHANGE_INTERVAL_DEFAULT)
                val lockInterval = intent.getIntExtra("lockInterval", WALLPAPER_CHANGE_INTERVAL_DEFAULT)
                val scheduleSeparately = intent.getBooleanExtra("scheduleSeparately", false)
                val type = intent.getIntExtra("type", Type.SINGLE.ordinal)
                val setHome = intent.getBooleanExtra("setHome", false)
                val setLock = intent.getBooleanExtra("setLock", false)
                val changeStartTime = intent.getBooleanExtra("changeStartTime", false)
                val startTime = intent.getIntArrayExtra("startTime") ?: intArrayOf(0, 0)

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

                val origin = intent.getIntExtra("origin", -1).takeIf { it != -1 }
                Log.d(TAG, "Rescheduling next alarm from receiver with origin: $origin")
                CoroutineScope(Dispatchers.IO).launch {
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    val currentTime = LocalDateTime.now()
                    settingsDataStore.putString(SettingsConstants.LAST_SET_TIME, currentTime.format(formatter))


                    val homeNext = settingsDataStore.getString(SettingsConstants.HOME_NEXT_SET_TIME)
                    val lockNext = settingsDataStore.getString(SettingsConstants.LOCK_NEXT_SET_TIME)

                    val alarmItem = WallpaperAlarmItem(
                        homeInterval = homeInterval,
                        lockInterval = lockInterval,
                        scheduleSeparately = scheduleSeparately,
                        setHome = setHome,
                        setLock = setLock,
                        changeStartTime = changeStartTime,
                        startTime = Pair(startTime[0], startTime[1]),
                    )

                    val scheduler = WallpaperAlarmSchedulerImpl(context, settingsDataStore)
                    scheduler.scheduleWallpaperAlarm(
                        wallpaperAlarmItem = alarmItem,
                        origin = origin,
                        changeImmediate = false,
                        cancelImmediate = false,
                        setAlarm = true,
                        firstLaunch = false,
                        homeNextTime = homeNext,
                        lockNextTime = lockNext,
                    )
                }
            }
        }
    }

    private fun startService(context: Context, serviceClass: Class<*>, action: String, homeInterval: Int?, lockInterval: Int?, scheduleSeparately: Boolean?, type: Int?) {
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