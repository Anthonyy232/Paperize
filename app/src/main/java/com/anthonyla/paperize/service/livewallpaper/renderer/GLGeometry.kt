package com.anthonyla.paperize.service.livewallpaper.renderer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

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
