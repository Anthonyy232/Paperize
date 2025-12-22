package com.anthonyla.paperize.domain.model

import com.anthonyla.paperize.core.constants.Constants
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WallpaperEffects domain model
 */
class WallpaperEffectsTest {

    // ============================================================
    // Test: hasEffects
    // ============================================================

    @Test
    fun `hasEffects returns false when all effects disabled`() {
        val effects = WallpaperEffects.none()
        assertFalse(effects.hasEffects)
    }

    @Test
    fun `hasEffects returns false when blur enabled but percentage is zero`() {
        val effects = WallpaperEffects(enableBlur = true, blurPercentage = 0)
        assertFalse(effects.hasEffects)
    }

    @Test
    fun `hasEffects returns true when blur enabled with non-zero percentage`() {
        val effects = WallpaperEffects(enableBlur = true, blurPercentage = 50)
        assertTrue(effects.hasEffects)
    }

    @Test
    fun `hasEffects returns true when darken enabled with non-zero percentage`() {
        val effects = WallpaperEffects(enableDarken = true, darkenPercentage = 25)
        assertTrue(effects.hasEffects)
    }

    @Test
    fun `hasEffects returns true when vignette enabled with non-zero percentage`() {
        val effects = WallpaperEffects(enableVignette = true, vignettePercentage = 75)
        assertTrue(effects.hasEffects)
    }

    @Test
    fun `hasEffects returns true when grayscale enabled with non-zero percentage`() {
        val effects = WallpaperEffects(enableGrayscale = true, grayscalePercentage = 100)
        assertTrue(effects.hasEffects)
    }

    @Test
    fun `hasEffects returns true when multiple effects are enabled`() {
        val effects = WallpaperEffects(
            enableBlur = true,
            blurPercentage = 10,
            enableDarken = true,
            darkenPercentage = 20
        )
        assertTrue(effects.hasEffects)
    }

    @Test
    fun `hasEffects ignores interactive effects`() {
        val effects = WallpaperEffects(
            enableDoubleTap = true,
            enableParallax = true,
            parallaxIntensity = 100
        )
        assertFalse(effects.hasEffects)
    }

    // ============================================================
    // Test: validate()
    // ============================================================

    @Test
    fun `validate coerces darkenPercentage above 100 to 100`() {
        val effects = WallpaperEffects(darkenPercentage = 150)
        val validated = effects.validate()
        assertEquals(100, validated.darkenPercentage)
    }

    @Test
    fun `validate coerces negative darkenPercentage to 0`() {
        val effects = WallpaperEffects(darkenPercentage = -50)
        val validated = effects.validate()
        assertEquals(0, validated.darkenPercentage)
    }

    @Test
    fun `validate coerces blurPercentage above 100 to 100`() {
        val effects = WallpaperEffects(blurPercentage = 200)
        val validated = effects.validate()
        assertEquals(100, validated.blurPercentage)
    }

    @Test
    fun `validate coerces negative blurPercentage to 0`() {
        val effects = WallpaperEffects(blurPercentage = -10)
        val validated = effects.validate()
        assertEquals(0, validated.blurPercentage)
    }

    @Test
    fun `validate coerces vignettePercentage to valid range`() {
        val over = WallpaperEffects(vignettePercentage = 120)
        val under = WallpaperEffects(vignettePercentage = -5)
        
        assertEquals(100, over.validate().vignettePercentage)
        assertEquals(0, under.validate().vignettePercentage)
    }

    @Test
    fun `validate coerces grayscalePercentage to valid range`() {
        val over = WallpaperEffects(grayscalePercentage = 999)
        val under = WallpaperEffects(grayscalePercentage = -1)
        
        assertEquals(100, over.validate().grayscalePercentage)
        assertEquals(0, under.validate().grayscalePercentage)
    }

    @Test
    fun `validate coerces parallaxIntensity to valid range`() {
        val over = WallpaperEffects(parallaxIntensity = 150)
        val under = WallpaperEffects(parallaxIntensity = -25)
        
        assertEquals(100, over.validate().parallaxIntensity)
        assertEquals(0, under.validate().parallaxIntensity)
    }

    @Test
    fun `validate keeps valid values unchanged`() {
        val effects = WallpaperEffects(
            darkenPercentage = 50,
            blurPercentage = 25,
            vignettePercentage = 75,
            grayscalePercentage = 100,
            parallaxIntensity = 60
        )
        
        val validated = effects.validate()
        
        assertEquals(50, validated.darkenPercentage)
        assertEquals(25, validated.blurPercentage)
        assertEquals(75, validated.vignettePercentage)
        assertEquals(100, validated.grayscalePercentage)
        assertEquals(60, validated.parallaxIntensity)
    }

    @Test
    fun `validate handles boundary values correctly`() {
        val minEffects = WallpaperEffects(
            darkenPercentage = 0,
            blurPercentage = 0,
            vignettePercentage = 0,
            grayscalePercentage = 0,
            parallaxIntensity = 0
        )
        val maxEffects = WallpaperEffects(
            darkenPercentage = 100,
            blurPercentage = 100,
            vignettePercentage = 100,
            grayscalePercentage = 100,
            parallaxIntensity = 100
        )
        
        val validatedMin = minEffects.validate()
        val validatedMax = maxEffects.validate()
        
        assertEquals(0, validatedMin.darkenPercentage)
        assertEquals(100, validatedMax.darkenPercentage)
    }

    // ============================================================
    // Test: Companion object
    // ============================================================

    @Test
    fun `default factory creates effects with default values`() {
        val effects = WallpaperEffects.default()
        
        assertFalse(effects.enableBlur)
        assertEquals(Constants.DEFAULT_BLUR_PERCENTAGE, effects.blurPercentage)
        assertFalse(effects.enableDarken)
        assertEquals(Constants.DEFAULT_DARKEN_PERCENTAGE, effects.darkenPercentage)
        assertFalse(effects.enableVignette)
        assertFalse(effects.enableGrayscale)
        assertFalse(effects.enableDoubleTap)
        assertFalse(effects.enableParallax)
    }

    @Test
    fun `none factory creates same as default`() {
        val none = WallpaperEffects.none()
        val default = WallpaperEffects.default()
        
        assertEquals(default, none)
    }
}
