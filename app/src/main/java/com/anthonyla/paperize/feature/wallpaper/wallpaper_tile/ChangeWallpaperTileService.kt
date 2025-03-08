package com.anthonyla.paperize.feature.wallpaper.wallpaper_tile

import android.content.Intent
import android.service.quicksettings.TileService
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperBootAndChangeReceiver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Service for the quick settings tile to change the wallpaper
 */
@AndroidEntryPoint
class ChangeWallpaperTileService: TileService() {
    companion object {
        const val ACTION_CHANGE_WALLPAPER = "com.anthonyla.paperize.SHORTCUT"
    }

    @Inject
    lateinit var settingsDataStoreImpl: SettingsDataStore

    override fun onTileAdded() {
        super.onTileAdded()
    }

    override fun onStartListening() {
        super.onStartListening()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(ACTION_CHANGE_WALLPAPER).apply {
            setClass(this@ChangeWallpaperTileService, WallpaperBootAndChangeReceiver::class.java)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        sendBroadcast(intent)
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
    }
}