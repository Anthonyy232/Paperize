package com.anthonyla.paperize.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.anthonyla.paperize.core.ScreenType

/**
 * Room entity tracking the last-applied wallpaper per (album, screen).
 *
 * A CASCADE on both FKs means: if the album or wallpaper is deleted the entry
 * disappears automatically — the service will then fall back to a normal queue
 * advance instead of reapplying a stale URI.
 */
@Entity(
    tableName = "wallpaper_current",
    primaryKeys = ["albumId", "screenType"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WallpaperEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallpaperId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["wallpaperId"])]
)
data class WallpaperCurrentEntity(
    @ColumnInfo(name = "albumId")
    val albumId: String,

    val screenType: ScreenType,

    @ColumnInfo(name = "wallpaperId")
    val wallpaperId: String,

    val appliedAt: Long = System.currentTimeMillis()
)
