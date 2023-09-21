package com.anthonyla.paperize.feature.wallpaper.util.navigation

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

sealed class BottomNavScreens(val route: String, @StringRes val resourceId: Int, val unfilledIcon: ImageVector, val filledIcon: ImageVector) {
    object Wallpaper : BottomNavScreens("wallpaper_screen", R.string.wallpaper_screen, Icons.Outlined.Home, Icons.Filled.Home)
    object Library : BottomNavScreens("library_screen", R.string.library_screen,  Icons.Outlined.AddToPhotos,  Icons.Filled.AddToPhotos)
    object Configure : BottomNavScreens("configure_screen", R.string.configure_screen, Icons.Outlined.Settings, Icons.Filled.Settings)
}