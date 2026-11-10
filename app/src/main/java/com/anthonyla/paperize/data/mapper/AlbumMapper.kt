package com.anthonyla.paperize.data.mapper

import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.database.entities.AlbumSummaryEntity
import com.anthonyla.paperize.data.database.relations.AlbumWithDetails
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper

fun AlbumEntity.toDomainModel(
    wallpapers: List<Wallpaper> = emptyList(),
    folders: List<Folder> = emptyList()
): Album = Album(
    id = id,
    name = name,
    coverUri = coverUri,
    wallpapers = wallpapers,
    folders = folders,
    createdAt = createdAt,
    modifiedAt = modifiedAt
)

fun Album.toEntity(): AlbumEntity = AlbumEntity(
    id = id,
    name = name,
    coverUri = coverUri,
    createdAt = createdAt,
    modifiedAt = modifiedAt
)

fun AlbumWithDetails.toDomainModel(): Album {
    val wallpapersByFolder = wallpapers.groupBy { it.folderId }
    
    val domainWallpapers = wallpapersByFolder[null]?.map { it.toDomainModel() } ?: emptyList()

    val domainFolders = folders.map { folderEntity ->
        val folderWallpapers = wallpapersByFolder[folderEntity.id]?.map { it.toDomainModel() } ?: emptyList()
        folderEntity.toDomainModel(folderWallpapers)
    }

    return album.toDomainModel(
        wallpapers = domainWallpapers,
        folders = domainFolders
    )
}

fun AlbumSummaryEntity.toDomainModel(): AlbumSummary = AlbumSummary(
    id = id,
    name = name,
    coverUri = coverUri,
    wallpaperCount = wallpaperCount,
    folderCount = folderCount,
    createdAt = createdAt,
    modifiedAt = modifiedAt
)
