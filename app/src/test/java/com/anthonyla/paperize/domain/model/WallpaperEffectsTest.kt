package com.anthonyla.paperize.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperEffectsTest {
    @Test
    fun `validation clamps every percentage and preserves effect toggles`() {
        listOf(-1 to 0, 0 to 0, 50 to 50, 100 to 100, 101 to 100).forEach { (input, expected) ->
            val effects = WallpaperEffects(
                enableBlur = true,
                enableParallax = true,
                darkenPercentage = input,
                blurPercentage = input,
                vignettePercentage = input,
                grayscalePercentage = input,
                parallaxIntensity = input
            )
            assertEquals(
                effects.copy(
                    darkenPercentage = expected,
                    blurPercentage = expected,
                    vignettePercentage = expected,
                    grayscalePercentage = expected,
                    parallaxIntensity = expected
                ),
                effects.validate()
            )
        }
    }
}
