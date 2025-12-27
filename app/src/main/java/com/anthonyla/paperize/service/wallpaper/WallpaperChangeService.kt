package com.anthonyla.paperize.service.wallpaper

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.EmptyAlbumException
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.usecase.ChangeWallpaperUseCase
import com.anthonyla.paperize.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Foreground service for changing wallpapers
 *
 * Actions:
 * - CHANGE_WALLPAPER: Change wallpaper immediately
 */
@AndroidEntryPoint
class WallpaperChangeService : Service() {

    @Inject
    lateinit var changeWallpaperUseCase: ChangeWallpaperUseCase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var wallpaperManager: WallpaperManager
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val TAG = "WallpaperChangeService"
        const val ACTION_CHANGE_WALLPAPER = Constants.ACTION_CHANGE_WALLPAPER
        const val EXTRA_SCREEN_TYPE = Constants.EXTRA_SCREEN_TYPE
        private const val ERROR_NOTIFICATION_ID = Constants.NOTIFICATION_ID + 1

        /**
         * Mutex to prevent concurrent wallpaper changes
         * Multiple service instances can be started simultaneously from different triggers
         * (alarms, manual changes, etc.). This mutex ensures only one wallpaper change
         * operation runs at a time, preventing race conditions.
         */
        private val wallpaperChangeMutex = Mutex()
    }

    override fun onCreate() {
        super.onCreate()
        wallpaperManager = WallpaperManager.getInstance(this)
        notificationManager = getSystemService(NotificationManager::class.java)
            ?: throw IllegalStateException("NotificationManager not available")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground service. Android 14+ requires specifying the service type.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                Constants.NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(Constants.NOTIFICATION_ID, createNotification())
        }

        when (intent?.action) {
            ACTION_CHANGE_WALLPAPER -> {
                val screenType = intent.getStringExtra(EXTRA_SCREEN_TYPE)?.let {
                    ScreenType.fromString(it)
                } ?: ScreenType.BOTH
                handleChangeWallpaper(screenType, startId)
            }
        }

        return START_NOT_STICKY
    }

    private fun handleChangeWallpaper(screenType: ScreenType, startId: Int) {
        serviceScope.launch {
            // Use mutex to prevent concurrent wallpaper changes
            wallpaperChangeMutex.withLock {
                try {
                    // Get schedule settings to determine which albums to use
                    val settings = settingsRepository.getScheduleSettings()

                when (screenType) {
                    ScreenType.LIVE -> {
                        // Live wallpaper changes are handled by the live wallpaper service
                        Log.d(TAG, "Live wallpaper - no action needed in service")
                    }
                    ScreenType.HOME -> {
                        val homeAlbumId = settings.homeAlbumId
                        if (homeAlbumId != null) {
                            changeHomeWallpaper(homeAlbumId)
                        } else {
                            Log.w(TAG, "No home album selected")
                        }
                    }
                    ScreenType.LOCK -> {
                        val lockAlbumId = settings.lockAlbumId
                        if (lockAlbumId != null) {
                            changeLockWallpaper(lockAlbumId)
                        } else {
                            Log.w(TAG, "No lock album selected")
                        }
                    }
                    ScreenType.BOTH -> {
                        val homeAlbumId = settings.homeAlbumId
                        val lockAlbumId = settings.lockAlbumId

                        // If same album and not separately scheduled, use same wallpaper
                        if (homeAlbumId != null && lockAlbumId != null &&
                            homeAlbumId == lockAlbumId && !settings.separateSchedules) {
                            // Get one wallpaper and set for both screens
                            val result = changeWallpaperUseCase(homeAlbumId, ScreenType.HOME)
                            result.onSuccess { bitmap ->
                                try {
                                    // Validate bitmap before setting
                                    if (bitmap.width <= 0 || bitmap.height <= 0) {
                                        throw IllegalStateException("Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}")
                                    }
                                    if (bitmap.isRecycled) {
                                        throw IllegalStateException("Bitmap has been recycled")
                                    }

                                    // Set for home screen
                                    wallpaperManager.setBitmap(
                                        bitmap,
                                        null,
                                        true,
                                        WallpaperManager.FLAG_SYSTEM
                                    )
                                    // Set for lock screen
                                    wallpaperManager.setBitmap(
                                        bitmap,
                                        null,
                                        true,
                                        WallpaperManager.FLAG_LOCK
                                    )

                                    Log.d(TAG, "Same wallpaper set for both screens")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error setting wallpaper for both screens", e)
                                } finally {
                                    // Always recycle bitmap to prevent memory leaks
                                    bitmap.recycle()
                                }
                            }.onError { error ->
                                if (error is EmptyAlbumException) {
                                    handleEmptyAlbumError()
                                } else {
                                    Log.e(TAG, "Error getting wallpaper bitmap", error)
                                }
                            }
                        } else {
                            // Different albums or separately scheduled - change independently
                            if (homeAlbumId != null) {
                                changeHomeWallpaper(homeAlbumId)
                            } else {
                                Log.w(TAG, "No home album selected")
                            }

                            if (lockAlbumId != null) {
                                changeLockWallpaper(lockAlbumId)
                            } else {
                                Log.w(TAG, "No lock album selected")
                            }
                        }
                    }
                }

                    stopSelf(startId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error changing wallpaper", e)
                    stopSelf(startId)
                }
            }
        }
    }

    private suspend fun changeHomeWallpaper(albumId: String) {
        val result = changeWallpaperUseCase(albumId, ScreenType.HOME)
        result.onSuccess { bitmap ->
            try {
                // Validate bitmap before setting
                if (bitmap.width <= 0 || bitmap.height <= 0) {
                    throw IllegalStateException("Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}")
                }
                if (bitmap.isRecycled) {
                    throw IllegalStateException("Bitmap has been recycled")
                }

                wallpaperManager.setBitmap(
                    bitmap,
                    null,
                    true,
                    WallpaperManager.FLAG_SYSTEM
                )
                Log.d(TAG, "Home wallpaper changed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting home wallpaper", e)
            } finally {
                // Always recycle bitmap to prevent memory leaks
                bitmap.recycle()
            }
        }.onError { error ->
            if (error is EmptyAlbumException) {
                handleEmptyAlbumError()
            } else {
                Log.e(TAG, "Error getting home wallpaper bitmap", error)
            }
        }
    }

    private suspend fun changeLockWallpaper(albumId: String) {
        val result = changeWallpaperUseCase(albumId, ScreenType.LOCK)
        result.onSuccess { bitmap ->
            try {
                // Validate bitmap before setting
                if (bitmap.width <= 0 || bitmap.height <= 0) {
                    throw IllegalStateException("Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}")
                }
                if (bitmap.isRecycled) {
                    throw IllegalStateException("Bitmap has been recycled")
                }

                wallpaperManager.setBitmap(
                    bitmap,
                    null,
                    true,
                    WallpaperManager.FLAG_LOCK
                )
                Log.d(TAG, "Lock wallpaper changed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting lock wallpaper", e)
            } finally {
                // Always recycle bitmap to prevent memory leaks
                bitmap.recycle()
            }
        }.onError { error ->
            if (error is EmptyAlbumException) {
                handleEmptyAlbumError()
            } else {
                Log.e(TAG, "Error getting lock wallpaper bitmap", error)
            }
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.changing_wallpaper))
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    /**
     * Show an error notification to the user
     */
    private fun showErrorNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
    }

    /**
     * Handle empty album error by showing notification and disabling wallpaper changer
     */
    private suspend fun handleEmptyAlbumError() {
        Log.w(TAG, "Album is empty, disabling wallpaper changer")

        // Disable wallpaper changer
        val settings = settingsRepository.getScheduleSettings()
        settingsRepository.updateScheduleSettings(settings.copy(enableChanger = false))

        // Show notification to user
        showErrorNotification(
            getString(R.string.no_wallpapers_in_album),
            getString(R.string.wallpaper_changer_disabled_empty_album)
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
