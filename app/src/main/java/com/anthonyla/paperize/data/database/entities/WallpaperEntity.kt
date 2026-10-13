package com.anthonyla.paperize.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    // Legacy: always derived from [folderId] ("DIRECT" or "FOLDER").
    val sourceType: String = "DIRECT",
    val addedAt: Long = System.currentTimeMillis(),

    // Legacy: Paperize only stores images.
    val mediaType: String = "IMAGE",
)
