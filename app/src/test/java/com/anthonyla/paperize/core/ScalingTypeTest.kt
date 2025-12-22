package com.anthonyla.paperize.core

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ScalingType enum
 */
class ScalingTypeTest {

    // ============================================================
    // Test: fromString
    // ============================================================

    @Test
    fun `fromString with FILL returns FILL`() {
        assertEquals(ScalingType.FILL, ScalingType.fromString("FILL"))
    }

    @Test
    fun `fromString with FIT returns FIT`() {
        assertEquals(ScalingType.FIT, ScalingType.fromString("FIT"))
    }

    @Test
    fun `fromString with STRETCH returns STRETCH`() {
        assertEquals(ScalingType.STRETCH, ScalingType.fromString("STRETCH"))
    }

    @Test
    fun `fromString with NONE returns NONE`() {
        assertEquals(ScalingType.NONE, ScalingType.fromString("NONE"))
    }

    @Test
    fun `fromString is case insensitive`() {
        assertEquals(ScalingType.FILL, ScalingType.fromString("fill"))
        assertEquals(ScalingType.FILL, ScalingType.fromString("Fill"))
        assertEquals(ScalingType.FIT, ScalingType.fromString("fit"))
        assertEquals(ScalingType.FIT, ScalingType.fromString("Fit"))
        assertEquals(ScalingType.STRETCH, ScalingType.fromString("stretch"))
        assertEquals(ScalingType.NONE, ScalingType.fromString("none"))
    }

    @Test
    fun `fromString with null returns FILL as default`() {
        assertEquals(ScalingType.FILL, ScalingType.fromString(null))
    }

    @Test
    fun `fromString with empty string returns FILL as default`() {
        assertEquals(ScalingType.FILL, ScalingType.fromString(""))
    }

    @Test
    fun `fromString with invalid value returns FILL as default`() {
        assertEquals(ScalingType.FILL, ScalingType.fromString("invalid"))
        assertEquals(ScalingType.FILL, ScalingType.fromString("crop"))
        assertEquals(ScalingType.FILL, ScalingType.fromString("center"))
        assertEquals(ScalingType.FILL, ScalingType.fromString("123"))
    }

    @Test
    fun `fromString with whitespace returns FILL as default`() {
        assertEquals(ScalingType.FILL, ScalingType.fromString(" "))
        assertEquals(ScalingType.FILL, ScalingType.fromString("  FILL  "))
    }

    // ============================================================
    // Test: Enum values
    // ============================================================

    @Test
    fun `enum has exactly four values`() {
        assertEquals(4, ScalingType.entries.size)
    }

    @Test
    fun `enum values are FILL, FIT, STRETCH, NONE`() {
        assertTrue(ScalingType.entries.contains(ScalingType.FILL))
        assertTrue(ScalingType.entries.contains(ScalingType.FIT))
        assertTrue(ScalingType.entries.contains(ScalingType.STRETCH))
        assertTrue(ScalingType.entries.contains(ScalingType.NONE))
    }

    @Test
    fun `enum names are correct`() {
        assertEquals("FILL", ScalingType.FILL.name)
        assertEquals("FIT", ScalingType.FIT.name)
        assertEquals("STRETCH", ScalingType.STRETCH.name)
        assertEquals("NONE", ScalingType.NONE.name)
    }
}
