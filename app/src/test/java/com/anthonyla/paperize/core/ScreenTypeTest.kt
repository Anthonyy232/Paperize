package com.anthonyla.paperize.core

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ScreenType enum
 */
class ScreenTypeTest {

    // ============================================================
    // Test: fromString
    // ============================================================

    @Test
    fun `fromString with HOME returns HOME`() {
        assertEquals(ScreenType.HOME, ScreenType.fromString("HOME"))
    }

    @Test
    fun `fromString with LOCK returns LOCK`() {
        assertEquals(ScreenType.LOCK, ScreenType.fromString("LOCK"))
    }

    @Test
    fun `fromString with BOTH returns BOTH`() {
        assertEquals(ScreenType.BOTH, ScreenType.fromString("BOTH"))
    }

    @Test
    fun `fromString with LIVE returns LIVE`() {
        assertEquals(ScreenType.LIVE, ScreenType.fromString("LIVE"))
    }

    @Test
    fun `fromString is case insensitive for home`() {
        assertEquals(ScreenType.HOME, ScreenType.fromString("home"))
        assertEquals(ScreenType.HOME, ScreenType.fromString("Home"))
        assertEquals(ScreenType.HOME, ScreenType.fromString("HOME"))
    }

    @Test
    fun `fromString is case insensitive for lock`() {
        assertEquals(ScreenType.LOCK, ScreenType.fromString("lock"))
        assertEquals(ScreenType.LOCK, ScreenType.fromString("Lock"))
    }

    @Test
    fun `fromString is case insensitive for both`() {
        assertEquals(ScreenType.BOTH, ScreenType.fromString("both"))
        assertEquals(ScreenType.BOTH, ScreenType.fromString("Both"))
    }

    @Test
    fun `fromString is case insensitive for live`() {
        assertEquals(ScreenType.LIVE, ScreenType.fromString("live"))
        assertEquals(ScreenType.LIVE, ScreenType.fromString("Live"))
    }

    @Test
    fun `fromString with null returns BOTH as default`() {
        assertEquals(ScreenType.BOTH, ScreenType.fromString(null))
    }

    @Test
    fun `fromString with empty string returns BOTH as default`() {
        assertEquals(ScreenType.BOTH, ScreenType.fromString(""))
    }

    @Test
    fun `fromString with invalid value returns BOTH as default`() {
        assertEquals(ScreenType.BOTH, ScreenType.fromString("invalid"))
        assertEquals(ScreenType.BOTH, ScreenType.fromString("screen"))
        assertEquals(ScreenType.BOTH, ScreenType.fromString("123"))
    }

    @Test
    fun `fromString with whitespace returns BOTH as default`() {
        assertEquals(ScreenType.BOTH, ScreenType.fromString(" "))
        assertEquals(ScreenType.BOTH, ScreenType.fromString("  HOME  "))
    }

    // ============================================================
    // Test: Enum values
    // ============================================================

    @Test
    fun `enum has exactly four values`() {
        assertEquals(4, ScreenType.entries.size)
    }

    @Test
    fun `enum values are HOME, LOCK, BOTH, LIVE`() {
        assertTrue(ScreenType.entries.contains(ScreenType.HOME))
        assertTrue(ScreenType.entries.contains(ScreenType.LOCK))
        assertTrue(ScreenType.entries.contains(ScreenType.BOTH))
        assertTrue(ScreenType.entries.contains(ScreenType.LIVE))
    }

    @Test
    fun `enum names are correct`() {
        assertEquals("HOME", ScreenType.HOME.name)
        assertEquals("LOCK", ScreenType.LOCK.name)
        assertEquals("BOTH", ScreenType.BOTH.name)
        assertEquals("LIVE", ScreenType.LIVE.name)
    }
}
