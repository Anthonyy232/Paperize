package com.anthonyla.paperize.core.util

import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WallpaperSorter utility
 */
class WallpaperSorterTest {

    // ============================================================
    // Test: sortWallpapersByName
    // ============================================================

    @Test
    fun `sortWallpapersByName ascending sorts A to Z`() {
        val wallpapers = listOf(
            createWallpaper("z_photo.jpg"),
            createWallpaper("a_photo.jpg"),
            createWallpaper("m_photo.jpg")
        )
        
        val sorted = WallpaperSorter.sortWallpapersByName(wallpapers, ascending = true)
        
        assertEquals("a_photo.jpg", sorted[0].fileName)
        assertEquals("m_photo.jpg", sorted[1].fileName)
        assertEquals("z_photo.jpg", sorted[2].fileName)
    }

    @Test
    fun `sortWallpapersByName descending sorts Z to A`() {
        val wallpapers = listOf(
            createWallpaper("a_photo.jpg"),
            createWallpaper("m_photo.jpg"),
            createWallpaper("z_photo.jpg")
        )
        
        val sorted = WallpaperSorter.sortWallpapersByName(wallpapers, ascending = false)
        
        assertEquals("z_photo.jpg", sorted[0].fileName)
        assertEquals("m_photo.jpg", sorted[1].fileName)
        assertEquals("a_photo.jpg", sorted[2].fileName)
    }

    @Test
    fun `sortWallpapersByName is case insensitive`() {
        val wallpapers = listOf(
            createWallpaper("Zebra.jpg"),
            createWallpaper("apple.jpg"),
            createWallpaper("BANANA.jpg")
        )
        
        val sorted = WallpaperSorter.sortWallpapersByName(wallpapers, ascending = true)
        
        assertEquals("apple.jpg", sorted[0].fileName)
        assertEquals("BANANA.jpg", sorted[1].fileName)
        assertEquals("Zebra.jpg", sorted[2].fileName)
    }

    @Test
    fun `sortWallpapersByName with empty list returns empty list`() {
        val sorted = WallpaperSorter.sortWallpapersByName(emptyList(), ascending = true)
        assertTrue(sorted.isEmpty())
    }

    @Test
    fun `sortWallpapersByName with single item returns same list`() {
        val wallpapers = listOf(createWallpaper("photo.jpg"))
        val sorted = WallpaperSorter.sortWallpapersByName(wallpapers, ascending = true)
        assertEquals(1, sorted.size)
        assertEquals("photo.jpg", sorted[0].fileName)
    }

    // ============================================================
    // Test: sortWallpapersByDateAdded
    // ============================================================

    @Test
    fun `sortWallpapersByDateAdded ascending sorts oldest first`() {
        val wallpapers = listOf(
            createWallpaper("c.jpg", addedAt = 3000L),
            createWallpaper("a.jpg", addedAt = 1000L),
            createWallpaper("b.jpg", addedAt = 2000L)
        )
        
        val sorted = WallpaperSorter.sortWallpapersByDateAdded(wallpapers, ascending = true)
        
        assertEquals(1000L, sorted[0].addedAt)
        assertEquals(2000L, sorted[1].addedAt)
        assertEquals(3000L, sorted[2].addedAt)
    }

    @Test
    fun `sortWallpapersByDateAdded descending sorts newest first`() {
        val wallpapers = listOf(
            createWallpaper("a.jpg", addedAt = 1000L),
            createWallpaper("c.jpg", addedAt = 3000L),
            createWallpaper("b.jpg", addedAt = 2000L)
        )
        
        val sorted = WallpaperSorter.sortWallpapersByDateAdded(wallpapers, ascending = false)
        
        assertEquals(3000L, sorted[0].addedAt)
        assertEquals(2000L, sorted[1].addedAt)
        assertEquals(1000L, sorted[2].addedAt)
    }

    // ============================================================
    // Test: sortWallpapersByDateModified
    // ============================================================

    @Test
    fun `sortWallpapersByDateModified ascending sorts oldest first`() {
        val wallpapers = listOf(
            createWallpaper("c.jpg", dateModified = 3000L),
            createWallpaper("a.jpg", dateModified = 1000L),
            createWallpaper("b.jpg", dateModified = 2000L)
        )
        
        val sorted = WallpaperSorter.sortWallpapersByDateModified(wallpapers, ascending = true)
        
        assertEquals(1000L, sorted[0].dateModified)
        assertEquals(2000L, sorted[1].dateModified)
        assertEquals(3000L, sorted[2].dateModified)
    }

    @Test
    fun `sortWallpapersByDateModified descending sorts newest first`() {
        val wallpapers = listOf(
            createWallpaper("a.jpg", dateModified = 1000L),
            createWallpaper("c.jpg", dateModified = 3000L),
            createWallpaper("b.jpg", dateModified = 2000L)
        )
        
        val sorted = WallpaperSorter.sortWallpapersByDateModified(wallpapers, ascending = false)
        
        assertEquals(3000L, sorted[0].dateModified)
        assertEquals(2000L, sorted[1].dateModified)
        assertEquals(1000L, sorted[2].dateModified)
    }

