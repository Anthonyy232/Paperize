package com.anthonyla.livewallpaper.data.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.anthonyla.livewallpaper.ui.screens.Library
import com.anthonyla.livewallpaper.ui.screens.Configure
import com.anthonyla.livewallpaper.ui.screens.Settings
import com.anthonyla.livewallpaper.ui.screens.Wallpaper

/**
 * The Nav graph builder links the routes for the different screens in the app
 */
fun NavGraphBuilder.navGraph(navController: NavController, modifier: Modifier = Modifier) {
    navigation(startDestination = BottomNavScreens.Wallpaper.route, route = "bottomNavigation") {
        composable(BottomNavScreens.Wallpaper.route) { Wallpaper(navController) }
        composable(BottomNavScreens.Library.route) { Library(navController) }
        composable(BottomNavScreens.Configure.route) { Configure(navController) }
        composable(SettingsNavScreens.Settings.route) { Settings(navController) }
    }
}