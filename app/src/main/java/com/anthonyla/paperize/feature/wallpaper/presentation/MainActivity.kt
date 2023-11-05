package com.anthonyla.paperize.feature.wallpaper.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.themes.PaperizeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val albumsViewModel: AlbumsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            settingsViewModel.shouldNotBypassSplashScreen && albumsViewModel.shouldNotBypassSplashScreen
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val lifecycleEvent = rememberLifecycleEvent()
            LaunchedEffect(lifecycleEvent) {
                if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
                    albumsViewModel.onEvent(AlbumsEvent.RefreshAlbums)
                }
            }
            PaperizeTheme(settingsViewModel.state) {
                Surface(tonalElevation = 5.dp) {
                    PaperizeApp()
                }
            }
        }
    }
}

@Composable
fun rememberLifecycleEvent(lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current): Lifecycle.Event {
    var state by remember { mutableStateOf(Lifecycle.Event.ON_ANY) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            state = event
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return state
}