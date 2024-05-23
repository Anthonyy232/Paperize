package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Album model
 *
 * @param initialAlbumName The initial album name -- should not be changed as it is used as the key for database queries
 * @param displayedAlbumName The displayed album name
 * @param coverUri The cover uri of the album
 * @param initialized Whether the album has been initialized
 * @param homeWallpapersInQueue The wallpapers in the queue to be displayed for the home screen or when the user schedules both lock and home together
 * @param lockWallpapersInQueue The wallpapers in the queue to be displayed for the lock screen when scheduled separately
 * @param currentHomeWallpaper The current wallpaper for the home screen
 * @param currentLockWallpaper The current wallpaper for the lock screen
 */
@Entity
data class Album(
    @PrimaryKey(autoGenerate = false) val initialAlbumName: String,
    val displayedAlbumName: String,
    val coverUri: String?,
    val initialized: Boolean = false,
    val homeWallpapersInQueue: List<String> = emptyList(),
    val lockWallpapersInQueue: List<String> = emptyList(),
    val currentHomeWallpaper: String? = null,
    val currentLockWallpaper: String? = null
)