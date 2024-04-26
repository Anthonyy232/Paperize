package com.anthonyla.paperize.feature.wallpaper.wallpaperservice

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class WallpaperService: Service() {
    @Inject lateinit var repository: SelectedAlbumRepository
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnableCode: Runnable
    private var timeInMinutes: Int = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT

    companion object {
        var isRunning = false
    }
    enum class Actions {
        START,
        STOP
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    /**
     * Set up the runnable code to change the wallpaper to prevent runnableCode from being null
     */
    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification(0, 0))
        isRunning = true
        runnableCode = object : Runnable {
            override fun run() {
                Log.d("PaperizeWallpaperChanger", "Scheduled in: $timeInMinutes")
                val self = this
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        changeWallpaper(this@WallpaperService)
                    } catch (e: Exception) {
                        Log.e("PaperizeWallpaperChanger", "Error in runnableCode", e)
                    } finally {
                        handler.postDelayed(self, timeInMinutes * 60 * 1000L)
                    }
                }
            }
        }
    }

    /**
     * Start the service and schedule the wallpaper change
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        when(intent?.action) {
            Actions.START.toString() -> {
                timeInMinutes = intent.getIntExtra("timeInMinutes", 1)
                // Schedule the next wallpaper change
                runnableCode = object : Runnable {
                    override fun run() {
                        Log.d("PaperizeWallpaperChanger", "Scheduled in: $timeInMinutes")
                        val self = this
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                changeWallpaper(this@WallpaperService)
                            } catch (e: Exception) {
                                Log.e("PaperizeWallpaperChanger", "Error in runnableCode", e)
                            } finally {
                                handler.postDelayed(self, timeInMinutes * 60 * 1000L)
                            }
                        }
                    }
                }
                handler.post(runnableCode)
            }
            Actions.STOP.toString() -> {
                handler.removeCallbacks(runnableCode)
                stopSelf()
            }
        }
        return START_STICKY
    }

    /**
     * Clean up the service when it is destroyed
     */

    override fun onDestroy() {
        Log.d("PaperizeWallpaperChanger", "Service destroyed")
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(runnableCode)
    }

    /**
     * Restart the service if it is removed
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("PaperizeWallpaperChanger", "Service removed by swipe")
        isRunning = false
        handler.removeCallbacks(runnableCode)
        val restartServiceIntent = Intent(applicationContext, this::class.java).apply {
            action = Actions.START.toString()
            putExtra("timeInMinutes", timeInMinutes)
        }
        restartServiceIntent.setPackage(packageName)
        applicationContext.startForegroundService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    } */

    /**
     * Creates a notification for the wallpaper service
     */

    private fun createNotification(current: Int = 0, total: Int = 0): Notification {
        return NotificationCompat.Builder(this, "wallpaper_service_channel")
            .setContentTitle("Paperize")
            .setContentText("Changing wallpaper... Album: $current/$total")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    /**
     * Changes the wallpaper to the next wallpaper in the queue of the selected album
     * If none left, reshuffle the wallpapers and pick the first one
     */
    private suspend fun changeWallpaper(context: Context) {
        val album = repository.getSelectedAlbum().first().firstOrNull()
        album?.let {
            val notification = createNotification(it.album.wallpapersInQueue.size, it.wallpapers.size)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(1, notification)
            var wallpaper = it.album.wallpapersInQueue.firstOrNull()
            if (wallpaper != null) {
                setWallpaper(context, wallpaper.toUri())
                repository.upsertSelectedAlbum(
                    it.copy(
                        album = it.album.copy(
                            wallpapersInQueue = it.album.wallpapersInQueue.drop(1)
                        )
                    )
                )
            }
            else {
                val newWallpaperInQueue = it.wallpapers.map { it.wallpaperUri }.shuffled()
                wallpaper = newWallpaperInQueue.firstOrNull()
                if (wallpaper != null) {
                    setWallpaper(context, wallpaper.toUri())
                    repository.upsertSelectedAlbum(
                        it.copy(
                            album = it.album.copy(
                                wallpapersInQueue = newWallpaperInQueue.drop(1)
                            )
                        )
                    )
                }
            }
        }
    }

    /**
     * Sets the wallpaper to the given uri
     */
    private fun setWallpaper(context: Context, wallpaper: Uri) {
        val wallpaperManager = WallpaperManager.getInstance(context)
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, wallpaper)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    val displayMetrics = context.resources.displayMetrics
                    val targetWidth = displayMetrics.widthPixels
                    val targetHeight = displayMetrics.heightPixels
                    decoder.setTargetSize(targetWidth, targetHeight)
                }
            } else {
                context.contentResolver.openInputStream(wallpaper)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }
            bitmap?.let { image -> wallpaperManager.setBitmap(image) }
        } catch (e: IOException) {
            Log.e("PaperizeWallpaperChanger", "Error changing wallpaper", e)
        }
    }
}