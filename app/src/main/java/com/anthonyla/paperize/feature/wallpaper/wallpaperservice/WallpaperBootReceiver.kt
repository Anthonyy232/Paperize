package com.anthonyla.paperize.feature.wallpaper.wallpaperservice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class WallpaperBootReceiver : BroadcastReceiver() {
    @Inject lateinit var settingsDataStoreImpl: SettingsDataStore
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Log.d("PaperizeWallpaperChanger", "Boot completed")
            val serviceIntent = Intent(context, WallpaperService::class.java)
            serviceIntent.action = WallpaperService.Actions.START.toString()
            val timeInMinutes = runBlocking { settingsDataStoreImpl.getInt(SettingsConstants.WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT }
            serviceIntent.putExtra("timeInMinutes", timeInMinutes)
            context.startForegroundService(serviceIntent);
        }
    }
}