    // ============================================================
    // Test: sortFoldersByName
    // ============================================================

    @Test
    fun `sortFoldersByName ascending sorts A to Z`() {
        val folders = listOf(
            createFolder("Wallpapers"),
            createFolder("Animals"),
            createFolder("Nature")
        )
        
        val sorted = WallpaperSorter.sortFoldersByName(folders, ascending = true)
        
        assertEquals("Animals", sorted[0].name)
        assertEquals("Nature", sorted[1].name)
        assertEquals("Wallpapers", sorted[2].name)
    }

    @Test
    fun `sortFoldersByName descending sorts Z to A`() {
        val folders = listOf(
            createFolder("Animals"),
            createFolder("Nature"),
            createFolder("Wallpapers")
        )
        
        val sorted = WallpaperSorter.sortFoldersByName(folders, ascending = false)
        
        assertEquals("Wallpapers", sorted[0].name)
        assertEquals("Nature", sorted[1].name)
        assertEquals("Animals", sorted[2].name)
    }

    // ============================================================
    // Test: sortFoldersByDateModified
    // ============================================================

    @Test
    fun `sortFoldersByDateModified ascending sorts oldest first`() {
        val folders = listOf(
            createFolder("C", dateModified = 3000L),
            createFolder("A", dateModified = 1000L),
            createFolder("B", dateModified = 2000L)
        )
        
        val sorted = WallpaperSorter.sortFoldersByDateModified(folders, ascending = true)
        
        assertEquals(1000L, sorted[0].dateModified)
        assertEquals(2000L, sorted[1].dateModified)
        assertEquals(3000L, sorted[2].dateModified)
    }

    // ============================================================
    // Test: applyDisplayOrder
    // ============================================================

    @Test
    fun `applyDisplayOrder sets indices correctly`() {
        val wallpapers = listOf(
            createWallpaper("a.jpg", displayOrder = 5),
            createWallpaper("b.jpg", displayOrder = 10),
            createWallpaper("c.jpg", displayOrder = 15)
        )
        
        val result = WallpaperSorter.applyDisplayOrder(wallpapers)
        
        assertEquals(0, result[0].displayOrder)
        assertEquals(1, result[1].displayOrder)
        assertEquals(2, result[2].displayOrder)
    }

    @Test
    fun `applyDisplayOrder with empty list returns empty list`() {
        val result = WallpaperSorter.applyDisplayOrder(emptyList())
        assertTrue(result.isEmpty())
    }

    // ============================================================
    // Test: applyFolderDisplayOrder
    // ============================================================

    @Test
    fun `applyFolderDisplayOrder sets indices correctly`() {
        val folders = listOf(
            createFolder("A", displayOrder = 99),
            createFolder("B", displayOrder = 50),
            createFolder("C", displayOrder = 1)
        )
        
        val result = WallpaperSorter.applyFolderDisplayOrder(folders)
        
        assertEquals(0, result[0].displayOrder)
        assertEquals(1, result[1].displayOrder)
        assertEquals(2, result[2].displayOrder)
    }

    // ============================================================
    // Test: Integration - Sort then apply order
    // ============================================================

    @Test
    fun `sort then applyDisplayOrder produces correct order`() {
        val wallpapers = listOf(
            createWallpaper("z.jpg", displayOrder = 0),
            createWallpaper("a.jpg", displayOrder = 1),
            createWallpaper("m.jpg", displayOrder = 2)
        )
        
        val sorted = WallpaperSorter.sortWallpapersByName(wallpapers, ascending = true)
        val withOrder = WallpaperSorter.applyDisplayOrder(sorted)
        
        assertEquals("a.jpg", withOrder[0].fileName)
        assertEquals(0, withOrder[0].displayOrder)
        assertEquals("m.jpg", withOrder[1].fileName)
        assertEquals(1, withOrder[1].displayOrder)
        assertEquals("z.jpg", withOrder[2].fileName)
        assertEquals(2, withOrder[2].displayOrder)
    }

    // ============================================================
    // Test: shiftWallpaper
    // ============================================================

    @Test
    fun `shiftWallpaper moves item and updates indices`() {
        val wallpapers = listOf(
            createWallpaper("a.jpg", displayOrder = 0),
            createWallpaper("b.jpg", displayOrder = 1),
            createWallpaper("c.jpg", displayOrder = 2)
        )
        
        // Move 'a' to the end (index 2)
        val result = WallpaperSorter.shiftWallpaper(wallpapers, "content://test/a.jpg", "content://test/c.jpg")
        
        assertEquals("b.jpg", result[0].fileName)
        assertEquals(0, result[0].displayOrder)
        assertEquals("c.jpg", result[1].fileName)
        assertEquals(1, result[1].displayOrder)
        assertEquals("a.jpg", result[2].fileName)
        assertEquals(2, result[2].displayOrder)
    }

