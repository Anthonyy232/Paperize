package com.anthonyla.paperize.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Folder domain model
 */
class FolderTest {

    // ============================================================
    // Test: displayName
    // ============================================================

    // Note: displayName uses Uri.decode() which is an Android API
    // These tests verify behavior in JVM environment where Uri.decode() returns input unchanged
    
    @Test
    fun `displayName returns name for simple names`() {
        val folder = createFolder(name = "Wallpapers")
        // In JVM tests, Uri.decode returns null, so fallback to name is used
        assertEquals("Wallpapers", folder.displayName)
    }

    @Test
    fun `displayName handles empty name gracefully`() {
        val folder = createFolder(name = "")
        assertEquals("", folder.displayName)
    }

    // ============================================================
    // Test: wallpaperCount
    // ============================================================

    @Test
    fun `wallpaperCount returns zero for empty folder`() {
        val folder = Folder.empty()
        assertEquals(0, folder.wallpaperCount)
    }

    @Test
    fun `wallpaperCount returns correct count`() {
        val wallpapers = listOf(
            Wallpaper.empty(id = "w1", albumId = "a1"),
            Wallpaper.empty(id = "w2", albumId = "a1"),
            Wallpaper.empty(id = "w3", albumId = "a1")
        )
        val folder = createFolder().copy(wallpapers = wallpapers)
        assertEquals(3, folder.wallpaperCount)
    }

    // ============================================================
    // Test: isEmpty
    // ============================================================

    @Test
    fun `isEmpty returns true for folder with no wallpapers`() {
        val folder = Folder.empty()
        assertTrue(folder.isEmpty)
    }

    @Test
    fun `isEmpty returns false for folder with wallpapers`() {
        val folder = createFolder().copy(
            wallpapers = listOf(Wallpaper.empty(id = "w1", albumId = "a1"))
        )
        assertFalse(folder.isEmpty)
    }

    // ============================================================
    // Test: sortedWallpapers
    // ============================================================

    @Test
    fun `sortedWallpapers returns wallpapers sorted by displayOrder`() {
        val wallpapers = listOf(
            Wallpaper.empty(id = "w3", albumId = "a1").copy(displayOrder = 3),
            Wallpaper.empty(id = "w1", albumId = "a1").copy(displayOrder = 1),
            Wallpaper.empty(id = "w2", albumId = "a1").copy(displayOrder = 2)
        )
        val folder = createFolder().copy(wallpapers = wallpapers)
        
        val sorted = folder.sortedWallpapers
        assertEquals("w1", sorted[0].id)
        assertEquals("w2", sorted[1].id)
        assertEquals("w3", sorted[2].id)
    }

    @Test
    fun `sortedWallpapers returns empty list for empty folder`() {
        val folder = Folder.empty()
        assertTrue(folder.sortedWallpapers.isEmpty())
    }

    // ============================================================
    // Test: Companion object
    // ============================================================

    @Test
    fun `empty factory creates folder with default values`() {
        val folder = Folder.empty(id = "test-id", albumId = "album-1")
        assertEquals("test-id", folder.id)
        assertEquals("album-1", folder.albumId)
        assertEquals("", folder.name)
        assertEquals("", folder.uri)
        assertNull(folder.coverUri)
        assertEquals(0L, folder.dateModified)
        assertTrue(folder.wallpapers.isEmpty())
    }

    // ============================================================
    // Helper
    // ============================================================

    private fun createFolder(
        name: String = "TestFolder"
    ) = Folder(
        id = "folder-1",
        albumId = "album-1",
        name = name,
        uri = "content://folder/$name",
        coverUri = null,
        dateModified = System.currentTimeMillis(),
        displayOrder = 0
    )
}
