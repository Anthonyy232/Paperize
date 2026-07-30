package com.anthonyla.paperize.core.util

import android.app.WallpaperManager
import android.graphics.Bitmap
import java.io.IOException

/**
 * Apply a bitmap and treat WallpaperManager's documented zero return value as a failure.
 */
fun WallpaperManager.setBitmapChecked(bitmap: Bitmap, which: Int): Int {
    check(bitmap.width > 0 && bitmap.height > 0) {
        "Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}"
    }
    check(!bitmap.isRecycled) { "Bitmap has been recycled" }

    val wallpaperId = setBitmap(bitmap, null, true, which)
    requireWallpaperSetSucceeded(wallpaperId)
    return wallpaperId
}

internal fun requireWallpaperSetSucceeded(wallpaperId: Int) {
    if (wallpaperId == 0) {
        throw IOException("WallpaperManager rejected the wallpaper")
    }
}
