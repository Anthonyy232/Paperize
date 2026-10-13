package com.anthonyla.paperize.service.wallpaper

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.NoValidWallpaperException
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.presentation.MainActivity
import com.anthonyla.paperize.service.WallpaperChangeLock
import com.anthonyla.paperize.service.worker.WallpaperScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

/** Runs manual wallpaper actions in the foreground, serialized with scheduled changes. */
@AndroidEntryPoint
class WallpaperChangeService : Service() {

    @Inject lateinit var wallpaperController: WallpaperController
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var wallpaperChangeLock: WallpaperChangeLock
    @Inject lateinit var wallpaperScheduler: WallpaperScheduler
    @Inject lateinit var wallpaperRepository: WallpaperRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this, Constants.NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        val action = intent?.action
        val screen = intent?.getStringExtra(EXTRA_SCREEN_TYPE)?.let(ScreenType::fromString)
        val wallpaperId = intent?.getStringExtra(EXTRA_WALLPAPER_ID)
        serviceScope.launch {
            wallpaperChangeLock.mutex.withLock {
                try {
                    when (action) {
                        ACTION_CHANGE_WALLPAPER -> changeWallpaper(screen)
                        ACTION_APPLY_SPECIFIC_WALLPAPER -> applySpecificWallpaper(wallpaperId, screen ?: ScreenType.BOTH)
                        ACTION_REAPPLY_EFFECTS -> reapplyEffects()
                        else -> Log.w(TAG, "Unknown action: $action")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Wallpaper action $action failed", e)
                    showErrorNotification(getString(R.string.app_name), errorMessage(e))
                } finally {
                    stopSelf(startId)
                }
            }
        }
        return START_NOT_STICKY
    }

    /** Without a [screen], changes every screen the current settings rotate. */
    private suspend fun changeWallpaper(screen: ScreenType?) {
        val mode = settingsRepository.getWallpaperMode()
        val targets = screen?.let(::setOf) ?: settingsRepository.getScheduleSettings().activeScreens(mode)
        for (target in targets) {
            val settings = settingsRepository.getScheduleSettings()
            val outcome = wallpaperController.change(target, settings)
            if (outcome.emptyAlbum) showEmptyAlbumNotification()
            if (outcome.changed) wallpaperScheduler.resetAfterManualChange(target, settings, mode)
        }
    }

    private suspend fun applySpecificWallpaper(wallpaperId: String?, screen: ScreenType) {
        require(!wallpaperId.isNullOrBlank()) { getString(R.string.wallpaper_not_found) }
        val mode = settingsRepository.getWallpaperMode()
        require(screen != ScreenType.LIVE && mode == WallpaperMode.STATIC) { getString(R.string.static_mode_required) }
        val wallpaper = wallpaperRepository.getWallpaperById(wallpaperId)
            ?: error(getString(R.string.wallpaper_not_found))
        val settings = settingsRepository.getScheduleSettings()
        wallpaperController.applySpecific(wallpaper.albumId, wallpaper.id, screen, settings)
        wallpaperScheduler.resetAfterManualChange(screen, settings, mode)
    }

    private suspend fun reapplyEffects() {
        val mode = settingsRepository.getWallpaperMode()
        val settings = settingsRepository.getScheduleSettings()
        for (target in settings.activeScreens(mode)) {
            if (wallpaperController.reapply(target, settings).emptyAlbum) showEmptyAlbumNotification()
        }
    }

    private fun errorMessage(e: Exception): String = when (e) {
        is NoValidWallpaperException -> getString(R.string.error_no_valid_wallpaper_after_retries)
        else -> e.localizedMessage ?: getString(R.string.error_no_valid_wallpaper_after_retries)
    }

    private fun showEmptyAlbumNotification() {
        showErrorNotification(
            getString(R.string.no_wallpapers_in_album),
            getString(R.string.wallpaper_changer_disabled_empty_album)
        )
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )

    private fun createNotification(): Notification =
        NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.changing_wallpaper))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .build()

    private fun showErrorNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(ERROR_NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WallpaperChangeService"
        private const val ACTION_CHANGE_WALLPAPER = "com.anthonyla.paperize.ACTION_CHANGE_WALLPAPER"
        private const val ACTION_APPLY_SPECIFIC_WALLPAPER = "com.anthonyla.paperize.ACTION_APPLY_SPECIFIC_WALLPAPER"
        private const val ACTION_REAPPLY_EFFECTS = "com.anthonyla.paperize.ACTION_REAPPLY_EFFECTS"
        private const val EXTRA_SCREEN_TYPE = Constants.EXTRA_SCREEN_TYPE
        private const val EXTRA_WALLPAPER_ID = "wallpaper_id"
        private const val ERROR_NOTIFICATION_ID = Constants.NOTIFICATION_ID + 1

        /** Without a [screen], changes every screen the current settings rotate (or the live wallpaper). */
        fun changeWallpaper(context: Context, screen: ScreenType? = null) =
            start(context, ACTION_CHANGE_WALLPAPER) { screen?.let { putExtra(EXTRA_SCREEN_TYPE, it.name) } }

        fun applyWallpaper(context: Context, wallpaperId: String, screen: ScreenType) =
            start(context, ACTION_APPLY_SPECIFIC_WALLPAPER) {
                putExtra(EXTRA_WALLPAPER_ID, wallpaperId)
                putExtra(EXTRA_SCREEN_TYPE, screen.name)
            }

        /** Re-render the current static wallpapers after display settings change. */
        fun reapplyEffects(context: Context) = start(context, ACTION_REAPPLY_EFFECTS)

        private fun start(context: Context, action: String, extras: Intent.() -> Unit = {}) {
            context.startForegroundService(
                Intent(context, WallpaperChangeService::class.java).setAction(action).apply(extras)
            )
        }
    }
}
