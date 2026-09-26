package com.anthonyla.paperize.core.util

import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import org.junit.Assert.*
import org.junit.Test

class WallpaperSorterTest {

    @Test
    fun `shiftWallpaper moves item and updates indices`() {
        val wallpapers = listOf(
            createWallpaper("a.jpg", displayOrder = 0),
            createWallpaper("b.jpg", displayOrder = 1),
            createWallpaper("c.jpg", displayOrder = 2)
        )
        
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

    @Test
    fun `shiftFolder moves folder and updates indices`() {
        val folders = listOf(
            createFolder("A", displayOrder = 0),
            createFolder("B", displayOrder = 1),
            createFolder("C", displayOrder = 2)
        )
        
        val result = WallpaperSorter.shiftFolder(folders, "content://folder/C", "content://folder/A")
        
        assertEquals("C", result[0].name)
        assertEquals(0, result[0].displayOrder)
        assertEquals("A", result[1].name)
        assertEquals(1, result[1].displayOrder)
        assertEquals("B", result[2].name)
        assertEquals(2, result[2].displayOrder)
    }

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
        assertEquals(listOf(0, 1), sortedFolders.map { it.displayOrder })
        assertEquals("content://test/w1", sortedFolders[0].coverUri)
        val (reversedFolders, reversedImages) = WallpaperSorter.sortAllAlphabetically(folders, wallpapers, ascending = false)
        assertEquals(listOf("Z", "A"), reversedFolders.map { it.name })
        assertEquals(listOf("z.jpg", "a.jpg"), reversedImages.map { it.fileName })
        assertEquals("content://test/w2", reversedFolders[1].coverUri)
    }

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
