package com.anthonyla.livewallpaper.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.anthonyla.livewallpaper.R

/**
 * BottomNavScreens groups the different screens of the bottom navigation bar together.
 * Each object represents a screen and has a route for the NavController, the name of the route,
 * and a icon that is shown on the bottom navigation bar.
 */
sealed class BottomNavScreens(val route: String, @StringRes val resourceId: Int, val icon: ImageVector) {
    object Wallpaper : BottomNavScreens("wallpaper", R.string.wallpaper, Icons.Filled.Home)
    object Library : BottomNavScreens("library", R.string.library, Icons.Filled.List)
    object Configure : BottomNavScreens("configure", R.string.configure, Icons.Filled.Settings)

}