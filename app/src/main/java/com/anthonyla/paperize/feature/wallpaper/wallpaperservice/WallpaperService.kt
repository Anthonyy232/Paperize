package com.anthonyla.paperize.feature.wallpaper.wallpaperservice

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
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
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import com.anthonyla.paperize.feature.wallpaper.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject


@AndroidEntryPoint
class WallpaperService: Service() {
    @Inject lateinit var selectedRepository: SelectedAlbumRepository
    @Inject lateinit var albumRepository: AlbumRepository
    @Inject lateinit var settingsDataStoreImpl: SettingsDataStore
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnableCode: Runnable
    private var timeInMinutes: Int = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT

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
        runnableCode = object : Runnable {
            override fun run() {
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
        handler.removeCallbacks(runnableCode)
        when(intent?.action) {
            Actions.START.toString() -> {
                timeInMinutes = intent.getIntExtra("timeInMinutes", SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT)
                // Schedule the next wallpaper change
                runnableCode = object : Runnable {
                    override fun run() {
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
                CoroutineScope(Dispatchers.IO).launch {
                    settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, "")
                    settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, "")
                }
                stopSelf()
            }
        }
        return START_STICKY
    }

    /**
     * Clean up the service when it is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnableCode)
        CoroutineScope(Dispatchers.IO).launch {
            settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, "")
            settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, "")
        }
    }

    /**
     * Creates a notification for the wallpaper service
     */
    private fun createNotification(current: Int = 0, total: Int = 0): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, "wallpaper_service_channel")
            .setContentTitle("Paperize")
            .setContentText("Rotation: $current/$total wallpapers\nInterval: ${formatTime(timeInMinutes)}")
            .setSmallIcon(R.drawable.notification_icon)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Changes the wallpaper to the next wallpaper in the queue of the selected album
     * If none left, reshuffle the wallpapers and pick the first one
     */
    private suspend fun changeWallpaper(context: Context) {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yy hh:mm\na")
        val time = LocalDateTime.now()
        settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, time.format(formatter))
        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, time.plusMinutes(timeInMinutes.toLong()).format(formatter))
        val setLockWithHome = settingsDataStoreImpl.getBoolean(SettingsConstants.SET_LOCK_WITH_HOME) ?: false
        val selectedAlbum = selectedRepository.getSelectedAlbum().first().firstOrNull()
        selectedAlbum?.let { it ->
            var wallpaper = it.album.wallpapersInQueue.firstOrNull()
            // Pick the next wallpaper in the queue
            if (wallpaper != null) {
                val notification = createNotification(it.album.wallpapersInQueue.size, it.wallpapers.size)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, notification)
                if (!setWallpaper(context, wallpaper.toUri(), setLockWithHome)) {
                    selectedAlbum.wallpapers.firstOrNull{ it.wallpaperUri == wallpaper }
                        ?.let { it1 ->
                            selectedRepository.deleteWallpaper(it1)
                            selectedRepository.upsertSelectedAlbum(
                                it.copy(
                                    album = it.album.copy(
                                        wallpapersInQueue = it.album.wallpapersInQueue.drop(1),
                                        currentWallpaper = null
                                    ),
                                    wallpapers = it.wallpapers.filter { it == it1 },
                                )
                            )
                            albumRepository.deleteWallpaper(it1)
                        }
                }
                else {
                    selectedRepository.upsertSelectedAlbum(
                        it.copy(
                            album = it.album.copy(
                                wallpapersInQueue = it.album.wallpapersInQueue.drop(1),
                                currentWallpaper = wallpaper
                            )
                        )
                    )
                }
            }
            // No more wallpapers in the queue -- reshuffle the wallpapers
            else {
                val notification = createNotification(it.wallpapers.size, it.wallpapers.size)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, notification)
                val newWallpaperInQueue = it.wallpapers.map { it.wallpaperUri }.shuffled()
                wallpaper = newWallpaperInQueue.firstOrNull()
                if (wallpaper != null) {
                    if (!setWallpaper(context, wallpaper.toUri(), setLockWithHome)) {
                        selectedAlbum.wallpapers.firstOrNull{ it.wallpaperUri == wallpaper }
                            ?.let { it1 ->
                                selectedRepository.deleteWallpaper(it1)
                                selectedRepository.upsertSelectedAlbum(
                                    it.copy(
                                        album = it.album.copy(
                                            wallpapersInQueue = it.album.wallpapersInQueue.drop(1),
                                            currentWallpaper = null
                                        ),
                                        wallpapers = it.wallpapers.filter { it == it1 }
                                    )
                                )
                                albumRepository.deleteWallpaper(it1)
                            }
                    }
                    else {
                        selectedRepository.upsertSelectedAlbum(
                            it.copy(
                                album = it.album.copy(
                                    wallpapersInQueue = newWallpaperInQueue.drop(1),
                                    currentWallpaper = wallpaper
                                )
                            )
                        )
                    }
                } else { onDestroy() }
            }
        }
    }

    /**
     * Sets the wallpaper to the given uri
     */
    private fun setWallpaper(context: Context, wallpaper: Uri, setLockWithHome: Boolean): Boolean {
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
            bitmap?.let {
                image -> wallpaperManager.setBitmap(image, null, true, when (setLockWithHome) {
                    true -> WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
                    false -> WallpaperManager.FLAG_SYSTEM
                })
            }
            return true
        } catch (e: IOException) { return false }
    }

    /**
     * Formats the time in minutes to a human readable format
     */
    private fun formatTime(timeInMinutes: Int): String {
        val days = timeInMinutes / (24 * 60)
        val hours = (timeInMinutes % (24 * 60)) / 60
        val minutes = timeInMinutes % 60

        val formattedDays = when {
            days > 1 -> "$days days"
            days == 1 -> "$days day"
            else -> ""
        }
        val formattedHours = when {
            hours > 1 -> "$hours hours"
            hours == 1 -> "$hours hour"
            else -> ""
        }
        val formattedMinutes = when {
            minutes > 1 -> "$minutes minutes"
            minutes == 1 -> "$minutes minute"
            else -> ""
        }

        return listOf(formattedDays, formattedHours, formattedMinutes).filter { it.isNotEmpty() }.joinToString(", ")
    }
}