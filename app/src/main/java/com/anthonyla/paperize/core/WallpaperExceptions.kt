package com.anthonyla.paperize.core

/**
 * Exception thrown when an album has no wallpapers available
 */
class EmptyAlbumException(message: String) : Exception(message)

/**
 * Exception thrown when no valid wallpaper can be found after retries
 */
class NoValidWallpaperException(message: String) : Exception(message)
