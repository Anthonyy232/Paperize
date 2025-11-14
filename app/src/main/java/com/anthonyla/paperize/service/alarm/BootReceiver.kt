package com.anthonyla.paperize.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.anthonyla.paperize.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Broadcast receiver to reschedule alarms on device boot
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var wallpaperAlarmScheduler: WallpaperAlarmScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot completed, rescheduling alarms")

            scope.launch {
                try {
                    val settings = settingsRepository.getScheduleSettings()

                    if (settings.enableChanger) {
                        // Reschedule wallpaper change alarms
                        if (settings.separateSchedules) {
                            wallpaperAlarmScheduler.scheduleWallpaperChange(
                                com.anthonyla.paperize.core.ScreenType.HOME,
                                settings.homeIntervalMinutes,
                                if (settings.useStartTime) settings.scheduleStartTime else null
                            )
                            wallpaperAlarmScheduler.scheduleWallpaperChange(
                                com.anthonyla.paperize.core.ScreenType.LOCK,
                                settings.lockIntervalMinutes,
                                if (settings.useStartTime) settings.scheduleStartTime else null
                            )
                        } else {
                            wallpaperAlarmScheduler.scheduleWallpaperChange(
                                com.anthonyla.paperize.core.ScreenType.BOTH,
                                settings.homeIntervalMinutes,
                                if (settings.useStartTime) settings.scheduleStartTime else null
                            )
                        }

                        // Schedule refresh alarm
                        wallpaperAlarmScheduler.scheduleRefreshAlarm()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms", e)
                }
            }
        }
    }
}
