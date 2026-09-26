package com.anthonyla.paperize.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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

    val albumId: String,

    val name: String,
    val uri: String,
    val coverUri: String?,
    val dateModified: Long,
    val displayOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
