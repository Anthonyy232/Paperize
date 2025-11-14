package com.anthonyla.paperize.data.mapper

import com.anthonyla.paperize.data.database.entities.FolderEntity
import com.anthonyla.paperize.data.database.relations.FolderWithWallpapers
import com.anthonyla.paperize.domain.model.Folder

/**
 * Mappers for Folder entity <-> domain model conversion
 */

fun FolderEntity.toDomainModel(
    wallpapers: List<com.anthonyla.paperize.domain.model.Wallpaper> = emptyList()
): Folder = Folder(
    id = id,
    albumId = albumId,
    name = name,
    uri = uri,
    coverUri = coverUri,
    dateModified = dateModified,
    displayOrder = displayOrder,
    wallpapers = wallpapers,
    addedAt = addedAt
)

fun Folder.toEntity(): FolderEntity = FolderEntity(
    id = id,
    albumId = albumId,
    name = name,
    uri = uri,
    coverUri = coverUri,
    dateModified = dateModified,
    displayOrder = displayOrder,
    addedAt = addedAt
)

fun FolderWithWallpapers.toDomainModel(): Folder = folder.toDomainModel(
    wallpapers = wallpapers.map { it.toDomainModel() }
)

fun List<FolderEntity>.toDomainModels(): List<Folder> = map { it.toDomainModel() }

fun List<FolderWithWallpapers>.toDomainModelsFromRelations(): List<Folder> = map { it.toDomainModel() }
