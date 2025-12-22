package com.anthonyla.paperize.domain.model

/**
 * Lightweight summary of an Album for list displays.
 * Prevents loading potentially thousands of Wallpaper objects into memory
 * when just displaying a list of albums.
 */
data class AlbumSummary(
    val id: String,
    val name: String,
    val coverUri: String?,
    val wallpaperCount: Int,
    val folderCount: Int,
    val createdAt: Long,
    val modifiedAt: Long
) {
    /**
     * Check if album is empty without a full object load
     */
    val isEmpty: Boolean
        get() = wallpaperCount == 0

    companion object {
        fun empty(id: String = "", name: String = "") = AlbumSummary(
            id = id,
            name = name,
            coverUri = null,
            wallpaperCount = 0,
            folderCount = 0,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
    }
}
