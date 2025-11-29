package com.anthonyla.paperize.service.livewallpaper.renderer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max

/**
 * Geometry utilities for OpenGL rendering including vertex positions,
 * texture coordinates, and adaptive parallax calculations.
 */
object GLGeometry {

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
     * Calculate texture coordinates with adaptive parallax offset.
     * The parallax effect pans the image based on home screen scroll position.
     *
     * @param normalOffsetX Normalized scroll offset (0.0 = leftmost, 1.0 = rightmost)
     * @param parallaxIntensity User setting (0-100, percentage of maximum parallax)
     * @param imageWidth Original image width in pixels
     * @param imageHeight Original image height in pixels
     * @param surfaceWidth Surface width in pixels
     * @param surfaceHeight Surface height in pixels
     * @return Texture coordinate array with parallax offset applied
     */
    fun calculateParallaxTexCoords(
        normalOffsetX: Float,
        parallaxIntensity: Int,
        imageWidth: Int,
        imageHeight: Int,
        surfaceWidth: Int,
        surfaceHeight: Int
    ): FloatArray {
        if (parallaxIntensity == 0) {
            return TEX_COORDS.clone()
        }

        // Calculate aspect ratios
        val imageAspect = imageWidth.toFloat() / imageHeight
        val surfaceAspect = surfaceWidth.toFloat() / surfaceHeight

        // Determine how much the image needs to be scaled to fill the surface
        // while maintaining aspect ratio
        val scaleFactor = if (imageAspect > surfaceAspect) {
            // Image is wider than surface - scale by height
            1.0f
        } else {
            // Image is narrower than surface - scale by width to fill
            surfaceAspect / imageAspect
        }

        // Calculate how much extra width we have for parallax
        // This is the difference between scaled width and visible width
        val extraWidth = max(0f, scaleFactor - 1.0f)

        // Apply parallax intensity (0-100% of available extra width)
        val parallaxAmount = extraWidth * (parallaxIntensity / 100.0f)

        // Map scroll position (0-1) to texture coordinate offset
        // Center position (0.5) shows middle of image
        // Left position (0.0) shows left side, right (1.0) shows right side
        val offset = parallaxAmount * (normalOffsetX - 0.5f)

        // Calculate texture coordinate bounds with offset
        val left = 0.0f - offset
        val right = 1.0f - offset

        return floatArrayOf(
            left, 1.0f,   // Bottom-left
            right, 1.0f,  // Bottom-right
            left, 0.0f,   // Top-left
            right, 0.0f   // Top-right
        )
    }

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
