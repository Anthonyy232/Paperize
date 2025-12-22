package com.anthonyla.paperize.core.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for QueueBuilder utility
 */
class QueueBuilderTest {

    // ============================================================
    // Test: buildSequentialQueue
    // ============================================================

    @Test
    fun `buildSequentialQueue sorts by display order`() {
        val wallpapers = listOf(
            "w3" to 2,
            "w1" to 0,
            "w2" to 1
        )
        
        val queue = QueueBuilder.buildSequentialQueue(wallpapers)
        
        assertEquals(listOf("w1", "w2", "w3"), queue)
    }

    @Test
    fun `buildSequentialQueue handles empty list`() {
        val queue = QueueBuilder.buildSequentialQueue(emptyList())
        assertTrue(queue.isEmpty())
    }

    @Test
    fun `buildSequentialQueue handles single item`() {
        val queue = QueueBuilder.buildSequentialQueue(listOf("w1" to 5))
        assertEquals(listOf("w1"), queue)
    }

    @Test
    fun `buildSequentialQueue handles non-contiguous display orders`() {
        val wallpapers = listOf(
            "w3" to 100,
            "w1" to 5,
            "w2" to 50
        )
        
        val queue = QueueBuilder.buildSequentialQueue(wallpapers)
        
        assertEquals(listOf("w1", "w2", "w3"), queue)
    }

    // ============================================================
    // Test: buildShuffledQueue
    // ============================================================

    @Test
    fun `buildShuffledQueue contains all original IDs`() {
        val ids = listOf("w1", "w2", "w3", "w4", "w5")
        
        val shuffled = QueueBuilder.buildShuffledQueue(ids)
        
        assertEquals(ids.size, shuffled.size)
        assertTrue(shuffled.containsAll(ids))
    }

    @Test
    fun `buildShuffledQueue handles empty list`() {
        val shuffled = QueueBuilder.buildShuffledQueue(emptyList())
        assertTrue(shuffled.isEmpty())
    }

    @Test
    fun `buildShuffledQueue handles single item`() {
        val shuffled = QueueBuilder.buildShuffledQueue(listOf("w1"))
        assertEquals(listOf("w1"), shuffled)
    }

    @Test
    fun `buildShuffledQueue produces different order over multiple calls`() {
        val ids = (1..100).map { "w$it" }
        
        // Run multiple times and check if we ever get a different order
        // With 100 items, the chance of same order is astronomically low
        val results = (1..10).map { QueueBuilder.buildShuffledQueue(ids) }
        val allSame = results.all { it == results.first() }
        
        assertFalse("Shuffle should produce different orders", allSame)
    }

    // ============================================================
    // Test: needsNormalization
    // ============================================================

    @Test
    fun `needsNormalization returns false for empty list`() {
        assertFalse(QueueBuilder.needsNormalization(emptyList()))
    }

    @Test
    fun `needsNormalization returns false for sequential positions`() {
        assertFalse(QueueBuilder.needsNormalization(listOf(0, 1, 2, 3)))
    }

    @Test
    fun `needsNormalization returns true for positions with gaps`() {
        assertTrue(QueueBuilder.needsNormalization(listOf(0, 2, 4)))
    }

    @Test
    fun `needsNormalization returns true for positions not starting at 0`() {
        assertTrue(QueueBuilder.needsNormalization(listOf(1, 2, 3)))
    }

    @Test
    fun `needsNormalization returns false for out of order but contiguous positions`() {
        // [2, 0, 1] when sorted becomes [0, 1, 2] - no gaps, so no normalization needed
        assertFalse(QueueBuilder.needsNormalization(listOf(2, 0, 1)))
    }

    @Test
    fun `needsNormalization returns false for single item at 0`() {
        assertFalse(QueueBuilder.needsNormalization(listOf(0)))
    }

    @Test
    fun `needsNormalization returns true for single item not at 0`() {
        assertTrue(QueueBuilder.needsNormalization(listOf(5)))
    }

    // ============================================================
    // Test: normalizePositions
    // ============================================================

    @Test
    fun `normalizePositions creates sequential indices`() {
        val normalized = QueueBuilder.normalizePositions(listOf(0, 2, 5))
        assertEquals(listOf(0, 1, 2), normalized)
    }

