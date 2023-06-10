package com.anthonyla.paperize.data.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.navigation
import com.anthonyla.paperize.ui.screens.Library
import com.anthonyla.paperize.ui.screens.Configure
import com.anthonyla.paperize.ui.screens.Settings
import com.anthonyla.paperize.ui.screens.Wallpaper

/**
 * The Nav graph builder links the routes for the different screens in the app
 */

@ExperimentalAnimationApi
fun NavGraphBuilder.navGraph(navController: NavController, modifier: Modifier = Modifier) {
    navigation(startDestination = BottomNavScreens.Wallpaper.route, route = "bottomNavigation") {
        composable(BottomNavScreens.Wallpaper.route) { Wallpaper(navController) }
        composable(BottomNavScreens.Library.route) { Library(navController) }
        composable(BottomNavScreens.Configure.route) { Configure(navController) }
        composable(SettingsNavScreens.Settings.route) { Settings(navController) }
    }
}