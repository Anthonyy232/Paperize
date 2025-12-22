package com.anthonyla.paperize.core.util

import com.anthonyla.paperize.core.constants.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

class BrightnessCalculatorTest {

    @Test
    fun `calculateLuminance returns correct value for white`() {
        // White = 0xFFFFFFFF
        val white = 0xFFFFFFFF.toInt()
        val luminance = BrightnessCalculator.calculateLuminance(white)
        // 0.2126 * 1 + 0.7152 * 1 + 0.0722 * 1 = 1.0
        assertEquals(1.0, luminance, 0.001)
    }

    @Test
    fun `calculateLuminance returns correct value for black`() {
        // Black = 0xFF000000
        val black = 0xFF000000.toInt()
        val luminance = BrightnessCalculator.calculateLuminance(black)
        assertEquals(0.0, luminance, 0.001)
    }

    @Test
    fun `calculateLuminance returns correct value for red`() {
        // Red = 0xFFFF0000
        val red = 0xFFFF0000.toInt()
        val luminance = BrightnessCalculator.calculateLuminance(red)
        assertEquals(Constants.LUMINANCE_RED, luminance, 0.001)
    }

    @Test
    fun `getAdaptiveMultiplier in dark mode darkens bright image`() {
        // If brightness > LIGHT_BRIGHTNESS_MIN (0.8), should return TARGET_BRIGHTNESS_DARK / brightness
        val brightness = 0.9f
        val multiplier = BrightnessCalculator.getAdaptiveMultiplier(true, brightness)
        val expected = Constants.TARGET_BRIGHTNESS_DARK / brightness
        assertEquals(expected, multiplier, 0.001f)
    }

    @Test
    fun `getAdaptiveMultiplier in dark mode does not change dark image`() {
        // If brightness <= LIGHT_BRIGHTNESS_MIN (0.8), should return 1.0
        val brightness = 0.5f
        val multiplier = BrightnessCalculator.getAdaptiveMultiplier(true, brightness)
        assertEquals(1.0f, multiplier, 0.001f)
    }

    @Test
    fun `getAdaptiveMultiplier in light mode brightens dark image`() {
        // If brightness < DARK_BRIGHTNESS_MAX (0.3), should return TARGET_BRIGHTNESS_LIGHT / brightness
        val brightness = 0.2f
        val multiplier = BrightnessCalculator.getAdaptiveMultiplier(false, brightness)
        val expected = Constants.TARGET_BRIGHTNESS_LIGHT / brightness
        assertEquals(expected, multiplier, 0.001f)
    }

    @Test
    fun `getAdaptiveMultiplier in light mode does not change bright image`() {
        // If brightness >= DARK_BRIGHTNESS_MAX (0.3), should return 1.0
        val brightness = 0.5f
        val multiplier = BrightnessCalculator.getAdaptiveMultiplier(false, brightness)
        assertEquals(1.0f, multiplier, 0.001f)
    }

    @Test
    fun `getAdaptiveMultiplier returns 1_0 for very dark items to avoid noise`() {
        val brightness = 0.005f
        val multiplier = BrightnessCalculator.getAdaptiveMultiplier(true, brightness)
        assertEquals(1.0f, multiplier, 0.001f)
    }
}
