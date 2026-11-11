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

    /**
     * The ordered ids that follow [pickedId], used to continue a sequential rotation from a
     * user-picked wallpaper. Ids before it are left out on purpose: when this queue runs out,
     * the normal ordered rebuild starts again at the first item, which continues the cycle
     * (picking 3 of 1..5 gives 4, 5, then 1, 2, 3, ...). If [pickedId] is unknown, every id is
     * returned unchanged.
     */
    fun idsAfter(orderedIds: List<String>, pickedId: String): List<String> {
        val index = orderedIds.indexOf(pickedId)
        return if (index < 0) orderedIds else orderedIds.subList(index + 1, orderedIds.size).toList()
    }
}
