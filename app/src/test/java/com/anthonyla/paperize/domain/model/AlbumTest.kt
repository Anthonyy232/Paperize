package com.anthonyla.paperize.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Album domain model
 */
class AlbumTest {

    // ============================================================
    // Test: totalWallpaperCount
    // ============================================================

    @Test
    fun `totalWallpaperCount with empty album returns zero`() {
        val album = Album.empty(id = "1", name = "Test")
        assertEquals(0, album.totalWallpaperCount)
    }

    @Test
    fun `totalWallpaperCount with direct wallpapers only`() {
        val wallpapers = listOf(
            Wallpaper.empty(id = "w1", albumId = "1"),
            Wallpaper.empty(id = "w2", albumId = "1")
        )
        val album = Album(
            id = "1",
            name = "Test",
            coverUri = null,
            wallpapers = wallpapers
        )
        assertEquals(2, album.totalWallpaperCount)
    }

    @Test
    fun `totalWallpaperCount with folders only`() {
        val folderWallpapers = listOf(
            Wallpaper.empty(id = "w1", albumId = "1"),
            Wallpaper.empty(id = "w2", albumId = "1"),
            Wallpaper.empty(id = "w3", albumId = "1")
        )
        val folders = listOf(
            Folder.empty(id = "f1", albumId = "1").copy(wallpapers = folderWallpapers)
        )
        val album = Album(
            id = "1",
            name = "Test",
            coverUri = null,
            folders = folders
        )
        assertEquals(3, album.totalWallpaperCount)
    }

    @Test
    fun `totalWallpaperCount with mixed direct and folder wallpapers`() {
        val directWallpapers = listOf(
            Wallpaper.empty(id = "w1", albumId = "1")
        )
        val folderWallpapers = listOf(
            Wallpaper.empty(id = "w2", albumId = "1"),
            Wallpaper.empty(id = "w3", albumId = "1")
        )
        val folders = listOf(
            Folder.empty(id = "f1", albumId = "1").copy(wallpapers = folderWallpapers)
        )
        val album = Album(
            id = "1",
            name = "Test",
            coverUri = null,
            wallpapers = directWallpapers,
            folders = folders
        )
        assertEquals(3, album.totalWallpaperCount)
    }

    // ============================================================
    // Test: allWallpapers
    // ============================================================

    @Test
    fun `allWallpapers returns combined direct and folder wallpapers`() {
        val directWallpaper = Wallpaper.empty(id = "direct", albumId = "1")
        val folderWallpaper = Wallpaper.empty(id = "folder", albumId = "1")
        val folders = listOf(
            Folder.empty(id = "f1", albumId = "1").copy(wallpapers = listOf(folderWallpaper))
        )
        val album = Album(
            id = "1",
            name = "Test",
            coverUri = null,
            wallpapers = listOf(directWallpaper),
            folders = folders
        )
        
        val allWallpapers = album.allWallpapers
        assertEquals(2, allWallpapers.size)
        assertTrue(allWallpapers.any { it.id == "direct" })
        assertTrue(allWallpapers.any { it.id == "folder" })
    }

    @Test
    fun `allWallpapers with empty album returns empty list`() {
        val album = Album.empty()
        assertTrue(album.allWallpapers.isEmpty())
    }

    // ============================================================
    // Test: sortedWallpapers
    // ============================================================

    @Test
    fun `sortedWallpapers returns wallpapers sorted by displayOrder`() {
        val wallpapers = listOf(
            Wallpaper.empty(id = "w3", albumId = "1").copy(displayOrder = 3),
            Wallpaper.empty(id = "w1", albumId = "1").copy(displayOrder = 1),
            Wallpaper.empty(id = "w2", albumId = "1").copy(displayOrder = 2)
        )
        val album = Album(
            id = "1",
            name = "Test",
            coverUri = null,
            wallpapers = wallpapers
        )
        
        val sorted = album.sortedWallpapers
        assertEquals("w1", sorted[0].id)
        assertEquals("w2", sorted[1].id)
        assertEquals("w3", sorted[2].id)
    }

    // ============================================================
    // Test: isEmpty
    // ============================================================

    @Test
    fun `isEmpty returns true for empty album`() {
        val album = Album.empty()
        assertTrue(album.isEmpty)
    }

    @Test
    fun `isEmpty returns false when album has wallpapers`() {
        val album = Album(
            id = "1",
            name = "Test",
            coverUri = null,
            wallpapers = listOf(Wallpaper.empty(id = "w1", albumId = "1"))
        )
        assertFalse(album.isEmpty)
    }

    @Test
    fun `isEmpty returns false when folder has wallpapers`() {
        val folderWithWallpapers = Folder.empty(id = "f1", albumId = "1").copy(
            wallpapers = listOf(Wallpaper.empty(id = "w1", albumId = "1"))
        )
        val album = Album(
            id = "1",
            name = "Test",
            coverUri = null,
            folders = listOf(folderWithWallpapers)
        )
        assertFalse(album.isEmpty)
    }

    // ============================================================
    // Test: Companion object
    // ============================================================

    @Test
    fun `empty factory creates album with empty collections`() {
        val album = Album.empty(id = "test-id", name = "Test Album")
        assertEquals("test-id", album.id)
        assertEquals("Test Album", album.name)
        assertNull(album.coverUri)
        assertTrue(album.wallpapers.isEmpty())
        assertTrue(album.folders.isEmpty())
    }
}
