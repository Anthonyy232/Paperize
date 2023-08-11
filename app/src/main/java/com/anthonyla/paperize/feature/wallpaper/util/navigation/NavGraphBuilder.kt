package com.anthonyla.paperize.feature.wallpaper.util.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.navigation
import com.anthonyla.paperize.feature.wallpaper.presentation.library.LibraryScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.configure.ConfigureScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper.WallpaperScreen

/**
 * The Nav graph builder links the routes for the different screens in the app
 */

@ExperimentalAnimationApi
fun NavGraphBuilder.navGraph(navController: NavController, modifier: Modifier = Modifier) {
    navigation(startDestination = BottomNavScreens.Wallpaper.route, route = "bottomNavigation") {
        composable(BottomNavScreens.Wallpaper.route) { WallpaperScreen(navController) }
        composable(BottomNavScreens.Library.route) { LibraryScreen(navController) }
        composable(BottomNavScreens.Configure.route) { ConfigureScreen(navController) }
        composable(SettingsNavScreens.Settings.route) { SettingsScreen(navController) }
    }
}