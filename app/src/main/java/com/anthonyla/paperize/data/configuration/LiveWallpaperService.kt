package com.anthonyla.paperize.data.configuration

import android.service.wallpaper.WallpaperService

class LiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = WallpaperEngine()
    inner class WallpaperEngine : WallpaperService.Engine() {}
}