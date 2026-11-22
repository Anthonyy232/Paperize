package com.anthonyla.paperize.data.mapper

import com.anthonyla.paperize.data.database.entities.ExcludedWallpaperEntity
import com.anthonyla.paperize.data.database.entities.WallpaperEntity
import com.anthonyla.paperize.domain.model.Wallpaper

fun WallpaperEntity.toDomainModel(): Wallpaper = Wallpaper(
    id = id,
    albumId = albumId,
    folderId = folderId,
    uri = uri,
    fileName = fileName,
    dateModified = dateModified,
    displayOrder = displayOrder,
    addedAt = addedAt
)

fun Wallpaper.toEntity(): WallpaperEntity = WallpaperEntity(
    id = id,
    albumId = albumId,
    folderId = folderId,
    uri = uri,
    fileName = fileName,
    dateModified = dateModified,
    displayOrder = displayOrder,
    sourceType = if (folderId == null) "DIRECT" else "FOLDER",
    addedAt = addedAt
)

fun ExcludedWallpaperEntity.toDomainModel(albumId: String): Wallpaper = Wallpaper(
    id = id,
    albumId = albumId,
    folderId = folderId,
    uri = uri,
    fileName = fileName,
    dateModified = dateModified,
    displayOrder = displayOrder,
    addedAt = addedAt
)

fun Wallpaper.toExcludedEntity(folderId: String): ExcludedWallpaperEntity = ExcludedWallpaperEntity(
    folderId = folderId,
    uri = uri,
    id = id,
    fileName = fileName,
    dateModified = dateModified,
    displayOrder = displayOrder,
    addedAt = addedAt
)
