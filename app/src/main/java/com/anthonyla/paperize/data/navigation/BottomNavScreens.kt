package com.anthonyla.paperize.data.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddToPhotos
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.anthonyla.paperize.R

/**
 * List of screens for the bottom navigation bar
 */
sealed class BottomNavScreens(val route: String, @StringRes val resourceId: Int, val unfilledIcon: ImageVector, val filledIcon: ImageVector) {
    object Wallpaper : BottomNavScreens("wallpaper", R.string.wallpaper, Icons.Outlined.Home, Icons.Filled.Home)
    object Library : BottomNavScreens("library", R.string.library,  Icons.Outlined.AddToPhotos,  Icons.Filled.AddToPhotos)
    object Configure : BottomNavScreens("configure", R.string.configure, Icons.Outlined.Settings, Icons.Filled.Settings)
}