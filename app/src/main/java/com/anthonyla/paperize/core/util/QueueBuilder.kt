package com.anthonyla.paperize.core.util

object QueueBuilder {
    fun mergeWithExistingQueue(
        existingQueueIds: List<String>,
        allWallpaperIds: List<String>
    ): List<String> {
        val allSet = allWallpaperIds.toHashSet()
        val existingSet = existingQueueIds.toHashSet()
        val existingValid = existingQueueIds.filter { it in allSet }
        val newWallpaperIds = allWallpaperIds.filter { it !in existingSet }
        return existingValid + newWallpaperIds.shuffled()
    }
}
