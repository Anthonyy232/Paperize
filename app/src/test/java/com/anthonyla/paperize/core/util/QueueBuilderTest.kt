package com.anthonyla.paperize.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueBuilderTest {
    @Test
    fun `merge keeps surviving order and appends new items once`() {
        val merged = QueueBuilder.mergeWithExistingQueue(
            existingQueueIds = listOf("third", "removed", "first"),
            allWallpaperIds = listOf("first", "second", "third", "fourth")
        )
        assertEquals(listOf("third", "first"), merged.take(2))
        assertEquals(setOf("second", "fourth"), merged.drop(2).toSet())
        assertEquals(4, merged.size)
    }

    private val ordered = listOf("1", "2", "3", "4", "5")

    @Test
    fun `idsAfter continues from the item after the picked one`() {
        assertEquals(listOf("4", "5"), QueueBuilder.idsAfter(ordered, "3"))
    }

    @Test
    fun `idsAfter on the first item queues the rest of the album`() {
        assertEquals(listOf("2", "3", "4", "5"), QueueBuilder.idsAfter(ordered, "1"))
    }

    @Test
    fun `idsAfter on the last item leaves an empty queue so the next rebuild wraps to the start`() {
        assertEquals(emptyList<String>(), QueueBuilder.idsAfter(ordered, "5"))
    }

    @Test
    fun `idsAfter with an unknown id keeps the full order`() {
        assertEquals(ordered, QueueBuilder.idsAfter(ordered, "missing"))
    }

    @Test
    fun `idsAfter on a single item album is empty`() {
        assertEquals(emptyList<String>(), QueueBuilder.idsAfter(listOf("only"), "only"))
    }
}
