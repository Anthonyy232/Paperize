package com.anthonyla.paperize.service.tile

import android.content.Intent
import android.service.quicksettings.TileService
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService

/**
 * Quick Settings Tile for changing wallpaper
 */
class WallpaperTileService : TileService() {

    override fun onClick() {
        super.onClick()

        // Start wallpaper change service
        val intent = Intent(this, WallpaperChangeService::class.java).apply {
            action = Constants.ACTION_CHANGE_WALLPAPER
            putExtra(Constants.EXTRA_SCREEN_TYPE, ScreenType.BOTH.name)
        }

        // minSdk is 31, so startForegroundService is always available
        startForegroundService(intent)
    }
}
