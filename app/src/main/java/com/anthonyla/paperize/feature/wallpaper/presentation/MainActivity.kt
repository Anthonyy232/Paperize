package com.anthonyla.paperize.feature.wallpaper.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.themes.PaperizeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val addAlbumViewModel: AddAlbumViewModel by viewModels()
    private val albumsViewModel: AlbumsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { settingsViewModel.shouldNotBypassSplashScreen }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PaperizeTheme(settingsViewModel.state) {
                Surface(tonalElevation = 5.dp) {
                    PaperizeApp()
                }
            }
        }
    }
}