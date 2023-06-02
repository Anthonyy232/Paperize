package com.anthonyla.livewallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
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
            LiveWallpaperTheme (isDarkMode(), isDynamicTheming()) {
                Surface(elevation = 5.dp) {
                    LiveWallpaperApp(isDarkMode(), settingsViewModel)
                }
            }
        }
    }

    @Composable
    private fun isDarkMode(): Boolean {
        return when (settingsViewModel.getDarkMode()) {
            true -> true
            false -> false
            null -> isSystemInDarkTheme()
        }
    }

    private fun isDynamicTheming(): Boolean {
        return when (settingsViewModel.getDynamicTheme()) {
            true, null -> true
            false -> false
        }
    }
}
