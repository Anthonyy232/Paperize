package com.anthonyla.paperize.feature.wallpaper.wallpaper_service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.core.calculateInSampleSize
import com.anthonyla.paperize.core.darkenBitmap
import com.anthonyla.paperize.core.fillBitmap
import com.anthonyla.paperize.core.fitBitmap
import com.anthonyla.paperize.core.getImageDimensions
import com.anthonyla.paperize.core.stretchBitmap
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
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlin.math.min


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
        STOP,
        UPDATE,
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
                handler.postDelayed(runnableCode, 3000)
            }
            Actions.UPDATE.toString() -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        updateCurrentWallpaper(this@WallpaperService)
                    } catch (e: Exception) {
                        Log.e("PaperizeWallpaperChanger", "Error in changing brightness", e)
                    }
                }
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
            .setContentTitle(getString(R.string.app_name))
            .setContentText(resources.getQuantityString(R.plurals.notification_content_text, total, current, total, formatTime(timeInMinutes)))
            .setSmallIcon(R.drawable.notification_icon)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Changes the wallpaper to the next wallpaper in the queue of the selected album
     * If none left, reshuffle the wallpapers and pick the first one
     */
    private suspend fun changeWallpaper(context: Context) {
        try {
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            val time = LocalDateTime.now()
            settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, time.format(formatter))
            settingsDataStoreImpl.putString(
                SettingsConstants.NEXT_SET_TIME,
                time.plusMinutes(timeInMinutes.toLong()).format(formatter)
            )
            val scaling = settingsDataStoreImpl.getString(SettingsConstants.WALLPAPER_SCALING)
                ?.let { ScalingConstants.valueOf(it) } ?: ScalingConstants.FILL
            val setLockWithHome =
                settingsDataStoreImpl.getBoolean(SettingsConstants.SET_LOCK_WITH_HOME) ?: false
            val darken = settingsDataStoreImpl.getBoolean(SettingsConstants.DARKEN) ?: false
            val darkenPercentage =
                settingsDataStoreImpl.getInt(SettingsConstants.DARKEN_PERCENTAGE) ?: 100
            val selectedAlbum = selectedRepository.getSelectedAlbum().first().firstOrNull()
            if (selectedAlbum == null) {
                onDestroy()
                return
            }
            else {
                selectedAlbum.let { it ->
                    var wallpaper = it.album.wallpapersInQueue.firstOrNull()
                    if (wallpaper != null) {
                        val notification =
                            createNotification(it.album.wallpapersInQueue.size, it.wallpapers.size)
                        val notificationManager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(1, notification)
                        if (!setWallpaper(
                                context,
                                wallpaper.toUri(),
                                setLockWithHome,
                                darken,
                                darkenPercentage,
                                scaling
                            )
                        ) {
                            selectedAlbum.wallpapers.firstOrNull { it.wallpaperUri == wallpaper }
                                ?.let { it1 ->
                                    selectedRepository.deleteWallpaper(it1)
                                    selectedRepository.upsertSelectedAlbum(
                                        it.copy(
                                            album = it.album.copy(
                                                wallpapersInQueue = it.album.wallpapersInQueue.drop(
                                                    1
                                                ),
                                                currentWallpaper = null
                                            ),
                                            wallpapers = it.wallpapers.filter { it == it1 },
                                        )
                                    )
                                    albumRepository.deleteWallpaper(it1)
                                }
                        } else {
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
                        val notification =
                            createNotification(it.wallpapers.size, it.wallpapers.size)
                        val notificationManager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(1, notification)
                        val newWallpaperInQueue = it.wallpapers.map { it.wallpaperUri }.shuffled()
                        wallpaper = newWallpaperInQueue.firstOrNull()
                        if (wallpaper != null) {
                            if (!setWallpaper(
                                    context,
                                    wallpaper.toUri(),
                                    setLockWithHome,
                                    darken,
                                    darkenPercentage,
                                    scaling
                                )
                            ) {
                                selectedAlbum.wallpapers.firstOrNull { it.wallpaperUri == wallpaper }
                                    ?.let { it1 ->
                                        selectedRepository.deleteWallpaper(it1)
                                        selectedRepository.upsertSelectedAlbum(
                                            it.copy(
                                                album = it.album.copy(
                                                    wallpapersInQueue = it.album.wallpapersInQueue.drop(
                                                        1
                                                    ),
                                                    currentWallpaper = null
                                                ),
                                                wallpapers = it.wallpapers.filter { it == it1 }
                                            )
                                        )
                                        albumRepository.deleteWallpaper(it1)
                                    }
                            } else {
                                selectedRepository.upsertSelectedAlbum(
                                    it.copy(
                                        album = it.album.copy(
                                            wallpapersInQueue = newWallpaperInQueue.drop(1),
                                            currentWallpaper = wallpaper
                                        )
                                    )
                                )
                            }
                        } else {
                            onDestroy()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error in changing wallpaper", e)
        }
    }

    private suspend fun updateCurrentWallpaper(context: Context) {
        try {
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            val time = LocalDateTime.now()
            settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, time.format(formatter))
            settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, time.plusMinutes(timeInMinutes.toLong()).format(formatter))
            val scaling = settingsDataStoreImpl.getString(SettingsConstants.WALLPAPER_SCALING)?.let { ScalingConstants.valueOf(it) } ?: ScalingConstants.FILL
            val setLockWithHome = settingsDataStoreImpl.getBoolean(SettingsConstants.SET_LOCK_WITH_HOME) ?: false
            val darken = settingsDataStoreImpl.getBoolean(SettingsConstants.DARKEN) ?: false
            val darkenPercentage = settingsDataStoreImpl.getInt(SettingsConstants.DARKEN_PERCENTAGE) ?: 100
            val selectedAlbum = selectedRepository.getSelectedAlbum().first().firstOrNull()
            if (selectedAlbum == null) {
                onDestroy()
                return
            }
            else {
                selectedAlbum.let { it ->
                    val wallpaper = it.album.currentWallpaper
                    if (wallpaper != null) {
                        val notification = createNotification(it.album.wallpapersInQueue.size, it.wallpapers.size)
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(1, notification)
                        if (!setWallpaper(context, wallpaper.toUri(), setLockWithHome, darken, darkenPercentage, scaling)) {
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
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error in updating", e)
        }
    }

    /**
     * Sets the wallpaper to the given uri
     */
    private fun setWallpaper(context: Context, wallpaper: Uri, setLockWithHome: Boolean, darken: Boolean, percent: Int, scaling: ScalingConstants): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(context)
        try {
            val imageSize = wallpaper.getImageDimensions(context)
            val aspectRatio = imageSize.height.toFloat() / imageSize.width.toFloat()
            val device = context.resources.displayMetrics
            val targetWidth = min(4 * device.widthPixels, imageSize.width)
            val targetHeight = (targetWidth * aspectRatio).toInt()

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, wallpaper)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSize(targetWidth, targetHeight)
                }
            } else {
                context.contentResolver.openInputStream(wallpaper)?.use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = calculateInSampleSize(imageSize, targetWidth, targetHeight)
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
            }

            if (bitmap == null) return false
            else {
                processBitmap(device, bitmap, darken, percent, scaling)?.let {
                    image -> wallpaperManager.setBitmap(image, null, true, when (setLockWithHome) {
                        true -> WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
                        false -> WallpaperManager.FLAG_SYSTEM
                    })
                }
                return true
            }
        } catch (e: IOException) {
            Log.e("PaperizeWallpaperChanger", "Error setting wallpaper", e)
            return false
        }
    }

    /**
     * Formats the time in minutes to a human readable format
     */
    private fun formatTime(timeInMinutes: Int): String {
        val days = timeInMinutes / (24 * 60)
        val hours = (timeInMinutes % (24 * 60)) / 60
        val minutes = timeInMinutes % 60

        val formattedDays = if (days > 0) resources.getQuantityString(R.plurals.days, days, days) else ""
        val formattedHours = if (hours > 0) resources.getQuantityString(R.plurals.hours, hours, hours) else ""
        val formattedMinutes = if (minutes > 0) resources.getQuantityString(R.plurals.minutes, minutes, minutes) else ""

        return listOf(formattedDays, formattedHours, formattedMinutes).filter { it.isNotEmpty() }.joinToString(", ")
    }

    /**
     * Darkens the bitmap by the given percentage and returns it
     * 0 - lightest, 100 - darkest
     */
    private fun processBitmap(device: DisplayMetrics, source: Bitmap, darken: Boolean, percent: Int, scaling: ScalingConstants): Bitmap? {
        try {
            var processedBitmap = source.copy(Bitmap.Config.ARGB_8888, true)

            // Apply wallpaper scaling effects
            processedBitmap = when (scaling) {
                ScalingConstants.FILL -> fillBitmap(processedBitmap, device.widthPixels, device.heightPixels)
                ScalingConstants.FIT -> fitBitmap(processedBitmap, device.widthPixels, device.heightPixels)
                ScalingConstants.STRETCH -> stretchBitmap(processedBitmap, device.widthPixels, device.heightPixels)
            }

            // Apply brightness effect
            if (darken && percent < 100) {
                processedBitmap = darkenBitmap(processedBitmap, percent, device.widthPixels, device.heightPixels)
            }
            return processedBitmap
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error darkening bitmap", e)
            return null
        }
    }
}