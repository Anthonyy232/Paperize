package com.anthonyla.paperize.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey

/** A folder image the user removed, so folder rescans skip it. Keeps the row to restore it as it was. */
@Entity(
    tableName = "excluded_wallpapers",
    primaryKeys = ["folderId", "uri"],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExcludedWallpaperEntity(
    val folderId: String,
    val uri: String,
    val id: String,
    val fileName: String,
    val dateModified: Long,
    val displayOrder: Int,
    val addedAt: Long
)
