package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper

import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper

data class WallpaperState (
    val wallpapers: List<Wallpaper> = emptyList()
)