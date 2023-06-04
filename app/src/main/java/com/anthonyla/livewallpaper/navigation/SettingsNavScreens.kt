package com.anthonyla.livewallpaper.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.anthonyla.livewallpaper.R

/**
 * List of screens for app settings
 */
sealed class SettingsNavScreens(val route: String, @StringRes val resourceId: Int, val icon: ImageVector) {
    object Settings : SettingsNavScreens("settings", R.string.settings, Icons.Rounded.Settings)
}