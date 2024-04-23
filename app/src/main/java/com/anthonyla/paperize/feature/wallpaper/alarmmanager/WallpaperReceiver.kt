package com.anthonyla.paperize.feature.wallpaper.alarmmanager

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class WallpaperReceiver: BroadcastReceiver() {
    @Inject lateinit var repository: SelectedAlbumRepository
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("WallpaperReceiver", "Changing wallpaper. Available wallpaper: " + repository.countWallpapersInRotation())
                changeWallpaper(context)
            }
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
                bitmap?.let { image -> wallpaperManager.setBitmap(image) }
                repository.upsertWallpaper(wallpaper.copy(isInRotation = false))
            } catch (e: IOException) {
                Log.e("WallpaperReceiver", "Error changing wallpaper", e)
            }
        }
        else {
            repository.setAllWallpapersInRotation()
        }
    }
}