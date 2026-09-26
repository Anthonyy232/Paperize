package com.anthonyla.paperize.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.presentation.common.navigation.HomeRoute
import com.anthonyla.paperize.presentation.common.navigation.NavigationGraph
import com.anthonyla.paperize.presentation.common.navigation.StartupRoute
import com.anthonyla.paperize.presentation.common.theme.PaperizeTheme
import com.anthonyla.paperize.presentation.screens.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()

            val currentSettings by settingsViewModel.appSettings.collectAsStateWithLifecycle()

            PaperizeTheme(
                darkMode = currentSettings?.darkMode,
                dynamicTheming = currentSettings?.dynamicTheming ?: false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentSettings != null) {
                        NavigationGraph(
                            startDestination = if (currentSettings?.firstLaunch != false) StartupRoute else HomeRoute,
                            animate = currentSettings?.animate ?: true,
                            onFirstLaunchComplete = {
                                settingsViewModel.updateFirstLaunch(false)
                            }
                        )
                    }
                }
            }
        }
    }
}
