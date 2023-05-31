package com.anthonyla.livewallpaper.ui

import android.app.Activity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.anthonyla.livewallpaper.navigation.BottomNavScreens
import com.anthonyla.livewallpaper.navigation.BottomNavigationBar
import com.anthonyla.livewallpaper.navigation.bottomNav
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveWallpaperApp(modifier: Modifier = Modifier) {
    SetTransparentSystemBars()

    // Bottom navigation
    val navController = rememberNavController()
    val bottomNavOptions = listOf(
        BottomNavScreens.Wallpaper,
        BottomNavScreens.Library,
        BottomNavScreens.Configure
    )
    Scaffold (
        bottomBar = {
            BottomNavigationBar(navController = navController, screens = bottomNavOptions)
        },
    ) {
        innerPadding -> NavHost (
            navController,
            startDestination = "bottomNavigation",
            Modifier.padding(innerPadding)
        ) {
            bottomNav(navController)
        }
    }
}

@Composable
fun SetTransparentSystemBars() {
    val systemUiController = rememberSystemUiController()
    val view = LocalView.current
    SideEffect {
        with(view.context as Activity) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                // check if app is in dark mode or not
                darkIcons = false
            )
        }
    }
}