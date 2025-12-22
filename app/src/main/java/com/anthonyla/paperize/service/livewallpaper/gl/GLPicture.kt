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
 * Large images are subdivided into tiles to avoid texture size limits.
 * Optimized for modern devices (API 31+) with larger tile sizes and pre-allocated buffers.
 *
 * @property width Original bitmap width
 * @property height Original bitmap height
 */
class GLPicture(
    bitmap: Bitmap,
    val brightnessFactor: Float = 1.0f
) {

    companion object {
        private const val TAG = "GLPicture"
        // Use larger tiles for modern devices - reduces draw calls significantly
        private const val PREFERRED_TILE_SIZE = 4096

        // Cache max texture size to avoid repeated GL queries
        @Volatile
        private var cachedMaxTextureSize = 0

        private fun getOptimalTileSize(): Int {
            if (cachedMaxTextureSize == 0) {
                cachedMaxTextureSize = GLUtil.getMaxTextureSize()
            }
            // Use up to 4096 for good memory/performance balance
            return min(cachedMaxTextureSize, PREFERRED_TILE_SIZE)
        }
    }

    val width = bitmap.width
    val height = bitmap.height

    private val tiles: Array<Tile>
    private val cols: Int
    private val rows: Int

    init {
        // Validate bitmap before processing
        require(!bitmap.isRecycled) { "Cannot create GLPicture from recycled bitmap" }
        require(bitmap.width > 0 && bitmap.height > 0) { 
            "Cannot create GLPicture from bitmap with invalid dimensions: ${bitmap.width}x${bitmap.height}" 
        }
        
        val maxTextureSize = GLCompatibility.getSafeMaxTextureSize(getOptimalTileSize())
        val tileSize = maxTextureSize

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

            // Calculate geometry once at creation time (not per-frame)
            val left = -1f + (tileX.toFloat() / width) * 2f
            val right = -1f + ((tileX + tileWidth).toFloat() / width) * 2f
            val bottom = 1f - ((tileY + tileHeight).toFloat() / height) * 2f
            val top = 1f - (tileY.toFloat() / height) * 2f

            val vertices = floatArrayOf(
                left, bottom,   // Bottom-left
                right, bottom,  // Bottom-right
                left, top,      // Top-left
                right, top      // Top-right
            )

            val texCoords = floatArrayOf(
                0f, 1f,  // Bottom-left
                1f, 1f,  // Bottom-right
                0f, 0f,  // Top-left
                1f, 0f   // Top-right
            )

            // Pre-allocate buffers (reused every frame)
            val vertexBuffer = createFloatBuffer(vertices)
            val texCoordBuffer = createFloatBuffer(texCoords)

            // Extract and upload tile bitmap
            val tileBitmap = Bitmap.createBitmap(bitmap, tileX, tileY, tileWidth, tileHeight)
            val textureId = loadTexture(tileBitmap)
            tileBitmap.recycle()

            Tile(
                textureId = textureId,
                x = tileX,
                y = tileY,
                width = tileWidth,
                height = tileHeight,
                vertexBuffer = vertexBuffer,
                texCoordBuffer = texCoordBuffer
            )
        }
    }

    /**
     * Draw all tiles to cover the full picture.
     * Uses pre-allocated buffers for optimal performance (zero allocations per frame).
     *
     * @param program Shader program to use
     * @param aPositionHandle Attribute location for vertex positions
     * @param aTexCoordHandle Attribute location for texture coordinates
     */
    fun draw(
        program: Int,
        aPositionHandle: Int,
        aTexCoordHandle: Int,
        mvpMatrix: FloatArray,
        uMvpMatrixHandle: Int
    ) {
        GLES20.glUseProgram(program)

        // Enable vertex attribute arrays
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glEnableVertexAttribArray(aTexCoordHandle)

        // Pass MVP matrix
        GLES20.glUniformMatrix4fv(uMvpMatrixHandle, 1, false, mvpMatrix, 0)

        // Draw each tile using pre-allocated buffers (no per-frame allocation)
        for (tile in tiles) {
            // Bind texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tile.textureId)

            // Use pre-allocated buffers - no allocation here!
            GLES20.glVertexAttribPointer(
                aPositionHandle, 2, GLES20.GL_FLOAT, false, 0, tile.vertexBuffer
            )
            GLES20.glVertexAttribPointer(
                aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, tile.texCoordBuffer
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
     * Represents a single tile of the picture with pre-allocated GPU buffers.
     */
    private data class Tile(
        val textureId: Int,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val vertexBuffer: FloatBuffer,
        val texCoordBuffer: FloatBuffer
    )

    override fun toString(): String {
        return "GLPicture(size=${width}x${height}, tiles=${cols}x${rows})"
    }
}
