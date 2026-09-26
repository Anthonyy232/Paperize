package com.anthonyla.paperize.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class BrightnessCalculatorTest {
    @Test
    fun `luminance ignores alpha and weights color channels`() {
        assertEquals(0.0, BrightnessCalculator.calculateLuminance(0xFF000000.toInt()), 0.001)
        assertEquals(1.0, BrightnessCalculator.calculateLuminance(0xFFFFFFFF.toInt()), 0.001)
        assertEquals(0.2126, BrightnessCalculator.calculateLuminance(0x00FF0000), 0.001)
    }

    @Test
    fun `adaptive brightness changes only images at odds with the system theme`() {
        assertEquals(0.7f, 0.9f * BrightnessCalculator.getAdaptiveMultiplier(true, 0.9f), 0.001f)
        assertEquals(0.4f, 0.2f * BrightnessCalculator.getAdaptiveMultiplier(false, 0.2f), 0.001f)
        assertEquals(1f, BrightnessCalculator.getAdaptiveMultiplier(true, 0.5f), 0.001f)
        assertEquals(1f, BrightnessCalculator.getAdaptiveMultiplier(false, 0.5f), 0.001f)
    }

    @Test
    fun `near black images are not amplified or divided by zero`() {
        listOf(0f, 0.005f).forEach { brightness ->
            assertEquals(1f, BrightnessCalculator.getAdaptiveMultiplier(false, brightness), 0f)
        }
    }
}
