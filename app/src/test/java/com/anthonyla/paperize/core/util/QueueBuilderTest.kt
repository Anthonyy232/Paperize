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
}
