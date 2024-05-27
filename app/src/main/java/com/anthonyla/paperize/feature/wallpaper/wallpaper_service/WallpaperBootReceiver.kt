package com.anthonyla.paperize.feature.wallpaper.wallpaper_service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.data.settings.SettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class WallpaperBootReceiver : BroadcastReceiver() {
    @Inject lateinit var settingsDataStoreImpl: SettingsDataStore
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val serviceIntent = Intent(context, WallpaperService::class.java)
            serviceIntent.action = WallpaperService.Actions.START.toString()
            runBlocking {
                val timeInMinutes1 = settingsDataStoreImpl.getInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
                val timeInMinutes2 = settingsDataStoreImpl.getInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
                val scheduleSeparately = settingsDataStoreImpl.getBoolean(SettingsConstants.SCHEDULE_SEPARATELY) ?: false
                serviceIntent.putExtra("timeInMinutes1", timeInMinutes1)
                serviceIntent.putExtra("timeInMinutes2", timeInMinutes2)
                serviceIntent.putExtra("scheduleSeparately", scheduleSeparately)
            }
            context.startForegroundService(serviceIntent)
        }
    }
}