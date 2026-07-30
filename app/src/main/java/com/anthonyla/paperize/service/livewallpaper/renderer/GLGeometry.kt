package com.anthonyla.paperize.service.livewallpaper.renderer

import com.anthonyla.paperize.core.ScalingType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * Geometry utilities for OpenGL rendering including vertex positions,
 * texture coordinates, and adaptive parallax calculations.
 */
object GLGeometry {

    data class WallpaperTransform(
        val scaledWidth: Float,
        val scaledHeight: Float,
        val horizontalOffset: Float
    )

    data class CrossfadeAlphas(
        val current: Float,
        val next: Float
    )

    /**
     * Alpha values for source-over crossfading.
     *
     * The current layer must stay opaque while the next layer fades over it. Fading
     * both layers produces a visible brightness dip at the midpoint.
     */
    fun calculateCrossfadeAlphas(
        progress: Float,
        hasNextPicture: Boolean
    ): CrossfadeAlphas {
        val nextAlpha = if (hasNextPicture) progress.coerceIn(0f, 1f) else 0f
        return CrossfadeAlphas(current = 1f, next = nextAlpha)
    }

    /**
     * Calculate live-wallpaper scale and launcher-scroll offset without OpenGL dependencies.
     */
    fun calculateWallpaperTransform(
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        scalingType: ScalingType,
        parallaxEnabled: Boolean,
        parallaxIntensity: Int,
        normalizedOffsetX: Float
    ): WallpaperTransform {
        require(viewWidth > 0f && viewHeight > 0f)
        require(imageWidth > 0f && imageHeight > 0f)

        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val (baseScaleX, baseScaleY) = when (scalingType) {
            ScalingType.FILL -> max(scaleX, scaleY).let { it to it }
            ScalingType.FIT -> min(scaleX, scaleY).let { it to it }
            ScalingType.STRETCH -> scaleX to scaleY
            ScalingType.NONE -> 1f to 1f
        }

        var effectiveScaleX = baseScaleX
        var effectiveScaleY = baseScaleY
        val intensity = if (parallaxEnabled) {
            parallaxIntensity.coerceIn(0, 100) / 100f
        } else {
            0f
        }

        if (intensity > 0f) {
            val currentWidth = imageWidth * effectiveScaleX
            val minimumExtraWidth = viewWidth * intensity * 0.2f
            if (currentWidth - viewWidth < minimumExtraWidth) {
                val zoom = (viewWidth + minimumExtraWidth) / currentWidth
                effectiveScaleX *= zoom
                effectiveScaleY *= zoom
            }
        }

        val scaledWidth = imageWidth * effectiveScaleX
        val scaledHeight = imageHeight * effectiveScaleY
        val extraWidth = max(0f, scaledWidth - viewWidth)
        val offset = normalizedOffsetX.coerceIn(0f, 1f)
        return WallpaperTransform(
            scaledWidth = scaledWidth,
            scaledHeight = scaledHeight,
            horizontalOffset = extraWidth * intensity * (0.5f - offset)
        )
    }

    /**
     * Full-screen quad vertices in normalized device coordinates (-1 to 1).
     * Layout: x, y for each vertex (2 floats per vertex, 4 vertices).
     * Order: bottom-left, bottom-right, top-left, top-right (triangle strip).
     */
    val VERTICES = floatArrayOf(
        -1.0f, -1.0f,  // Bottom-left
         1.0f, -1.0f,  // Bottom-right
        -1.0f,  1.0f,  // Top-left
         1.0f,  1.0f   // Top-right
    )

    /**
     * Standard texture coordinates (0 to 1, Y-flipped for OpenGL convention).
     * Layout: u, v for each vertex (2 floats per vertex, 4 vertices).
     * Order: bottom-left, bottom-right, top-left, top-right.
     */
    val TEX_COORDS = floatArrayOf(
        0.0f, 1.0f,  // Bottom-left
        1.0f, 1.0f,  // Bottom-right
        0.0f, 0.0f,  // Top-left
        1.0f, 0.0f   // Top-right
    )



    /**
     * Create a native-order FloatBuffer from a float array.
     * Required for passing vertex data to OpenGL.
     *
     * @param data Float array to convert
     * @return FloatBuffer ready for use with OpenGL
     */
    fun createFloatBuffer(data: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(data)
                position(0)
            }
    }

    /**
     * Update an existing FloatBuffer with new data.
     * More efficient than creating a new buffer.
     *
     * @param buffer Buffer to update
     * @param data New data to put in buffer
     */
    fun updateFloatBuffer(buffer: FloatBuffer, data: FloatArray) {
        buffer.clear()
        buffer.put(data)
        buffer.position(0)
    }
}
