package com.anthonyla.livewallpaper.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.anthonyla.livewallpaper.R

/**
 * List of screens for the bottom navigation bar
 */
sealed class BottomNavScreens(val route: String, @StringRes val resourceId: Int, val icon: ImageVector) {
    object Wallpaper : BottomNavScreens("wallpaper", R.string.wallpaper, Icons.Rounded.Home)
    object Library : BottomNavScreens("library", R.string.library, Icons.Rounded.Menu)
    object Configure : BottomNavScreens("configure", R.string.configure, Icons.Rounded.Settings)

}