package com.anthonyla.paperize.service.tile

import android.service.quicksettings.TileService
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService

class WallpaperTileService : TileService() {
    override fun onClick() {
        super.onClick()
        WallpaperChangeService.changeWallpaper(this)
    }
}
