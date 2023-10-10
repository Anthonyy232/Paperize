package com.anthonyla.paperize.feature.wallpaper.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsState
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.themes.PaperizeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { settingsViewModel.shouldNotBypassSplashScreen }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PaperizeTheme(
                isDarkMode = isDarkMode(settingsState = settingsViewModel.state.value),
                isDynamicTheming = isDynamicTheming(settingsState = settingsViewModel.state.value)
            ) {
                Surface(tonalElevation = 5.dp) {
                    PaperizeApp()
                }
            }
        }
    }
}

@Composable
private fun isDarkMode(settingsState: SettingsState): Boolean =
    when(settingsState.darkMode) {
        true -> true // User enabled dark mode
        false -> false // User enabled light mode
        else -> isSystemInDarkTheme() // System default
    }

@Composable
private fun isDynamicTheming(settingsState: SettingsState): Boolean =
    when(settingsState.dynamicTheming) {
        true -> true
        false -> false
    }