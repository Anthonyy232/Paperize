package com.anthonyla.paperize.service.livewallpaper.renderer

import com.anthonyla.paperize.core.ScalingType
import org.junit.Assert.assertEquals
import org.junit.Test

class GLGeometryTest {

    @Test
    fun `fill parallax reaches both real image edges`() {
        val left = transform(ScalingType.FILL, offset = 0f)
        val center = transform(ScalingType.FILL, offset = 0.5f)
        val right = transform(ScalingType.FILL, offset = 1f)

        assertEquals(400f, left.scaledWidth, 0.001f)
        assertEquals(150f, left.horizontalOffset, 0.001f)
        assertEquals(0f, center.horizontalOffset, 0.001f)
        assertEquals(-150f, right.horizontalOffset, 0.001f)
    }

    @Test
    fun `fit adds bounded overscan when parallax is enabled`() {
        val left = transform(ScalingType.FIT, offset = 0f)
        val right = transform(ScalingType.FIT, offset = 1f)

        assertEquals(120f, left.scaledWidth, 0.001f)
        assertEquals(10f, left.horizontalOffset, 0.001f)
        assertEquals(-10f, right.horizontalOffset, 0.001f)
    }

    @Test
    fun `launcher offsets are clamped to documented range`() {
        assertEquals(
            transform(ScalingType.FILL, offset = 0f),
            transform(ScalingType.FILL, offset = -5f)
        )
        assertEquals(
            transform(ScalingType.FILL, offset = 1f),
            transform(ScalingType.FILL, offset = 5f)
        )
    }

    @Test
    fun `disabled parallax keeps a wide image centered`() {
        val result = transform(
            scalingType = ScalingType.FILL,
            offset = 0f,
            enabled = false
        )

        assertEquals(0f, result.horizontalOffset, 0.001f)
    }

    @Test
    fun `parallax intensity scales travel across existing overflow`() {
        val result = transform(
            scalingType = ScalingType.FILL,
            offset = 0f,
            intensity = 50
        )

        assertEquals(75f, result.horizontalOffset, 0.001f)
    }

    @Test
    fun `crossfade keeps current opaque while next fades in`() {
        val start = GLGeometry.calculateCrossfadeAlphas(0f, hasNextPicture = true)
        val midpoint = GLGeometry.calculateCrossfadeAlphas(0.5f, hasNextPicture = true)
        val end = GLGeometry.calculateCrossfadeAlphas(1f, hasNextPicture = true)

        assertEquals(1f, start.current, 0.001f)
        assertEquals(0f, start.next, 0.001f)
        assertEquals(1f, midpoint.current, 0.001f)
        assertEquals(0.5f, midpoint.next, 0.001f)
        assertEquals(1f, end.current, 0.001f)
        assertEquals(1f, end.next, 0.001f)
    }

    @Test
    fun `crossfade clamps progress and disables next alpha when absent`() {
        assertEquals(
            0f,
            GLGeometry.calculateCrossfadeAlphas(-1f, hasNextPicture = true).next,
            0.001f
        )
        assertEquals(
            1f,
            GLGeometry.calculateCrossfadeAlphas(2f, hasNextPicture = true).next,
            0.001f
        )
        assertEquals(
            0f,
            GLGeometry.calculateCrossfadeAlphas(0.75f, hasNextPicture = false).next,
            0.001f
        )
    }

    private fun transform(
        scalingType: ScalingType,
        offset: Float,
        enabled: Boolean = true,
        intensity: Int = 100
    ): GLGeometry.WallpaperTransform =
        GLGeometry.calculateWallpaperTransform(
            viewWidth = 100f,
            viewHeight = 200f,
            imageWidth = 400f,
            imageHeight = 200f,
            scalingType = scalingType,
            parallaxEnabled = enabled,
            parallaxIntensity = intensity,
            normalizedOffsetX = offset
        )
}
