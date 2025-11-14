package com.anthonyla.paperize.presentation

import android.Manifest
import android.app.WallpaperManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anthonyla.paperize.presentation.common.navigation.HomeRoute
import com.anthonyla.paperize.presentation.common.navigation.NavigationGraph
import com.anthonyla.paperize.presentation.common.navigation.StartupRoute
import com.anthonyla.paperize.presentation.common.theme.PaperizeTheme
import com.anthonyla.paperize.presentation.screens.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for Paperize
 *
 * Single activity architecture with Jetpack Compose
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request necessary permissions
        requestPermissions()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val appSettings by settingsViewModel.appSettings.collectAsState()
            var firstLaunch by remember { mutableStateOf(appSettings.firstLaunch) }

            PaperizeTheme(
                darkTheme = appSettings.darkMode,
                amoledTheme = appSettings.amoledTheme,
                dynamicColor = appSettings.dynamicTheming
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationGraph(
                        startDestination = if (firstLaunch) StartupRoute else HomeRoute,
                        onFirstLaunchComplete = {
                            firstLaunch = false
                            // Update settings to mark first launch as complete
                        }
                    )
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // SET_WALLPAPER permission (required)
        // SET_WALLPAPER permission (always available since minSdk is 31)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SET_WALLPAPER
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.SET_WALLPAPER)
        }

        // POST_NOTIFICATIONS permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
