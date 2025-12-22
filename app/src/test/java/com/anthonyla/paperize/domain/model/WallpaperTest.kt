package com.anthonyla.paperize.domain.model

import com.anthonyla.paperize.core.WallpaperMediaType
import com.anthonyla.paperize.core.WallpaperSourceType
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Wallpaper domain model
 */
class WallpaperTest {

    // ============================================================
    // Test: displayFileName
    // ============================================================

    // Note: displayFileName uses Uri.decode() which is an Android API
    // These tests verify behavior in JVM environment where Uri.decode() returns input unchanged
    
    @Test
    fun `displayFileName returns fileName for simple names`() {
        val wallpaper = createWallpaper(fileName = "image.jpg")
        // In JVM tests, Uri.decode returns null, so fallback to fileName is used
        assertEquals("image.jpg", wallpaper.displayFileName)
    }

    @Test
    fun `displayFileName handles empty fileName gracefully`() {
        val wallpaper = createWallpaper(fileName = "")
        assertEquals("", wallpaper.displayFileName)
    }

    // ============================================================
    // Test: isFromFolder
    // ============================================================

    @Test
    fun `isFromFolder returns true when folderId is set and sourceType is FOLDER`() {
        val wallpaper = createWallpaper(
            folderId = "folder-123",
            sourceType = WallpaperSourceType.FOLDER
        )
        assertTrue(wallpaper.isFromFolder)
    }

    @Test
    fun `isFromFolder returns false when folderId is null`() {
        val wallpaper = createWallpaper(
            folderId = null,
            sourceType = WallpaperSourceType.FOLDER
        )
        assertFalse(wallpaper.isFromFolder)
    }

    @Test
    fun `isFromFolder returns false when sourceType is DIRECT`() {
        val wallpaper = createWallpaper(
            folderId = "folder-123",
            sourceType = WallpaperSourceType.DIRECT
        )
        assertFalse(wallpaper.isFromFolder)
    }

    // ============================================================
    // Test: extension
    // ============================================================

    @Test
    fun `extension extracts jpg extension`() {
        val wallpaper = createWallpaper(fileName = "photo.jpg")
        assertEquals("jpg", wallpaper.extension)
    }

    @Test
    fun `extension extracts JPEG extension as lowercase`() {
        val wallpaper = createWallpaper(fileName = "photo.JPEG")
        assertEquals("jpeg", wallpaper.extension)
    }

    @Test
    fun `extension returns empty string when no extension`() {
        val wallpaper = createWallpaper(fileName = "noextension")
        assertEquals("", wallpaper.extension)
    }

    @Test
    fun `extension handles multiple dots correctly`() {
        val wallpaper = createWallpaper(fileName = "my.photo.file.png")
        assertEquals("png", wallpaper.extension)
    }

    // ============================================================
    // Test: isValidImage
    // ============================================================

    @Test
    fun `isValidImage returns true for jpg`() {
        assertTrue(createWallpaper(fileName = "image.jpg").isValidImage)
    }

    @Test
    fun `isValidImage returns true for jpeg`() {
        assertTrue(createWallpaper(fileName = "image.jpeg").isValidImage)
    }

    @Test
    fun `isValidImage returns true for png`() {
        assertTrue(createWallpaper(fileName = "image.png").isValidImage)
    }

    @Test
    fun `isValidImage returns true for webp`() {
        assertTrue(createWallpaper(fileName = "image.webp").isValidImage)
    }

    @Test
    fun `isValidImage returns true for avif`() {
        assertTrue(createWallpaper(fileName = "image.avif").isValidImage)
    }

    @Test
    fun `isValidImage returns false for gif`() {
        assertFalse(createWallpaper(fileName = "animation.gif").isValidImage)
    }

    @Test
    fun `isValidImage returns false for bmp`() {
        assertFalse(createWallpaper(fileName = "image.bmp").isValidImage)
    }

    @Test
    fun `isValidImage returns false for no extension`() {
        assertFalse(createWallpaper(fileName = "noext").isValidImage)
    }

    @Test
    fun `isValidImage is case insensitive`() {
        assertTrue(createWallpaper(fileName = "image.PNG").isValidImage)
        assertTrue(createWallpaper(fileName = "image.JPG").isValidImage)
    }

    // ============================================================
    // Test: Companion object
    // ============================================================

    @Test
    fun `empty factory creates wallpaper with default values`() {
        val wallpaper = Wallpaper.empty(id = "test-id", albumId = "album-1")
        assertEquals("test-id", wallpaper.id)
        assertEquals("album-1", wallpaper.albumId)
        assertEquals("", wallpaper.uri)
        assertEquals("", wallpaper.fileName)
        assertEquals(0L, wallpaper.dateModified)
        assertNull(wallpaper.folderId)
    }

    // ============================================================
    // Helper
    // ============================================================

    private fun createWallpaper(
        fileName: String = "test.jpg",
        folderId: String? = null,
        sourceType: WallpaperSourceType = WallpaperSourceType.DIRECT,
        mediaType: WallpaperMediaType = WallpaperMediaType.IMAGE
    ) = Wallpaper(
        id = "test-id",
        albumId = "album-1",
        folderId = folderId,
        uri = "content://test/$fileName",
        fileName = fileName,
        dateModified = System.currentTimeMillis(),
        displayOrder = 0,
        sourceType = sourceType,
        mediaType = mediaType
    )
}
