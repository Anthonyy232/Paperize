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
data class AddAlbum(val initialAlbumName: String)

/**
 * Data class for WallpaperView screen
 */
@Serializable
data class WallpaperView(val wallpaperUri: String, val wallpaperName: String)

/**
 * Object for FolderView screen
 */
@Serializable
object FolderView

/**
 * Object for SortView screen
 */
@Serializable
object SortView

