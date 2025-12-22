package com.anthonyla.paperize.data.mapper

import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.database.relations.AlbumWithDetails
import com.anthonyla.paperize.domain.model.Album

/**
 * Mappers for Album entity <-> domain model conversion
 */

fun AlbumEntity.toDomainModel(
    wallpapers: List<com.anthonyla.paperize.domain.model.Wallpaper> = emptyList(),
    folders: List<com.anthonyla.paperize.domain.model.Folder> = emptyList()
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
    // Optimization: Group wallpapers by folderId once to avoid repeated filtering
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

fun List<AlbumEntity>.toDomainModels(): List<Album> = map { it.toDomainModel() }

fun List<AlbumWithDetails>.toDomainModelsFromRelations(): List<Album> = map { it.toDomainModel() }

fun com.anthonyla.paperize.data.database.entities.AlbumSummaryEntity.toDomainModel(): com.anthonyla.paperize.domain.model.AlbumSummary =
    com.anthonyla.paperize.domain.model.AlbumSummary(
        id = id,
        name = name,
        coverUri = coverUri,
        wallpaperCount = wallpaperCount,
        folderCount = folderCount,
        createdAt = createdAt,
        modifiedAt = modifiedAt
    )

fun List<com.anthonyla.paperize.data.database.entities.AlbumSummaryEntity>.toDomainModelsFromSummaries(): List<com.anthonyla.paperize.domain.model.AlbumSummary> =
    map { it.toDomainModel() }
