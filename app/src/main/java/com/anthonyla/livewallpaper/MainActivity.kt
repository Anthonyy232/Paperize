package com.anthonyla.livewallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.anthonyla.livewallpaper.themes.LiveWallpaperTheme
import com.anthonyla.livewallpaper.ui.LiveWallpaperApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveWallpaperTheme {
                LiveWallpaperApp()
            }
        }
    }
}