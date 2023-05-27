package com.anthonyla.livewallpaper.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.anthonyla.livewallpaper.R

sealed class BottomNavScreens(val route: String, @StringRes val resourceId: Int, val icon: ImageVector) {
    object Wallpaper : BottomNavScreens("wallpaper", R.string.wallpaper, Icons.Filled.Home)
    object Library : BottomNavScreens("library", R.string.library, Icons.Filled.List)
    object Setting : BottomNavScreens("setting", R.string.setting, Icons.Filled.Settings)
}