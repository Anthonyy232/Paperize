package com.anthonyla.paperize.feature.wallpaper.util.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrowseGallery
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BrowseGallery
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.anthonyla.paperize.R

/**
 * Enum class for navigation screens in application
 */
sealed class NavScreens(val route: String, @StringRes val resourceId: Int, val unfilledIcon: ImageVector, val filledIcon: ImageVector) {
    /**
     * Home screen that the user lands on initially
     */
    object Home : NavScreens("home_screen", R.string.home_screen, Icons.Outlined.Image, Icons.Filled.Image)

    /**
     * Settings screen for the application
     */
    object Settings : NavScreens("settings_screen", R.string.settings_screen, Icons.Outlined.Settings, Icons.Filled.Settings)

    /**
     * Add/Edit screen for album when adding or editing
     */
    object AddEdit : NavScreens("add_edit_screen", R.string.add_wallpaper_screen, Icons.Outlined.Add, Icons.Filled.Add)

    /**
     * Wallpaper view screen for viewing wallpapers when clicking on a wallpaper
     */
    object WallpaperView : NavScreens("wallpaper_view_screen", R.string.wallpaper_view, Icons.Outlined.BrowseGallery, Icons.Filled.BrowseGallery)

    /**
     * Folder view screen for viewing wallpapers in a folder when clicking on a folder
     */
    object FolderView : NavScreens("folder_view_screen", R.string.folder_view, Icons.Outlined.BrowseGallery, Icons.Filled.BrowseGallery)

    /**
     * Album view screen for viewing albums when clicking on an album. Shows wallpapers and folders of the album
     */
    object AlbumView : NavScreens("album_view_screen", R.string.album_view, Icons.Outlined.BrowseGallery, Icons.Filled.BrowseGallery)
}