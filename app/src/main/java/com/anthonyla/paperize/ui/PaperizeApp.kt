package com.anthonyla.paperize.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import com.anthonyla.paperize.data.settings.SettingsViewModel
import com.anthonyla.paperize.data.navigation.BottomNavScreens
import com.anthonyla.paperize.data.navigation.navGraph
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PaperizeApp(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    SetTransparentSystemBars(settingsViewModel.isDarkMode())
    val navController = rememberAnimatedNavController()

    // Hide top level bar and bottom navigation bar when the current screen is not the top level screen
    var topLevel by rememberSaveable { mutableStateOf(true) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    topLevel = when (navBackStackEntry?.destination?.route) {
        BottomNavScreens.Wallpaper.route,
        BottomNavScreens.Library.route,
        BottomNavScreens.Configure.route -> true
        else -> false
    }

    Scaffold (
        topBar = { TopAppBar(navController = navController, topLevel) },
        bottomBar = {
            if (topLevel) BottomNavigationBar(
                navController = navController,
                screens = listOf(
                    BottomNavScreens.Wallpaper,
                    BottomNavScreens.Library,
                    BottomNavScreens.Configure
                )
            )
        }
    ) { innerPadding -> AnimatedNavHost (
            navController,
            startDestination = "bottomNavigation",
            Modifier.padding(innerPadding)
        ) {
            navGraph(navController)
        }
    }
}