package com.anthonyla.paperize.feature.wallpaper.presentation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anthonyla.paperize.core.presentation.components.BottomNavigationBar
import com.anthonyla.paperize.core.presentation.components.SetTransparentSystemBars
import com.anthonyla.paperize.core.presentation.components.TopAppBar
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.util.navigation.BottomNavScreens
import com.anthonyla.paperize.feature.wallpaper.util.navigation.navGraph

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PaperizeApp(
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    SetTransparentSystemBars(viewModel.isDarkMode())
    val navController = rememberNavController()

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
    ) { innerPadding -> NavHost (
            navController,
            startDestination = "bottomNavigation",
            Modifier.padding(innerPadding)
        ) {
            navGraph(navController)
        }
    }
}