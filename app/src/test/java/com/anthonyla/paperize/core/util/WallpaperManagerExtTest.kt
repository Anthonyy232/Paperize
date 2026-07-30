package com.anthonyla.paperize.core.util

import java.io.IOException
import org.junit.Assert.assertThrows
import org.junit.Test

class WallpaperManagerExtTest {

    @Test
    fun `zero wallpaper id is treated as failure`() {
        assertThrows(IOException::class.java) {
            requireWallpaperSetSucceeded(0)
        }
    }

    @Test
    fun `positive wallpaper id is accepted`() {
        requireWallpaperSetSucceeded(42)
    }
}
