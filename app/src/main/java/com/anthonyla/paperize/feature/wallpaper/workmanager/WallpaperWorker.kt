package com.anthonyla.paperize.feature.wallpaper.workmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anthonyla.paperize.R
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Worker class to change wallpaper periodically
 */
@HiltWorker
class WallpaperWorker @AssistedInject constructor(
    @Assisted private val repository: SelectedAlbumRepository,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "wallpaper_worker_channel"
        const val NOTIFICATION_ID = 1
    }
    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                changeWallpaper(appContext)
                createNotificationChannel()
                showNotification()
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun changeWallpaper(context: Context) {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val wallpaper = repository.getRandomWallpaperInRotation()
        if (wallpaper != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(
                        context.contentResolver,
                        wallpaper.wallpaperUri.toUri()
                    )
                    ImageDecoder.decodeBitmap(source)
                } else {
                    context.contentResolver.openInputStream(wallpaper.wallpaperUri.toUri())
                        ?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }
                }
                bitmap?.let { bmp ->
                    wallpaperManager.setBitmap(bmp)
                }
                repository.upsertWallpaper(wallpaper.copy(isInRotation = false))
            } catch (e: IOException) {
                Log.e("WallpaperWorker", "Error changing wallpaper", e)
            }
        }
        else {
            repository.setAllWallpapersInRotation()
        }
    }

    private fun createNotificationChannel() {
        val name = "Wallpaper Worker"
        val descriptionText = "Notification for Wallpaper Worker"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showNotification() {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Wallpaper Worker")
            .setContentText("Wallpaper Worker is running")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}