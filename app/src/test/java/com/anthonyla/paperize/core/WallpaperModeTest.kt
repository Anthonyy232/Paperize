package com.anthonyla.paperize.core

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WallpaperMode enum
 */
class WallpaperModeTest {

    // ============================================================
    // Test: fromString
    // ============================================================

    @Test
    fun `fromString with STATIC returns STATIC`() {
        assertEquals(WallpaperMode.STATIC, WallpaperMode.fromString("STATIC"))
    }

    @Test
    fun `fromString with LIVE returns LIVE`() {
        assertEquals(WallpaperMode.LIVE, WallpaperMode.fromString("LIVE"))
    }

    @Test
    fun `fromString is case insensitive for static`() {
        assertEquals(WallpaperMode.STATIC, WallpaperMode.fromString("static"))
        assertEquals(WallpaperMode.STATIC, WallpaperMode.fromString("Static"))
        assertEquals(WallpaperMode.STATIC, WallpaperMode.fromString("STATIC"))
    }

    @Test
    fun `fromString is case insensitive for live`() {
        assertEquals(WallpaperMode.LIVE, WallpaperMode.fromString("live"))
        assertEquals(WallpaperMode.LIVE, WallpaperMode.fromString("Live"))
        assertEquals(WallpaperMode.LIVE, WallpaperMode.fromString("LIVE"))
    }

    @Test
    fun `fromString with null returns STATIC as default`() {
        assertEquals(WallpaperMode.STATIC, WallpaperMode.fromString(null))
    }

    @Test
    fun `fromString with empty string returns STATIC as default`() {
        assertEquals(WallpaperMode.STATIC, WallpaperMode.fromString(""))
    }

    @Test
    fun `fromString with invalid value returns STATIC as default`() {
        assertEquals(WallpaperMode.STATIC, WallpaperMode.fromString("invalid"))
        assertEquals(WallpaperMode.STATIC, WallpaperMode.fromString("dynamic"))
        assertEquals(WallpaperMode.STATIC, WallpaperMode.fromString("123"))
    }

    @Test
    fun `fromString with whitespace returns STATIC as default`() {
        assertEquals(WallpaperMode.STATIC, WallpaperMode.fromString(" "))
        assertEquals(WallpaperMode.STATIC, WallpaperMode.fromString("  STATIC  "))
    }

    // ============================================================
    // Test: Enum values
    // ============================================================

    @Test
    fun `enum has exactly two values`() {
        assertEquals(2, WallpaperMode.entries.size)
    }

    @Test
    fun `enum values are STATIC and LIVE`() {
        assertTrue(WallpaperMode.entries.contains(WallpaperMode.STATIC))
        assertTrue(WallpaperMode.entries.contains(WallpaperMode.LIVE))
    }

    @Test
    fun `STATIC name is correct`() {
        assertEquals("STATIC", WallpaperMode.STATIC.name)
    }

    @Test
    fun `LIVE name is correct`() {
        assertEquals("LIVE", WallpaperMode.LIVE.name)
    }
}
