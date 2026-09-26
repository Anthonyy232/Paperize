package com.anthonyla.paperize.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anthonyla.paperize.core.WallpaperMediaType
import com.anthonyla.paperize.core.WallpaperSourceType

@Entity(
    tableName = "wallpapers",
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["albumId"]),
        Index(value = ["folderId"]),
        Index(value = ["uri"])
    ]
)
data class WallpaperEntity(
    @PrimaryKey
    val id: String,

    val albumId: String,

    val folderId: String? = null,

    val uri: String,
    val fileName: String,
    val dateModified: Long,
    val displayOrder: Int = 0,
    val sourceType: WallpaperSourceType = WallpaperSourceType.DIRECT,
    val addedAt: Long = System.currentTimeMillis(),

    val mediaType: WallpaperMediaType = WallpaperMediaType.IMAGE,
)
