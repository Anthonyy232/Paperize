package com.anthonyla.paperize.domain.source

import android.graphics.Bitmap
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.Wallpaper

interface WallpaperRenderer {
    /** Decode [wallpaper] with [screen]'s scaling and effects. Returns null if it cannot be read. */
    suspend fun render(wallpaper: Wallpaper, screen: ScreenType, settings: ScheduleSettings): Bitmap?
}
