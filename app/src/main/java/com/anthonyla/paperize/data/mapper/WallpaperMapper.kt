package com.anthonyla.paperize.data.mapper

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
    sourceType = sourceType,
    addedAt = addedAt,
    mediaType = mediaType
)

fun Wallpaper.toEntity(): WallpaperEntity = WallpaperEntity(
    id = id,
    albumId = albumId,
    folderId = folderId,
    uri = uri,
    fileName = fileName,
    dateModified = dateModified,
    displayOrder = displayOrder,
    sourceType = sourceType,
    addedAt = addedAt,
    mediaType = mediaType
)
