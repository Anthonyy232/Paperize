package com.anthonyla.paperize.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoredEnumTest {
    @Test fun storedNamesAcceptEitherCase() {
        ScalingType.entries.forEach { assertEquals(it, ScalingType.fromString(it.name.lowercase())) }
        ScreenType.entries.forEach { assertEquals(it, ScreenType.fromString(it.name.lowercase())) }
        WallpaperMode.entries.forEach { assertEquals(it, WallpaperMode.fromString(it.name.lowercase())) }
        WallpaperMediaType.entries.forEach { assertEquals(it, WallpaperMediaType.fromString(it.name.lowercase())) }
    }

    @Test fun missingOrUnrecognizedValuesUseTheirStorageDefaults() {
        listOf(null, "", "unrecognized").forEach {
            assertEquals(ScalingType.FILL, ScalingType.fromString(it))
            assertEquals(ScreenType.BOTH, ScreenType.fromString(it))
            assertEquals(WallpaperMode.STATIC, WallpaperMode.fromString(it))
            assertNull(WallpaperMediaType.fromString(it))
        }
    }
}
