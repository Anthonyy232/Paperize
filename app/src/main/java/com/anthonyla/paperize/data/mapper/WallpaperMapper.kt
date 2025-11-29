package com.anthonyla.paperize.data.mapper

import com.anthonyla.paperize.data.database.entities.WallpaperEntity
import com.anthonyla.paperize.domain.model.Wallpaper

/**
 * Mappers for Wallpaper entity <-> domain model conversion
 */

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

fun List<WallpaperEntity>.toDomainModels(): List<Wallpaper> = map { it.toDomainModel() }

fun List<Wallpaper>.toEntities(): List<WallpaperEntity> = map { it.toEntity() }
