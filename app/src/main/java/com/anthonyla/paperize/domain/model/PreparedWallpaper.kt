package com.anthonyla.paperize.domain.model

import android.graphics.Bitmap
import com.anthonyla.paperize.core.ScreenType

/** A decoded and processed wallpaper that has not yet been committed as current. */
data class PreparedWallpaper(
    val bitmap: Bitmap,
    val albumId: String,
    val screenType: ScreenType,
    val wallpaperId: String,
    val shuffle: Boolean
)
