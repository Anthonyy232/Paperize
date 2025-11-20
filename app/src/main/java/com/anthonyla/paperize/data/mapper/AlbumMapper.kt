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
    val domainWallpapers = wallpapers
        .filter { it.folderId == null } // Only direct wallpapers
        .map { it.toDomainModel() }

    val domainFolders = folders.map { folderEntity ->
        val folderWallpapers = wallpapers
            .filter { it.folderId == folderEntity.id }
            .map { it.toDomainModel() }
        folderEntity.toDomainModel(folderWallpapers)
    }

    return album.toDomainModel(
        wallpapers = domainWallpapers,
        folders = domainFolders
    )
}

fun List<AlbumEntity>.toDomainModels(): List<Album> = map { it.toDomainModel() }

fun List<AlbumWithDetails>.toDomainModelsFromRelations(): List<Album> = map { it.toDomainModel() }
