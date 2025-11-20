package com.anthonyla.paperize.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anthonyla.paperize.core.ScreenType

/**
 * Room entity for Wallpaper Queue
 *
 * Properly manages wallpaper queues instead of storing them as JSON lists in Album
 * Separate queues for HOME and LOCK screens
 */
@Entity(
    tableName = "wallpaper_queue",
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
    indices = [
        Index(value = ["albumId", "screenType"]),
        Index(value = ["wallpaperId"]),
        Index(value = ["queuePosition"])
    ]
)
data class WallpaperQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "albumId")
    val albumId: String,

    @ColumnInfo(name = "wallpaperId")
    val wallpaperId: String,

    val screenType: ScreenType,
    val queuePosition: Int,
    val addedAt: Long = System.currentTimeMillis()
)
