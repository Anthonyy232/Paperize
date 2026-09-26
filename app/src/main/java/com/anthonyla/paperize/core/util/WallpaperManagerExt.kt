package com.anthonyla.paperize.core.util

import android.app.WallpaperManager
import android.graphics.Bitmap
import java.io.IOException

/**
 * Apply a bitmap and treat WallpaperManager's documented zero return value as a failure.
 */
fun WallpaperManager.setBitmapChecked(bitmap: Bitmap, which: Int): Int {
    val wallpaperId = setBitmap(bitmap, null, true, which)
    requireWallpaperSetSucceeded(wallpaperId)
    return wallpaperId
}

internal fun requireWallpaperSetSucceeded(wallpaperId: Int) {
    if (wallpaperId == 0) {
        throw IOException("WallpaperManager rejected the wallpaper")
    }
}
