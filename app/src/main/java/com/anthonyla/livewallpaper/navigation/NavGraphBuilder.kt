package com.anthonyla.livewallpaper.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.anthonyla.livewallpaper.ui.Library
import com.anthonyla.livewallpaper.ui.Configure
import com.anthonyla.livewallpaper.ui.Wallpaper

/**
 * The bottomNav graph builder links the routes for the different screens of the bottom navigation
 * bar together.
 */
fun NavGraphBuilder.bottomNav(navController: NavController, modifier: Modifier = Modifier) {
    navigation(startDestination = BottomNavScreens.Wallpaper.route, route = "bottomNavigation") {
        composable(BottomNavScreens.Wallpaper.route) { Wallpaper() }
        composable(BottomNavScreens.Library.route) { Library() }
        composable(BottomNavScreens.Configure.route) { Configure() }
    }
}