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
    @Inject lateinit var settingsDataStoreImpl: SettingsDataStore

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
        val intent = Intent(this, WallpaperBootAndChangeReceiver::class.java)
        sendBroadcast(intent)
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
    }
}