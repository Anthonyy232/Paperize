package com.anthonyla.paperize.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for Folder
 *
 * No longer stores nested wallpapers - they're separate entities with foreign keys
 */
@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["albumId"]),
        Index(value = ["uri"])
    ]
)
data class FolderEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "albumId")
    val albumId: String,

    val name: String,
    val uri: String,
    val coverUri: String?,
    val dateModified: Long,
    val displayOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
