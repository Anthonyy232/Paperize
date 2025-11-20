package com.anthonyla.paperize.domain.model

import android.net.Uri

/**
 * Domain model for Folder
 *
 * Pure Kotlin data class without Room annotations
 */
data class Folder(
    val id: String,
    val albumId: String,
    val name: String,
    val uri: String,
    val coverUri: String?,
    val dateModified: Long,
    val displayOrder: Int = 0,
    val wallpapers: List<Wallpaper> = emptyList(),
    val addedAt: Long = System.currentTimeMillis()
) {
    /**
     * Get URL-decoded name for display (last path segment only)
     */
    val displayName: String
        get() = try {
            val decoded = Uri.decode(name)
            // Extract just the folder name (last segment after / or :)
            decoded.substringAfterLast('/', decoded.substringAfterLast(':', decoded))
        } catch (e: Exception) {
            name
        }

    /**
     * Get wallpaper count
     */
    val wallpaperCount: Int
        get() = wallpapers.size

    /**
     * Check if folder is empty
     */
    val isEmpty: Boolean
        get() = wallpapers.isEmpty()

    /**
     * Get sorted wallpapers
     */
    val sortedWallpapers: List<Wallpaper>
        get() = wallpapers.sortedBy { it.displayOrder }

    companion object {
        fun empty(id: String = "", albumId: String = "") = Folder(
            id = id,
            albumId = albumId,
            name = "",
            uri = "",
            coverUri = null,
            dateModified = 0L
        )
    }
}
