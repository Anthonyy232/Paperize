package com.anthonyla.livewallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import com.anthonyla.livewallpaper.theming.LiveWallpaperTheme
import androidx.compose.ui.graphics.Color
import com.anthonyla.livewallpaper.ui.LiveWallpaperApp
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            // Update the status bar to transparent
            val systemUiController = rememberSystemUiController()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            DisposableEffect(systemUiController) {
                systemUiController.setSystemBarsColor(color = Color.Transparent)
                onDispose {}
            }
            systemUiController.setSystemBarsColor(color = Color.Transparent)

            LiveWallpaperTheme {
                LiveWallpaperApp()
            }
        }
    }
}