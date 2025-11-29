package com.anthonyla.paperize.presentation.common.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using kotlinx.serialization
 */

@Serializable
object StartupRoute

@Serializable
object NotificationRoute

@Serializable
object StoragePermissionRoute

@Serializable
object WallpaperModeSelectionRoute

@Serializable
object HomeRoute

@Serializable
data class AlbumRoute(val albumId: String)

@Serializable
data class FolderRoute(val folderId: String)

@Serializable
data class WallpaperViewRoute(val wallpaperUri: String, val wallpaperName: String)

@Serializable
data class SortRoute(val albumId: String)

@Serializable
object SettingsRoute

@Serializable
object PrivacyRoute
