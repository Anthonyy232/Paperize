package com.anthonyla.livewallpaper.ui

import android.app.Activity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.anthonyla.livewallpaper.data.settings.SettingsViewModel
import com.anthonyla.livewallpaper.navigation.BottomNavScreens
import com.anthonyla.livewallpaper.navigation.BottomNavigationBar
import com.anthonyla.livewallpaper.navigation.TopBar
import com.anthonyla.livewallpaper.navigation.navGraph
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveWallpaperApp(
    isDark: Boolean,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    // Transparent system bars
    SetTransparentSystemBars(isDark)

    // Bottom navigation
    val bottomNavOptions = listOf(
        BottomNavScreens.Wallpaper,
        BottomNavScreens.Library,
        BottomNavScreens.Configure
    )
    Scaffold (
        topBar = {
            TopBar(navController = navController)
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, screens = bottomNavOptions)
        },
    ) { innerPadding -> NavHost (
            navController,
            startDestination = "bottomNavigation",
            Modifier.padding(innerPadding)
        ) {
            navGraph(navController)
        }
    }
}

@Composable
fun SetTransparentSystemBars(darkMode: Boolean) {
    val systemUiController = rememberSystemUiController()
    val view = LocalView.current
    SideEffect {
        with(view.context as Activity) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = !darkMode
            )
        }
    }
}