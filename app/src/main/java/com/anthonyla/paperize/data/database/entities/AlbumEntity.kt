package com.anthonyla.paperize.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for Album
 *
 * Stores album metadata without nested collections to avoid CursorWindow issues
 */
@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val coverUri: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)
