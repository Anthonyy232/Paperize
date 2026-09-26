package com.anthonyla.paperize.data.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.database.entities.FolderEntity
import com.anthonyla.paperize.data.database.entities.WallpaperEntity

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
)
