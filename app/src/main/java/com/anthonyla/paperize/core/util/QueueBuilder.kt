package com.anthonyla.paperize.core.util

/**
 * Utility object for building and managing wallpaper queues
 * Extracted from WallpaperRepositoryImpl for testability
 * 
 * Note: The DAO-specific operations remain in the repository,
 * but the pure logic functions are extracted here for testing.
 */
object QueueBuilder {

    /**
     * Build a queue of wallpaper IDs based on display order (sequential mode)
     * @param wallpaperIds List of wallpaper IDs with their display orders
     * @return List of IDs sorted by display order
     */
    fun buildSequentialQueue(wallpaperIds: List<Pair<String, Int>>): List<String> {
        return wallpaperIds.sortedBy { it.second }.map { it.first }
    }

    /**
     * Build a shuffled queue of wallpaper IDs
     * @param wallpaperIds List of wallpaper IDs to shuffle
     * @return Shuffled list of IDs
     */
    fun buildShuffledQueue(wallpaperIds: List<String>): List<String> {
        return wallpaperIds.shuffled()
    }

    /**
     * Check if queue positions need normalization
     * Positions need normalization if they have gaps (e.g., 0, 2, 4 instead of 0, 1, 2)
     * @param positions Current queue positions
     * @return True if normalization is needed, false otherwise
     */
    fun needsNormalization(positions: List<Int>): Boolean {
        if (positions.isEmpty()) return false
        val sorted = positions.sorted()
        return sorted.indices.any { sorted[it] != it }
    }

    /**
     * Normalize queue positions to be sequential starting from 0
     * @param positions Current queue positions (may have gaps)
     * @return Normalized positions (0, 1, 2, ...)
     */
    fun normalizePositions(positions: List<Int>): List<Int> {
        return positions.indices.toList()
    }

    /**
     * Merge existing queue with new wallpapers while preserving existing order
     * Used when new wallpapers are added to an album during shuffle mode
     * @param existingQueueIds Current queue order (filtered to valid IDs)
     * @param allWallpaperIds All current wallpaper IDs in the album
     * @return Combined list with existing order preserved, new IDs shuffled at end
     */
    fun mergeWithExistingQueue(
        existingQueueIds: List<String>,
        allWallpaperIds: List<String>
    ): List<String> {
        // Filter existing queue to only include IDs that still exist
        val existingValid = existingQueueIds.filter { it in allWallpaperIds }
        
        // Find new wallpapers not in existing queue
        val newWallpaperIds = allWallpaperIds.filter { it !in existingQueueIds }
        
        // Return existing order with new wallpapers shuffled at the end
        return existingValid + newWallpaperIds.shuffled()
    }

    /**
     * Deterministic version of mergeWithExistingQueue for testing
     * New wallpapers are appended in their original order instead of shuffled
     */
    fun mergeWithExistingQueueDeterministic(
        existingQueueIds: List<String>,
        allWallpaperIds: List<String>
    ): List<String> {
        val existingValid = existingQueueIds.filter { it in allWallpaperIds }
        val newWallpaperIds = allWallpaperIds.filter { it !in existingQueueIds }
        return existingValid + newWallpaperIds
    }

    /**
     * Build a wallpaper queue based on shuffle and existing queues
     * @param wallpapers Current list of wallpapers in the album
     * @param shuffle Whether shuffle is enabled
     * @param otherScreenQueue Optional existing queue for another screen type to synchronize with
     * @return List of wallpaper IDs for the new queue
     */
    fun buildQueue(
        wallpapers: List<com.anthonyla.paperize.domain.model.Wallpaper>,
        shuffle: Boolean,
        otherScreenQueue: List<String>? = null
    ): List<String> {
        return if (shuffle) {
            if (otherScreenQueue != null && otherScreenQueue.isNotEmpty()) {
                // Synchronize with existing queue from another screen
                mergeWithExistingQueue(otherScreenQueue, wallpapers.map { it.id })
            } else {
                // New shuffled order
                buildShuffledQueue(wallpapers.map { it.id })
            }
        } else {
            // Sequential order by displayOrder
            buildSequentialQueue(wallpapers.map { it.id to it.displayOrder })
        }
    }
}
