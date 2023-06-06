package com.anthonyla.paperize.ui

import android.app.Activity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.anthonyla.paperize.data.settings.SettingsViewModel
import com.anthonyla.paperize.data.navigation.BottomNavScreens
import com.anthonyla.paperize.data.navigation.navGraph
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PaperizeApp(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    SetTransparentSystemBars(settingsViewModel.isDarkMode())

    val navController = rememberAnimatedNavController()
    var topLevel by rememberSaveable { mutableStateOf(true) }

    // Hide top level bar and bottom navigation bar when the current screen is not the top level screen
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    topLevel = when (navBackStackEntry?.destination?.route) {
        BottomNavScreens.Wallpaper.route,
        BottomNavScreens.Library.route,
        BottomNavScreens.Configure.route -> true
        else -> false
    }

    val bottomNavOptions = listOf(
        BottomNavScreens.Wallpaper,
        BottomNavScreens.Library,
        BottomNavScreens.Configure
    )

    Scaffold (
        topBar = {
            if (topLevel) TopLevelBar(navController = navController)
        },
        bottomBar = {
            if (topLevel) BottomNavigationBar(
                navController = navController,
                screens = bottomNavOptions
            ) }
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