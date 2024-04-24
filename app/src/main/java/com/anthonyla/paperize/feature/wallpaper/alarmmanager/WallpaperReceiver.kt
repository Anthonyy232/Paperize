package com.anthonyla.paperize.feature.wallpaper.alarmmanager

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class WallpaperReceiver: BroadcastReceiver() {
    @Inject lateinit var repository: SelectedAlbumRepository
    @Inject lateinit var wallpaperScheduler: WallpaperScheduler

    override fun onReceive(context: Context?, intent: Intent?) {
        val defaultInterval = 15L
        val timeInMinutes = intent?.getLongExtra("timeInMinutes", defaultInterval) ?: defaultInterval
        if (context != null) {
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("WallpaperReceiver", "Changing wallpaper")
                changeWallpaper(context)
                wallpaperScheduler.scheduleWallpaperChanger(timeInMinutes, false)
            }
        }
    }

    /**
     * Changes the wallpaper to the next wallpaper in the queue of the selected album
     * If none left, reshuffle the wallpapers and pick the first one
     */
    private suspend fun changeWallpaper(context: Context) {
        val album = repository.getSelectedAlbum().first().firstOrNull()
        album?.let {
            var wallpaper = it.album.wallpapersInQueue.firstOrNull()
            if (wallpaper != null) {
                setWallpaper(context, wallpaper.toUri())
                Log.d("WallpaperReceiver", "Count: ${it.album.wallpapersInQueue.size}")
                /*
                repository.upsertSelectedAlbum(
                    it.copy(
                        album = it.album.copy(
                            wallpapersInQueue = it.album.wallpapersInQueue.drop(1)
                        )
                    )
                )
                 */
            }
            /*
            else {
                val newWallpaperInQueue = it.wallpapers.map { it.wallpaperUri }.shuffled()
                wallpaper = newWallpaperInQueue.firstOrNull()
                if (wallpaper != null) {
                    setWallpaper(context, wallpaper.toUri())
                    Log.d("WallpaperReceiver", "Count before: ${it.album.wallpapersInQueue.size}")
                    repository.upsertSelectedAlbum(
                        it.copy(
                            album = it.album.copy(
                                wallpapersInQueue = newWallpaperInQueue.drop(1)
                            )
                        )
                    )
                    Log.d("WallpaperReceiver", "Count after: ${it.album.wallpapersInQueue.size}")
                }
            }
             */
        }
    }

    private fun setWallpaper(context: Context, wallpaper: Uri) {
        val wallpaperManager = WallpaperManager.getInstance(context)
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(
                    context.contentResolver,
                    wallpaper
                )
                ImageDecoder.decodeBitmap(source)
            } else {
                context.contentResolver.openInputStream(wallpaper)
                    ?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
            }
            bitmap?.let { image -> wallpaperManager.setBitmap(image) }
        } catch (e: IOException) {
            Log.e("WallpaperReceiver", "Error changing wallpaper", e)
        }
    }
}