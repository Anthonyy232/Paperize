package com.anthonyla.livewallpaper.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.anthonyla.livewallpaper.ui.Library
import com.anthonyla.livewallpaper.ui.Setting
import com.anthonyla.livewallpaper.ui.Wallpaper

fun NavGraphBuilder.bottomNav(navController: NavController, modifier: Modifier = Modifier) {
    navigation(startDestination = BottomNavScreens.Wallpaper.route, route = "bottomNavigation") {
        composable(BottomNavScreens.Wallpaper.route) { Wallpaper() }
        composable(BottomNavScreens.Library.route) { Library() }
        composable(BottomNavScreens.Setting.route) { Setting() }
    }
}