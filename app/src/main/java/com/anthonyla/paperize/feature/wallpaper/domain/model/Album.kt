package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Album model
 *
 * @param initialAlbumName The initial album name -- should not be changed as it is used as the key for database queries
 * @param displayedAlbumName The displayed album name
 * @param coverUri The cover uri of the album
 * @param homeWallpapersInQueue The list of home wallpapers in the queue
 * @param lockWallpapersInQueue The list of lock wallpapers in the queue
 * @param selected Whether the album is selected
 */
@Entity(indices = [Index(value = ["selected"])])
data class Album(
    @PrimaryKey(autoGenerate = false) val initialAlbumName: String,
    val displayedAlbumName: String,
    val coverUri: String?,
    val homeWallpapersInQueue: List<String> = emptyList(),
    val lockWallpapersInQueue: List<String> = emptyList(),
    val selected : Boolean = false
)