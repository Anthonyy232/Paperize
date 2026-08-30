package com.anthonyla.paperize.service.tile

import android.content.Intent
import android.service.quicksettings.TileService
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService

/**
 * Quick Settings Tile for changing wallpaper
 */
class WallpaperTileService : TileService() {

    override fun onClick() {
        super.onClick()

        startForegroundService(
            Intent(this, WallpaperChangeService::class.java).apply {
                action = WallpaperChangeService.ACTION_CHANGE_WALLPAPER_AUTO
            }
        )
    }
}
