package com.anthonyla.paperize.data.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.anthonyla.paperize.ui.screens.Library
import com.anthonyla.paperize.ui.screens.Configure
import com.anthonyla.paperize.ui.screens.Settings
import com.anthonyla.paperize.ui.screens.Wallpaper

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