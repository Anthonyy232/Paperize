package com.anthonyla.paperize.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anthonyla.paperize.core.WallpaperSourceType

/**
 * Room entity for Wallpaper
 *
 * Properly indexed foreign key to Album and optional foreign key to Folder
 */
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

    @ColumnInfo(name = "albumId")
    val albumId: String,

    @ColumnInfo(name = "folderId")
    val folderId: String? = null,

    val uri: String,
    val fileName: String,
    val dateModified: Long,
    val displayOrder: Int = 0,
    val sourceType: WallpaperSourceType = WallpaperSourceType.DIRECT,
    val addedAt: Long = System.currentTimeMillis()
)
