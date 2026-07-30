package com.anthonyla.paperize.domain.model

import android.graphics.Bitmap
import com.anthonyla.paperize.core.ScreenType

/**
 * A decoded and processed wallpaper that has not yet been committed as current.
 *
 * The queue item is only committed after WallpaperManager confirms that the platform accepted
 * the bitmap. If applying fails, callers restore this item to the front of its queue.
 */
data class PreparedWallpaper(
    val bitmap: Bitmap,
    val albumId: String,
    val screenType: ScreenType,
    val wallpaperId: String,
    val shuffle: Boolean
)
