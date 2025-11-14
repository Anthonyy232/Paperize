package com.anthonyla.paperize.domain.model

import com.anthonyla.paperize.core.WallpaperSourceType

/**
 * Domain model for Wallpaper
 *
 * Pure Kotlin data class without Room annotations
 */
data class Wallpaper(
    val id: String,
    val albumId: String,
    val folderId: String? = null,
    val uri: String,
    val fileName: String,
    val dateModified: Long,
    val displayOrder: Int = 0,
    val sourceType: WallpaperSourceType = WallpaperSourceType.DIRECT,
    val addedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if wallpaper is from a folder
     */
    val isFromFolder: Boolean
        get() = folderId != null && sourceType == WallpaperSourceType.FOLDER

    /**
     * Get file extension
     */
    val extension: String
        get() = fileName.substringAfterLast('.', "").lowercase()

    /**
     * Check if file is a valid image
     */
    val isValidImage: Boolean
        get() = extension in setOf("jpg", "jpeg", "png", "webp", "avif")

    companion object {
        fun empty(id: String = "", albumId: String = "") = Wallpaper(
            id = id,
            albumId = albumId,
            uri = "",
            fileName = "",
            dateModified = 0L
        )
    }
}
