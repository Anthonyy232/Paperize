package com.anthonyla.paperize.core

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WallpaperMediaType enum
 */
class WallpaperMediaTypeTest {

    // ============================================================
    // Test: fromExtension - JPEG formats
    // ============================================================

    @Test
    fun `fromExtension with jpg returns IMAGE`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("jpg"))
    }

    @Test
    fun `fromExtension with jpeg returns IMAGE`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("jpeg"))
    }

    // ============================================================
    // Test: fromExtension - Modern formats
    // ============================================================

    @Test
    fun `fromExtension with png returns IMAGE`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("png"))
    }

    @Test
    fun `fromExtension with webp returns IMAGE`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("webp"))
    }

    @Test
    fun `fromExtension with avif returns IMAGE`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("avif"))
    }

    // ============================================================
    // Test: fromExtension - Apple formats
    // ============================================================

    @Test
    fun `fromExtension with heic returns IMAGE`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("heic"))
    }

    @Test
    fun `fromExtension with heif returns IMAGE`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("heif"))
    }

    // ============================================================
    // Test: fromExtension - Legacy formats
    // ============================================================

    @Test
    fun `fromExtension with bmp returns IMAGE`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("bmp"))
    }

    @Test
    fun `fromExtension with gif returns IMAGE`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("gif"))
    }

    @Test
    fun `fromExtension with tiff returns IMAGE`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("tiff"))
    }

    @Test
    fun `fromExtension with tif returns IMAGE`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("tif"))
    }

    // ============================================================
    // Test: fromExtension - Vector format
    // ============================================================

    @Test
    fun `fromExtension with svg returns IMAGE`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("svg"))
    }

    // ============================================================
    // Test: fromExtension - Case insensitivity
    // ============================================================

    @Test
    fun `fromExtension is case insensitive for uppercase`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("JPG"))
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("PNG"))
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("WEBP"))
    }

    @Test
    fun `fromExtension is case insensitive for mixed case`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("Jpg"))
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("PnG"))
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromExtension("WebP"))
    }

    // ============================================================
    // Test: fromExtension - Unsupported/Invalid extensions
    // ============================================================

    @Test
    fun `fromExtension with unsupported extension returns null`() {
        assertNull(WallpaperMediaType.fromExtension("mp4"))
        assertNull(WallpaperMediaType.fromExtension("mp3"))
        assertNull(WallpaperMediaType.fromExtension("pdf"))
        assertNull(WallpaperMediaType.fromExtension("txt"))
        assertNull(WallpaperMediaType.fromExtension("doc"))
    }

    @Test
    fun `fromExtension with empty string returns null`() {
        assertNull(WallpaperMediaType.fromExtension(""))
    }

    @Test
    fun `fromExtension with whitespace returns null`() {
        assertNull(WallpaperMediaType.fromExtension(" "))
        assertNull(WallpaperMediaType.fromExtension("  "))
    }

    // ============================================================
    // Test: fromString - Valid values
    // ============================================================

    @Test
    fun `fromString with IMAGE returns IMAGE`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromString("IMAGE"))
    }

    @Test
    fun `fromString is case insensitive`() {
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromString("image"))
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromString("Image"))
        assertEquals(WallpaperMediaType.IMAGE, WallpaperMediaType.fromString("iMaGe"))
    }

    // ============================================================
    // Test: fromString - Invalid/null values
    // ============================================================

    @Test
    fun `fromString with null returns null`() {
        assertNull(WallpaperMediaType.fromString(null))
    }

    @Test
    fun `fromString with empty string returns null`() {
        assertNull(WallpaperMediaType.fromString(""))
    }

    @Test
    fun `fromString with invalid value returns null`() {
        assertNull(WallpaperMediaType.fromString("invalid"))
        assertNull(WallpaperMediaType.fromString("video"))
        assertNull(WallpaperMediaType.fromString("IMAGES"))
    }

    // ============================================================
    // Test: supportedExtensions property
    // ============================================================

    @Test
    fun `IMAGE supportedExtensions contains all expected formats`() {
        val extensions = WallpaperMediaType.IMAGE.supportedExtensions
        
        // JPEG formats
        assertTrue(extensions.contains("jpg"))
        assertTrue(extensions.contains("jpeg"))
        
        // Modern formats
        assertTrue(extensions.contains("png"))
        assertTrue(extensions.contains("webp"))
        assertTrue(extensions.contains("avif"))
        
        // Apple formats
        assertTrue(extensions.contains("heic"))
        assertTrue(extensions.contains("heif"))
        
        // Legacy formats
        assertTrue(extensions.contains("bmp"))
        assertTrue(extensions.contains("gif"))
        assertTrue(extensions.contains("tiff"))
        assertTrue(extensions.contains("tif"))
        
        // Vector
        assertTrue(extensions.contains("svg"))
    }

    @Test
    fun `IMAGE supportedExtensions has expected count`() {
        // jpg, jpeg, png, webp, avif, heic, heif, bmp, gif, tiff, tif, svg = 12
        assertEquals(12, WallpaperMediaType.IMAGE.supportedExtensions.size)
    }

    // ============================================================
    // Test: supportedInStaticMode property
    // ============================================================

    @Test
    fun `IMAGE is supported in static mode`() {
        assertTrue(WallpaperMediaType.IMAGE.supportedInStaticMode)
    }

    // ============================================================
    // Test: supportedInLiveMode property
    // ============================================================

    @Test
    fun `IMAGE is supported in live mode`() {
        assertTrue(WallpaperMediaType.IMAGE.supportedInLiveMode)
    }

    // ============================================================
    // Test: Enum values
    // ============================================================

    @Test
    fun `enum has exactly one value`() {
        assertEquals(1, WallpaperMediaType.entries.size)
    }

    @Test
    fun `enum value name is IMAGE`() {
        assertEquals("IMAGE", WallpaperMediaType.IMAGE.name)
    }
}
