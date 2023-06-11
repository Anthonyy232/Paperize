package com.anthonyla.paperize

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.anthonyla.paperize.data.configuration.LiveWallpaperService
import com.anthonyla.paperize.data.settings.SettingsViewModel
import com.anthonyla.paperize.themes.PaperizeTheme
import com.anthonyla.paperize.ui.PaperizeApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, LiveWallpaperService::class.java)
        )
        startActivity(intent) */

        setTheme(R.style.Theme_Paperize)
        setContent {
            PaperizeTheme (settingsViewModel) {
                PaperizeApp(settingsViewModel)
            }
        }
    }
}
