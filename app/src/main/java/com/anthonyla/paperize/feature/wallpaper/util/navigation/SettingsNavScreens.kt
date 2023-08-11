package com.anthonyla.paperize.feature.wallpaper.util.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.anthonyla.paperize.R

/**
 * List of screens for app settings
 */
sealed class SettingsNavScreens(val route: String, @StringRes val resourceId: Int,  val unfilledIcon: ImageVector, val filledIcon: ImageVector) {
    object Settings : SettingsNavScreens("settings_screen", R.string.settings_screen, Icons.Outlined.Settings, Icons.Filled.Settings)
}