    @Test
    fun `shiftWallpaper handles invalid URIs`() {
        val wallpapers = listOf(createWallpaper("a.jpg"))
        val result = WallpaperSorter.shiftWallpaper(wallpapers, "invalid", "invalid")
        assertEquals(wallpapers, result)
    }

    // ============================================================
    // Test: shiftFolder
    // ============================================================

    @Test
    fun `shiftFolder moves folder and updates indices`() {
        val folders = listOf(
            createFolder("A", displayOrder = 0),
            createFolder("B", displayOrder = 1),
            createFolder("C", displayOrder = 2)
        )
        
        // Move 'C' to the beginning (index 0)
        val result = WallpaperSorter.shiftFolder(folders, "content://folder/C", "content://folder/A")
        
        assertEquals("C", result[0].name)
        assertEquals(0, result[0].displayOrder)
        assertEquals("A", result[1].name)
        assertEquals(1, result[1].displayOrder)
        assertEquals("B", result[2].name)
        assertEquals(2, result[2].displayOrder)
    }

    // ============================================================
    // Test: shiftWallpaperInFolder
    // ============================================================

    @Test
    fun `shiftWallpaperInFolder updates correct folder and cover URI`() {
        val folders = listOf(
            createFolder("F1").copy(
                wallpapers = listOf(
                    createWallpaper("w1"),
                    createWallpaper("w2")
                )
            ),
            createFolder("F2")
        )
        
        val result = WallpaperSorter.shiftWallpaperInFolder(
            folders, 
            "folder-F1", 
            "content://test/w1", 
            "content://test/w2"
        )
        
        val f1 = result.find { it.name == "F1" }!!
        assertEquals("w2", f1.wallpapers[0].fileName)
        assertEquals("w1", f1.wallpapers[1].fileName)
        assertEquals("content://test/w2", f1.coverUri)
    }

    // ============================================================
    // Test: sortAllAlphabetically
    // ============================================================

    @Test
    fun `sortAllAlphabetically sorts everything correctly`() {
        val folders = listOf(
            createFolder("Z"),
            createFolder("A").copy(
                wallpapers = listOf(
                    createWallpaper("w2"),
                    createWallpaper("w1")
                )
            )
        )
        val wallpapers = listOf(
            createWallpaper("z.jpg"),
            createWallpaper("a.jpg")
        )
        
        val (sortedFolders, sortedWallpapers) = WallpaperSorter.sortAllAlphabetically(folders, wallpapers)
        
        assertEquals("A", sortedFolders[0].name)
        assertEquals("w1", sortedFolders[0].wallpapers[0].fileName)
        assertEquals("Z", sortedFolders[1].name)
        assertEquals("a.jpg", sortedWallpapers[0].fileName)
        assertEquals("z.jpg", sortedWallpapers[1].fileName)
    }

    // ============================================================
    // Test: sortAllByDateModified
    // ============================================================

    @Test
    fun `sortAllByDateModified sorts everything correctly`() {
        val folders = listOf(
            createFolder("Old", dateModified = 100L),
            createFolder("New", dateModified = 500L).copy(
                wallpapers = listOf(
                    createWallpaper("wNew", dateModified = 500L),
                    createWallpaper("wOld", dateModified = 100L)
                )
            )
        )
        val wallpapers = listOf(
            createWallpaper("z.jpg", dateModified = 500L),
            createWallpaper("a.jpg", dateModified = 100L)
        )
        
        val (sortedFolders, sortedWallpapers) = WallpaperSorter.sortAllByDateModified(folders, wallpapers, ascending = true)
        
        assertEquals("Old", sortedFolders[0].name)
        assertEquals("New", sortedFolders[1].name)
        assertEquals("wOld", sortedFolders[1].wallpapers[0].fileName)
        assertEquals("a.jpg", sortedWallpapers[0].fileName)
    }

    // ============================================================
    // Helpers
    // ============================================================

    private fun createWallpaper(
        fileName: String,
        displayOrder: Int = 0,
        addedAt: Long = System.currentTimeMillis(),
        dateModified: Long = System.currentTimeMillis()
    ) = Wallpaper(
        id = "id-$fileName",
        albumId = "album-1",
        uri = "content://test/$fileName",
        fileName = fileName,
        displayOrder = displayOrder,
        addedAt = addedAt,
        dateModified = dateModified
    )

    private fun createFolder(
        name: String,
        displayOrder: Int = 0,
        dateModified: Long = System.currentTimeMillis()
    ) = Folder(
        id = "folder-$name",
        albumId = "album-1",
        name = name,
        uri = "content://folder/$name",
        coverUri = null,
        dateModified = dateModified,
        displayOrder = displayOrder
    )
}
