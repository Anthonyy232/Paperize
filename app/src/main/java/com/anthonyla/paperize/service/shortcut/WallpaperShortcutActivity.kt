package com.anthonyla.paperize.service.shortcut

import android.app.Activity
import android.os.Bundle
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService

/** Invisible launcher-shortcut entry point that changes the configured wallpaper target. */
class WallpaperShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WallpaperChangeService.changeWallpaper(this)
        finish()
    }
}