    @Test
    fun `normalizePositions handles empty list`() {
        val normalized = QueueBuilder.normalizePositions(emptyList())
        assertTrue(normalized.isEmpty())
    }

    @Test
    fun `normalizePositions handles already normalized`() {
        val normalized = QueueBuilder.normalizePositions(listOf(0, 1, 2))
        assertEquals(listOf(0, 1, 2), normalized)
    }

    // ============================================================
    // Test: mergeWithExistingQueueDeterministic
    // ============================================================

    @Test
    fun `mergeWithExistingQueue preserves existing order`() {
        val existing = listOf("w3", "w1", "w2")
        val all = listOf("w1", "w2", "w3")
        
        val merged = QueueBuilder.mergeWithExistingQueueDeterministic(existing, all)
        
        assertEquals(listOf("w3", "w1", "w2"), merged)
    }

    @Test
    fun `mergeWithExistingQueue filters removed wallpapers`() {
        val existing = listOf("w1", "w2", "w3")
        val all = listOf("w1", "w3") // w2 was removed
        
        val merged = QueueBuilder.mergeWithExistingQueueDeterministic(existing, all)
        
        assertEquals(listOf("w1", "w3"), merged)
    }

    @Test
    fun `mergeWithExistingQueue appends new wallpapers`() {
        val existing = listOf("w1", "w2")
        val all = listOf("w1", "w2", "w3", "w4") // w3, w4 are new
        
        val merged = QueueBuilder.mergeWithExistingQueueDeterministic(existing, all)
        
        assertEquals(listOf("w1", "w2", "w3", "w4"), merged)
    }

    @Test
    fun `mergeWithExistingQueue handles empty existing queue`() {
        val existing = emptyList<String>()
        val all = listOf("w1", "w2", "w3")
        
        val merged = QueueBuilder.mergeWithExistingQueueDeterministic(existing, all)
        
        assertEquals(all, merged)
    }

    @Test
    fun `mergeWithExistingQueue handles empty all list`() {
        val existing = listOf("w1", "w2")
        val all = emptyList<String>()
        
        val merged = QueueBuilder.mergeWithExistingQueueDeterministic(existing, all)
        
        assertTrue(merged.isEmpty())
    }

    @Test
    fun `mergeWithExistingQueue complex scenario`() {
        // Existing queue order: w3, w1, w5 (w2 was played)
        // Current album: w1, w3, w4, w5, w6 (w2 removed, w4 and w6 added)
        val existing = listOf("w3", "w1", "w5")
        val all = listOf("w1", "w3", "w4", "w5", "w6")
        
        val merged = QueueBuilder.mergeWithExistingQueueDeterministic(existing, all)
        
        // Should preserve w3, w1, w5 order, then add w4, w6
        assertEquals(listOf("w3", "w1", "w5", "w4", "w6"), merged)
    }

    // ============================================================
    // Test: buildQueue
    // ============================================================

    @Test
    fun `buildQueue sequential uses display order`() {
        val wallpapers = listOf(
            createWallpaper("w2", displayOrder = 1),
            createWallpaper("w1", displayOrder = 0)
        )
        
        val result = QueueBuilder.buildQueue(wallpapers, shuffle = false)
        
        assertEquals(listOf("w1", "w2"), result)
    }

    @Test
    fun `buildQueue shuffle without other screen produces new shuffle`() {
        val wallpapers = (1..50).map { createWallpaper("w$it") }
        
        val result = QueueBuilder.buildQueue(wallpapers, shuffle = true)
        
        assertEquals(50, result.size)
        assertTrue(result.containsAll(wallpapers.map { it.id }))
    }

    @Test
    fun `buildQueue shuffle with other screen synchronizes order`() {
        val wallpapers = (1..5).map { createWallpaper("w$it") }
        val otherQueue = listOf("w3", "w1", "w2", "w4", "w5")
        
        val result = QueueBuilder.buildQueue(wallpapers, shuffle = true, otherScreenQueue = otherQueue)
        
        assertEquals(otherQueue, result)
    }

    // ============================================================
    // Helpers
    // ============================================================

    private fun createWallpaper(id: String, displayOrder: Int = 0) = com.anthonyla.paperize.domain.model.Wallpaper(
        id = id,
        albumId = "album-1",
        uri = "content://test/$id",
        fileName = "$id.jpg",
        displayOrder = displayOrder,
        dateModified = System.currentTimeMillis()
    )
}
