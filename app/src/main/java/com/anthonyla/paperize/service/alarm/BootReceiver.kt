package com.anthonyla.paperize.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.service.worker.WallpaperScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Broadcast receiver to reschedule wallpaper changes on device boot
 *
 * Uses WorkManager for scheduling instead of AlarmManager
 * Uses goAsync() to ensure coroutine completes before receiver is destroyed
 */
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

            // Use goAsync() to extend receiver lifecycle for async work
            // This prevents the system from killing the receiver before coroutine completes
            val pendingResult = goAsync()

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val settings = settingsRepository.getScheduleSettings()

                    if (settings.enableChanger) {
                        // Get wallpaper mode to determine scheduling type
                        val wallpaperMode = settingsRepository.getWallpaperMode()
                        
                        if (wallpaperMode == com.anthonyla.paperize.core.WallpaperMode.LIVE) {
                            // LIVE mode: schedule live wallpaper changes
                            if (settings.liveAlbumId != null && settings.liveIntervalMinutes > 0) {
                                wallpaperScheduler.scheduleWallpaperChange(
                                    ScreenType.LIVE,
                                    settings.liveIntervalMinutes
                                )
                                Log.d(TAG, "Live wallpaper changes scheduled on boot")
                            } else {
                                Log.d(TAG, "Live mode but no album or interval, not scheduling")
                            }
                        } else {
                            // STATIC mode: existing logic
                            // Check if we have all required albums before scheduling
                            val homeActive = settings.homeEnabled && settings.homeAlbumId != null
                            val lockActive = settings.lockEnabled && settings.lockAlbumId != null
                            val hasRequiredAlbums = when {
                                settings.homeEnabled && settings.lockEnabled -> homeActive && lockActive
                                settings.homeEnabled -> homeActive
                                settings.lockEnabled -> lockActive
                                else -> false
                            }

                            if (hasRequiredAlbums) {
                                // Reschedule wallpaper changes with WorkManager
                                val homeInterval: Int
                                val lockInterval: Int

                                if (settings.separateSchedules) {
                                    // Separate schedules for home and lock
                                    homeInterval = if (settings.homeEnabled) settings.homeIntervalMinutes else 0
                                    lockInterval = if (settings.lockEnabled) settings.lockIntervalMinutes else 0
                                } else {
                                    // Same interval for both (enabled screens)
                                    val interval = settings.homeIntervalMinutes
                                    homeInterval = if (settings.homeEnabled) interval else 0
                                    lockInterval = if (settings.lockEnabled) interval else 0
                                }

                                // Determine if screens should be synchronized
                                val shouldSync = settings.homeEnabled && settings.lockEnabled &&
                                                settings.homeAlbumId != null &&
                                                settings.homeAlbumId == settings.lockAlbumId &&
                                                !settings.separateSchedules

                                wallpaperScheduler.scheduleWallpaperChanges(
                                    homeIntervalMinutes = homeInterval,
                                    lockIntervalMinutes = lockInterval,
                                    synchronized = shouldSync,
                                    onlyIfNotScheduled = true  // Don't reschedule if already scheduled
                                )

                                Log.d(TAG, "Wallpaper changes rescheduled successfully")
                            } else {
                                Log.d(TAG, "Wallpaper changer enabled but required albums not selected, not scheduling")
                            }
                        }
                    } else {
                        Log.d(TAG, "Wallpaper changer disabled, not scheduling")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling wallpaper changes", e)
                } finally {
                    // Must call finish() to release the async operation
                    pendingResult.finish()
                }
            }
        }
    }
}
