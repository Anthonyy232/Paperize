package com.anthonyla.paperize.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.service.WallpaperChangeLock
import com.anthonyla.paperize.service.wallpaper.WallpaperController
import com.anthonyla.paperize.service.worker.WallpaperScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

/**
 * Fires the short-interval static rotation and immediately re-arms the next one.
 *
 * The change runs here rather than in [com.anthonyla.paperize.service.wallpaper.WallpaperChangeService],
 * because Android 12+ rejects a background foreground-service start, and it does not go through
 * WorkManager either: the platform defers plain jobs until the next doze maintenance window, which
 * would defeat an alarm that just woke the device. A background broadcast keeps roughly a minute of
 * lifetime through [goAsync], which is enough for one wallpaper change; if it still fails, one-time
 * work is enqueued so the rotation is retried instead of silently skipped.
 *
 * Re-arming happens before the change so a slow change can never stall the cadence.
 */
@AndroidEntryPoint
class StaticWallpaperAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmScheduler: StaticWallpaperAlarmScheduler
    @Inject lateinit var wallpaperScheduler: WallpaperScheduler
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var wallpaperController: WallpaperController
    @Inject lateinit var wallpaperChangeLock: WallpaperChangeLock

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Constants.ACTION_STATIC_ALARM) return
        val screenType = intent.getStringExtra(Constants.EXTRA_SCREEN_TYPE)
            ?.let(ScreenType::fromString) ?: return
        val intervalMinutes = intent.getIntExtra(Constants.EXTRA_INTERVAL_MINUTES, 0)
        Log.d(TAG, "Static alarm fired for $screenType ($intervalMinutes min)")

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Re-read the interval: the user may have edited it after this alarm was armed,
                // in which case the stale value in the intent must not win.
                val settings = settingsRepository.getScheduleSettings()
                val currentInterval = settings.intervalMinutes(screenType)
                if (!wallpaperScheduler.isStaticAlarmScheduled(settings, screenType)) {
                    Log.d(TAG, "$screenType is no longer alarm-driven; not re-arming")
                    alarmScheduler.cancel(screenType)
                    return@launch
                }
                alarmScheduler.schedule(screenType, currentInterval)
                changeWallpaper(screenType, settings)
            } catch (e: Exception) {
                Log.e(TAG, "Static alarm handling failed for $screenType", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun changeWallpaper(
        screenType: ScreenType,
        settings: com.anthonyla.paperize.domain.model.ScheduleSettings
    ) {
        try {
            wallpaperChangeLock.mutex.withLock {
                wallpaperController.change(screenType, settings)
            }
            Log.d(TAG, "Changed $screenType wallpaper from alarm")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The broadcast window is short and must not be spent retrying: hand the rotation to
            // one-time work, which the framework retries on its own schedule.
            Log.e(TAG, "Direct $screenType change failed; deferring to one-time work", e)
            wallpaperScheduler.changeNow(screenType)
        }
    }

    private companion object {
        const val TAG = "StaticAlarmReceiver"
    }
}
