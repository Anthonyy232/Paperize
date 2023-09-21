package com.anthonyla.paperize.feature.wallpaper.util.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.anthonyla.paperize.R

sealed class AddEditNavScreens(val route: String, @StringRes val resourceId: Int, val unfilledIcon: ImageVector, val filledIcon: ImageVector) {
    object ImageAdd : AddEditNavScreens("image_add_screen", R.string.add_image, Icons.Filled.PhotoLibrary, Icons.Filled.PhotoLibrary)
}