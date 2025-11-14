package com.anthonyla.paperize.service.tile

import android.content.Intent
import android.os.Build
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
