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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.anthonyla.paperize.presentation.common.theme.PaperizeTheme
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
            // TODO: Get settings from ViewModel
            val darkMode = false // TODO: From settings
            val amoledTheme = false // TODO: From settings
            val dynamicTheming = true // TODO: From settings

            PaperizeTheme(
                darkTheme = darkMode,
                amoledTheme = amoledTheme,
                dynamicColor = dynamicTheming
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // TODO: Navigation graph will go here
                    // PaperizeApp()
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // SET_WALLPAPER permission (required)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SET_WALLPAPER
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_WALLPAPER)) {
                permissionsToRequest.add(Manifest.permission.SET_WALLPAPER)
            }
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
