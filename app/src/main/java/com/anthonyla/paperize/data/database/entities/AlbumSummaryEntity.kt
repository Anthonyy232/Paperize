package com.anthonyla.paperize.data.database.entities

/**
 * Entity for holding lightweight album summary query results
 */
data class AlbumSummaryEntity(
    val id: String,
    val name: String,
    val coverUri: String?,
    val wallpaperCount: Int,
    val folderCount: Int,
    val createdAt: Long,
    val modifiedAt: Long
)
