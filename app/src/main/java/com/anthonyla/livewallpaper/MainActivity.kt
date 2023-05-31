package com.anthonyla.livewallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.anthonyla.livewallpaper.data.settings.SettingsViewModel
import com.anthonyla.livewallpaper.themes.LiveWallpaperTheme
import com.anthonyla.livewallpaper.ui.LiveWallpaperApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveWallpaperTheme {
                LiveWallpaperApp(settingsViewModel)
            }
        }
    }
}