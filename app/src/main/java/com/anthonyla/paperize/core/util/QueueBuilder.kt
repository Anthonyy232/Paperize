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

    /** Ids before [pickedId] are left out: the ordered rebuild of an empty queue starts at the first item. */
    fun idsAfter(orderedIds: List<String>, pickedId: String): List<String> {
        val index = orderedIds.indexOf(pickedId)
        return if (index < 0) orderedIds else orderedIds.subList(index + 1, orderedIds.size).toList()
    }
}
