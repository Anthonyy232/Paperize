package com.anthonyla.paperize.service.wallpaper

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anthonyla.paperize.R
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

@AndroidEntryPoint
class WallpaperChangeService : Service() {

    @Inject lateinit var wallpaperController: WallpaperController
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var wallpaperChangeLock: WallpaperChangeLock
    @Inject lateinit var wallpaperScheduler: WallpaperScheduler
    @Inject lateinit var wallpaperRepository: WallpaperRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
            ?: error("NotificationManager not available")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                Constants.NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(Constants.NOTIFICATION_ID, createNotification())
        }

        val screenType = intent?.getStringExtra(EXTRA_SCREEN_TYPE)
            ?.let(ScreenType::fromString)
            ?: ScreenType.BOTH
        when (intent?.action) {
            ACTION_CHANGE_WALLPAPER -> handleChangeWallpaper(screenType, startId)
            ACTION_CHANGE_WALLPAPER_AUTO ->
                handleChangeWallpaper(screenType, startId, respectWallpaperMode = true)
            ACTION_APPLY_SPECIFIC_WALLPAPER -> handleApplySpecificWallpaper(
                wallpaperId = intent.getStringExtra(EXTRA_WALLPAPER_ID),
                screenType = screenType,
                startId = startId
            )
            ACTION_REAPPLY_EFFECTS -> handleReapplyEffects(screenType, startId)
            else -> {
                Log.w(TAG, "Unknown action: ${intent?.action}")
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun handleChangeWallpaper(
        screenType: ScreenType,
        startId: Int,
        respectWallpaperMode: Boolean = false
    ) {
        serviceScope.launch {
            wallpaperChangeLock.mutex.withLock {
                try {
                    val wallpaperMode = settingsRepository.getWallpaperMode()
                    val effectiveScreenType =
                        if (
                            respectWallpaperMode &&
                            wallpaperMode == WallpaperMode.LIVE
                        ) {
                            ScreenType.LIVE
                        } else {
                            screenType
                        }
                    val settings = settingsRepository.getScheduleSettings()
                    val outcome = wallpaperController.change(effectiveScreenType, settings)
                    if (outcome.emptyAlbum) showEmptyAlbumNotification()
                    if (outcome.changed) {
                        wallpaperScheduler.resetAfterManualChange(
                            effectiveScreenType,
                            settings,
                            wallpaperMode
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error changing wallpaper", e)
                    showErrorNotification(
                        getString(R.string.app_name),
                        e.localizedMessage
                            ?: getString(R.string.error_no_valid_wallpaper_after_retries)
                    )
                } finally {
                    stopSelf(startId)
                }
            }
        }
    }

    private fun handleApplySpecificWallpaper(
        wallpaperId: String?,
        screenType: ScreenType,
        startId: Int
    ) {
        serviceScope.launch {
            wallpaperChangeLock.mutex.withLock {
                try {
                    require(!wallpaperId.isNullOrBlank()) { getString(R.string.wallpaper_not_found) }
                    require(screenType != ScreenType.LIVE) { getString(R.string.static_mode_required) }
                    val wallpaperMode = settingsRepository.getWallpaperMode()
                    require(wallpaperMode == WallpaperMode.STATIC) {
                        getString(R.string.static_mode_required)
                    }
                    val wallpaper = wallpaperRepository.getWallpaperById(wallpaperId)
                        ?: error(getString(R.string.wallpaper_not_found))
                    val settings = settingsRepository.getScheduleSettings()

                    wallpaperController.applySpecific(
                        albumId = wallpaper.albumId,
                        wallpaperId = wallpaper.id,
                        screen = screenType,
                        settings = settings
                    )
                    wallpaperScheduler.resetAfterManualChange(
                        screenType = screenType,
                        settings = settings,
                        wallpaperMode = wallpaperMode
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error applying selected wallpaper", e)
                    showErrorNotification(
                        getString(R.string.app_name),
                        e.localizedMessage ?: getString(R.string.error_no_valid_wallpaper_after_retries)
                    )
                } finally {
                    stopSelf(startId)
                }
            }
        }
    }

    private fun handleReapplyEffects(screenType: ScreenType, startId: Int) {
        serviceScope.launch {
            wallpaperChangeLock.mutex.withLock {
                try {
                    val settings = settingsRepository.getScheduleSettings()
                    val outcome = wallpaperController.reapply(screenType, settings)
                    if (outcome.emptyAlbum) showEmptyAlbumNotification()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error reapplying effects", e)
                    showErrorNotification(
                        getString(R.string.app_name),
                        e.localizedMessage
                            ?: getString(R.string.error_no_valid_wallpaper_after_retries)
                    )
                } finally {
                    stopSelf(startId)
                }
            }
        }
    }

    private fun showEmptyAlbumNotification() {
        showErrorNotification(
            getString(R.string.no_wallpapers_in_album),
            getString(R.string.wallpaper_changer_disabled_empty_album)
        )
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent().setClassName(packageName, MainActivity::class.java.name),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.changing_wallpaper))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showErrorNotification(title: String, message: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent().setClassName(packageName, MainActivity::class.java.name),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WallpaperChangeService"
        const val ACTION_CHANGE_WALLPAPER = Constants.ACTION_CHANGE_WALLPAPER
        const val ACTION_CHANGE_WALLPAPER_AUTO =
            "com.anthonyla.paperize.ACTION_CHANGE_WALLPAPER_AUTO"
        const val ACTION_APPLY_SPECIFIC_WALLPAPER = Constants.ACTION_APPLY_SPECIFIC_WALLPAPER
        const val ACTION_REAPPLY_EFFECTS = Constants.ACTION_REAPPLY_EFFECTS
        const val EXTRA_SCREEN_TYPE = Constants.EXTRA_SCREEN_TYPE
        const val EXTRA_WALLPAPER_ID = Constants.EXTRA_WALLPAPER_ID
        private const val ERROR_NOTIFICATION_ID = Constants.NOTIFICATION_ID + 1
    }
}
