package com.anthonyla.paperize.feature.wallpaper.util.navigation

import kotlinx.serialization.Serializable


/**
 * Data class for Startup screen
 */
@Serializable
object Startup

/**
 * Object for Notification screen
 */
@Serializable
object Notification

/**
 * Object for Home screen
 */
@Serializable
object Home

/**
 * Object for Settings screen
 */
@Serializable
object Settings

/**
 * Object for Licenses screen
 */
@Serializable
object Licenses

/**
 * Object for Privacy screen
 */
@Serializable
object Privacy

/**
 * Data class for AlbumView screen
 */
@Serializable
data class AlbumView(val initialAlbumName: String)

/**
 * Data class for AddEdit screen
 */
@Serializable
data class AddEdit(val wallpaper: String)

/**
 * Data class for WallpaperView screen
 */
@Serializable
data class WallpaperView(val wallpaper: String)

/**
 * Data class for FolderView screen
 */
@Serializable
data class FolderView(val folderName: String?, val wallpapers: List<String>)