package com.anthonyla.paperize.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.service.worker.WallpaperScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var wallpaperScheduler: WallpaperScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot completed, rescheduling wallpaper changes")

            val pendingResult = goAsync()

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    wallpaperScheduler.updateSchedules(
                        settingsRepository.getScheduleSettings(),
                        settingsRepository.getWallpaperMode(),
                        onlyIfNotScheduled = true
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling wallpaper changes", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
