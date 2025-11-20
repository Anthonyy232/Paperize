package com.anthonyla.paperize.data.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.anthonyla.paperize.data.database.entities.FolderEntity
import com.anthonyla.paperize.data.database.entities.WallpaperEntity

/**
 * Room relation combining Folder with its Wallpapers
 */
data class FolderWithWallpapers(
    @Embedded
    val folder: FolderEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "folderId",
        entity = WallpaperEntity::class
    )
    val wallpapers: List<WallpaperEntity> = emptyList()
)
