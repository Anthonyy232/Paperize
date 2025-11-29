package com.anthonyla.paperize.domain.model

import android.net.Uri
import com.anthonyla.paperize.core.WallpaperMediaType
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
    val addedAt: Long = System.currentTimeMillis(),
    val mediaType: WallpaperMediaType = WallpaperMediaType.IMAGE
) {
    /**
     * Get URL-decoded file name for display (filename only, no path)
     */
    val displayFileName: String
        get() = try {
            val decoded = Uri.decode(fileName)
            // Extract just the filename (last segment after / or :)
            decoded.substringAfterLast('/', decoded.substringAfterLast(':', decoded))
        } catch (e: Exception) {
            fileName
        }

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
