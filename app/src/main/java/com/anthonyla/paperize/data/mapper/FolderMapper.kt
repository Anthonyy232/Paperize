package com.anthonyla.paperize.data.mapper

import com.anthonyla.paperize.data.database.entities.FolderEntity
import com.anthonyla.paperize.data.database.relations.FolderWithWallpapers
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper

fun FolderEntity.toDomainModel(
    wallpapers: List<Wallpaper> = emptyList(),
    removedWallpapers: List<Wallpaper> = emptyList()
): Folder = Folder(
    id = id,
    albumId = albumId,
    name = name,
    uri = uri,
    coverUri = coverUri,
    dateModified = dateModified,
    displayOrder = displayOrder,
    wallpapers = wallpapers,
    removedWallpapers = removedWallpapers,
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
    wallpapers = wallpapers.map { it.toDomainModel() },
    removedWallpapers = excluded.map { it.toDomainModel(folder.albumId) }
)
