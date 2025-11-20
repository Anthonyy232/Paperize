package com.anthonyla.paperize.data.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.database.entities.FolderEntity
import com.anthonyla.paperize.data.database.entities.WallpaperEntity

/**
 * Room relation combining Album with its Wallpapers and Folders
 *
 * This replaces the old AlbumWithWallpaperAndFolder but with proper foreign keys
 * No nested collections in entities - avoiding CursorWindow issues!
 */
data class AlbumWithDetails(
    @Embedded
    val album: AlbumEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "albumId",
        entity = WallpaperEntity::class
    )
    val wallpapers: List<WallpaperEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "albumId",
        entity = FolderEntity::class
    )
    val folders: List<FolderEntity> = emptyList()
) {
    /**
     * Get direct wallpapers (not from folders)
     */
    val directWallpapers: List<WallpaperEntity>
        get() = wallpapers.filter { it.folderId == null }

    /**
     * Get total count including folder wallpapers
     */
    fun getTotalWallpaperCount(folderWallpapers: Map<String, Int>): Int {
        val directCount = directWallpapers.size
        val folderCount = folders.sumOf { folderWallpapers[it.id] ?: 0 }
        return directCount + folderCount
    }
}
