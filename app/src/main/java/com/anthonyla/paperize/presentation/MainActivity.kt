package com.anthonyla.paperize.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.domain.model.AppSettings
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()

            // Track if DataStore has loaded at least once
            // This prevents the loading screen from showing again if the composable is recomposed
            var hasLoadedDataStore by remember { mutableStateOf(false) }

            val appSettings by settingsViewModel.appSettings.collectAsStateWithLifecycle()

            // Wait for the first real DataStore emission (not the default value)
            // This prevents flickering to onboarding screen on app start
            LaunchedEffect(Unit) {
                snapshotFlow { appSettings }
                    .collect { settings ->
                        // Mark as loaded once we get a value different from the default
                        // OR if we've already marked it as loaded (prevents reset on recomposition)
                        if (settings != AppSettings.default() || hasLoadedDataStore) {
                            hasLoadedDataStore = true
                        }
                    }
            }

            PaperizeTheme(
                darkMode = appSettings.darkMode,
                dynamicTheming = appSettings.dynamicTheming
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Only render navigation once DataStore has loaded
                    // After first load, always show navigation even during updates
                    if (hasLoadedDataStore) {
                        NavigationGraph(
                            startDestination = if (appSettings.firstLaunch) StartupRoute else HomeRoute,
                            animate = appSettings.animate,
                            onFirstLaunchComplete = {
                                settingsViewModel.updateFirstLaunch(false)
                            }
                        )
                    } else {
                        // Show blank screen briefly while DataStore loads on first start (usually <100ms)
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            // Empty - just shows background color while DataStore loads
                        }
                    }
                }
            }
        }
    }
}
