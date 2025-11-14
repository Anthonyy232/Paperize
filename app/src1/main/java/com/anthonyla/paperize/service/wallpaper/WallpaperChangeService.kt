package com.anthonyla.paperize.service.wallpaper

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.WallpaperManager
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.usecase.ChangeWallpaperUseCase
import com.anthonyla.paperize.domain.usecase.GetSelectedAlbumsUseCase
import com.anthonyla.paperize.domain.usecase.RefreshAlbumUseCase
import com.anthonyla.paperize.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for changing wallpapers
 *
 * Actions:
 * - CHANGE_WALLPAPER: Change wallpaper immediately
 * - UPDATE_WALLPAPER: Update current wallpaper with new effects
 * - REFRESH_ALBUM: Refresh album data (validate URIs, detect changes)
 * - CANCEL: Stop the service
 */
@AndroidEntryPoint
class WallpaperChangeService : Service() {

    @Inject
    lateinit var changeWallpaperUseCase: ChangeWallpaperUseCase

    @Inject
    lateinit var getSelectedAlbumsUseCase: GetSelectedAlbumsUseCase

    @Inject
    lateinit var refreshAlbumUseCase: RefreshAlbumUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var wallpaperManager: WallpaperManager
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val TAG = "WallpaperChangeService"
        const val ACTION_CHANGE_WALLPAPER = Constants.ACTION_CHANGE_WALLPAPER
        const val ACTION_UPDATE_WALLPAPER = Constants.ACTION_UPDATE_WALLPAPER
        const val ACTION_REFRESH_ALBUM = Constants.ACTION_REFRESH_ALBUM
        const val ACTION_CANCEL = Constants.ACTION_CANCEL
        const val EXTRA_SCREEN_TYPE = Constants.EXTRA_SCREEN_TYPE
    }

    override fun onCreate() {
        super.onCreate()
        wallpaperManager = WallpaperManager.getInstance(this)
        notificationManager = getSystemService(NotificationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground service
        startForeground(Constants.NOTIFICATION_ID, createNotification())

        when (intent?.action) {
            ACTION_CHANGE_WALLPAPER -> {
                val screenType = intent.getStringExtra(EXTRA_SCREEN_TYPE)?.let {
                    ScreenType.fromString(it)
                } ?: ScreenType.BOTH
                handleChangeWallpaper(screenType)
            }
            ACTION_UPDATE_WALLPAPER -> {
                val screenType = intent.getStringExtra(EXTRA_SCREEN_TYPE)?.let {
                    ScreenType.fromString(it)
                } ?: ScreenType.BOTH
                handleUpdateWallpaper(screenType)
            }
            ACTION_REFRESH_ALBUM -> {
                handleRefreshAlbum()
            }
            ACTION_CANCEL -> {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun handleChangeWallpaper(screenType: ScreenType) {
        serviceScope.launch {
            try {
                // Get selected albums
                val selectedAlbums = getSelectedAlbumsUseCase().first()
                if (selectedAlbums.isEmpty()) {
                    Log.w(TAG, "No selected albums")
                    stopSelf()
                    return@launch
                }

                // Use first selected album (can be extended to support multiple albums)
                val album = selectedAlbums.first()

                when (screenType) {
                    ScreenType.HOME -> changeHomeWallpaper(album.id)
                    ScreenType.LOCK -> changeLockWallpaper(album.id)
                    ScreenType.BOTH -> {
                        changeHomeWallpaper(album.id)
                        changeLockWallpaper(album.id)
                    }
                }

                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Error changing wallpaper", e)
                stopSelf()
            }
        }
    }

    private suspend fun changeHomeWallpaper(albumId: String) {
        val result = changeWallpaperUseCase(albumId, ScreenType.HOME)
        result.onSuccess { bitmap ->
            try {
                wallpaperManager.setBitmap(
                    bitmap,
                    null,
                    true,
                    WallpaperManager.FLAG_SYSTEM
                )
                Log.d(TAG, "Home wallpaper changed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting home wallpaper", e)
            }
        }.onError { error ->
            Log.e(TAG, "Error getting home wallpaper bitmap", error)
        }
    }

    private suspend fun changeLockWallpaper(albumId: String) {
        val result = changeWallpaperUseCase(albumId, ScreenType.LOCK)
        result.onSuccess { bitmap ->
            try {
                wallpaperManager.setBitmap(
                    bitmap,
                    null,
                    true,
                    WallpaperManager.FLAG_LOCK
                )
                Log.d(TAG, "Lock wallpaper changed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting lock wallpaper", e)
            }
        }.onError { error ->
            Log.e(TAG, "Error getting lock wallpaper bitmap", error)
        }
    }

    private fun handleUpdateWallpaper(screenType: ScreenType) {
        // TODO: Implement update wallpaper (re-apply effects to current wallpaper)
        stopSelf()
    }

    private fun handleRefreshAlbum() {
        serviceScope.launch {
            try {
                val selectedAlbums = getSelectedAlbumsUseCase().first()
                selectedAlbums.forEach { album ->
                    refreshAlbumUseCase(album.id)
                }
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing album", e)
                stopSelf()
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
            .setContentTitle("Paperize")
            .setContentText("Changing wallpaper...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
