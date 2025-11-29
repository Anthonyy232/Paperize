package com.anthonyla.paperize.service.livewallpaper.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.ceil
import kotlin.math.min

/**
 * Represents a picture as a collection of OpenGL textures (tiles).
 * Large images are subdivided into 512x512 tiles to avoid texture size limits.
 * Based on Muzei's GLPicture implementation.
 *
 * @property width Original bitmap width
 * @property height Original bitmap height
 */
class GLPicture(bitmap: Bitmap) {

    companion object {
        private const val TAG = "GLPicture"
        private const val TILE_SIZE = 512
    }

    val width = bitmap.width
    val height = bitmap.height

    private val tiles: Array<Tile>
    private val cols: Int
    private val rows: Int

    init {
        val maxTextureSize = min(GLUtil.getMaxTextureSize(), 2048)
        val tileSize = min(TILE_SIZE, maxTextureSize)

        cols = ceil(width.toFloat() / tileSize).toInt()
        rows = ceil(height.toFloat() / tileSize).toInt()

        Log.d(TAG, "Creating GLPicture: ${width}x${height}, tiling to ${cols}x${rows} (tile size: $tileSize)")

        tiles = Array(cols * rows) { index ->
            val col = index % cols
            val row = index / cols

            val tileX = col * tileSize
            val tileY = row * tileSize
            val tileWidth = min(tileSize, width - tileX)
            val tileHeight = min(tileSize, height - tileY)

            // Extract tile bitmap
            val tileBitmap = Bitmap.createBitmap(bitmap, tileX, tileY, tileWidth, tileHeight)

            // Upload to GPU
            val textureId = loadTexture(tileBitmap)

            // Recycle tile bitmap (no longer needed after GPU upload)
            tileBitmap.recycle()

            Tile(
                textureId = textureId,
                x = tileX,
                y = tileY,
                width = tileWidth,
                height = tileHeight
            )
        }
    }

    /**
     * Draw all tiles to cover the full picture.
     *
     * @param program Shader program to use
     * @param aPositionHandle Attribute location for vertex positions
     * @param aTexCoordHandle Attribute location for texture coordinates
     * @param surfaceWidth Surface width for coordinate mapping
     * @param surfaceHeight Surface height for coordinate mapping
     */
    fun draw(
        program: Int,
        aPositionHandle: Int,
        aTexCoordHandle: Int,
        @Suppress("UNUSED_PARAMETER") surfaceWidth: Int,
        @Suppress("UNUSED_PARAMETER") surfaceHeight: Int
    ) {
        GLES20.glUseProgram(program)

        // Enable vertex attribute arrays
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glEnableVertexAttribArray(aTexCoordHandle)

        // Draw each tile
        for (tile in tiles) {
            // Bind texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tile.textureId)

            // Calculate normalized device coordinates for this tile
            val left = -1f + (tile.x.toFloat() / width) * 2f
            val right = -1f + ((tile.x + tile.width).toFloat() / width) * 2f
            val bottom = 1f - ((tile.y + tile.height).toFloat() / height) * 2f
            val top = 1f - (tile.y.toFloat() / height) * 2f

            // Vertex positions (clip space)
            val vertices = floatArrayOf(
                left, bottom,   // Bottom-left
                right, bottom,  // Bottom-right
                left, top,      // Top-left
                right, top      // Top-right
            )

            // Texture coordinates (full tile)
            val texCoords = floatArrayOf(
                0f, 1f,  // Bottom-left
                1f, 1f,  // Bottom-right
                0f, 0f,  // Top-left
                1f, 0f   // Top-right
            )

            val vertexBuffer = createFloatBuffer(vertices)
            val texCoordBuffer = createFloatBuffer(texCoords)

            // Set vertex data
            GLES20.glVertexAttribPointer(
                aPositionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer
            )
            GLES20.glVertexAttribPointer(
                aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer
            )

            // Draw quad
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }

        // Disable vertex attribute arrays
        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTexCoordHandle)
    }

    /**
     * Recycle all tile textures and free GPU memory.
     * Must be called on GL thread.
     */
    fun recycle() {
        for (tile in tiles) {
            GLUtil.deleteTexture(tile.textureId)
        }
    }

    /**
     * Load a bitmap as an OpenGL texture.
     */
    private fun loadTexture(bitmap: Bitmap): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]

        if (textureId == 0) {
            throw RuntimeException("Failed to generate texture ID")
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Set texture parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // Upload bitmap to GPU
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            throw RuntimeException("Texture upload failed: error 0x${Integer.toHexString(error)}")
        }

        return textureId
    }

    /**
     * Create a native-order float buffer from an array.
     */
    private fun createFloatBuffer(data: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(data)
                position(0)
            }
    }

    /**
     * Represents a single tile of the picture.
     */
    private data class Tile(
        val textureId: Int,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )

    override fun toString(): String {
        return "GLPicture(size=${width}x${height}, tiles=${cols}x${rows})"
    }
}
