package com.anthonyla.paperize.domain.model

/**
 * Domain model for Album
 *
 * Pure Kotlin data class without Room annotations
 */
data class Album(
    val id: String,
    val name: String,
    val coverUri: String?,
    val wallpapers: List<Wallpaper> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    /**
     * Get total wallpaper count (direct + in folders)
     */
    val totalWallpaperCount: Int
        get() = wallpapers.size + folders.sumOf { it.wallpapers.size }

    /**
     * Get all wallpapers (direct + from folders)
     */
    val allWallpapers: List<Wallpaper>
        get() = wallpapers + folders.flatMap { it.wallpapers }

    /**
     * Get all wallpapers sorted by display order
     */
    val sortedWallpapers: List<Wallpaper>
        get() = allWallpapers.sortedBy { it.displayOrder }

    /**
     * Check if album is empty
     */
    val isEmpty: Boolean
        get() = totalWallpaperCount == 0

    companion object {
        fun empty(id: String = "", name: String = "") = Album(
            id = id,
            name = name,
            coverUri = null
        )
    }
}
