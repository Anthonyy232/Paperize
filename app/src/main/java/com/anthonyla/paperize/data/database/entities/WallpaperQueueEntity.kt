package com.anthonyla.paperize.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anthonyla.paperize.core.ScreenType

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

    val albumId: String,

    val wallpaperId: String,

    val screenType: ScreenType,
    val queuePosition: Int,
    val addedAt: Long = System.currentTimeMillis()
)
