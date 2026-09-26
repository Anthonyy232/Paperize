package com.anthonyla.paperize.service.livewallpaper.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import com.anthonyla.paperize.service.livewallpaper.renderer.GLGeometry
import java.nio.FloatBuffer
import kotlin.math.ceil
import kotlin.math.min

/** Splits large images into textures within the device limit. Geometry is allocated once per tile. */
class GLPicture(
    bitmap: Bitmap,
    /**
     * Luminance of the unmodified source bitmap. The renderer derives the adaptive
     * brightness multiplier at draw time so toggling the setting affects the image
     * that is already on screen.
     */
    val sourceBrightness: Float
) {

    companion object {
        private const val TAG = "GLPicture"

    }

    val width = bitmap.width
    val height = bitmap.height

    private val tiles: Array<Tile>
    private val cols: Int
    private val rows: Int

    init {
        require(!bitmap.isRecycled) { "Cannot create GLPicture from recycled bitmap" }
        val tileSize = GLCompatibility.getSafeMaxTextureSize(GLUtil.getMaxTextureSize())

        cols = ceil(width.toFloat() / tileSize).toInt()
        rows = ceil(height.toFloat() / tileSize).toInt()

        Log.d(TAG, "Creating GLPicture: ${width}x${height}, tiling to ${cols}x${rows} (tile size: $tileSize)")

        val partialTiles = ArrayList<Tile>(cols * rows)
        try {
            for (index in 0 until cols * rows) {
                val col = index % cols
                val row = index / cols

                val tileX = col * tileSize
                val tileY = row * tileSize
                val tileWidth = min(tileSize, width - tileX)
                val tileHeight = min(tileSize, height - tileY)

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

                val vertexBuffer = GLGeometry.createFloatBuffer(vertices)
                val texCoordBuffer = GLGeometry.createFloatBuffer(GLGeometry.TEX_COORDS)

                val tileBitmap = Bitmap.createBitmap(bitmap, tileX, tileY, tileWidth, tileHeight)
                val textureId = try {
                    loadTexture(tileBitmap)
                } finally {
                    if (tileBitmap !== bitmap) tileBitmap.recycle()
                }

                partialTiles.add(Tile(
                    textureId = textureId,
                    vertexBuffer = vertexBuffer,
                    texCoordBuffer = texCoordBuffer
                ))
            }
        } catch (e: Throwable) {
            for (tile in partialTiles) {
                GLUtil.deleteTexture(tile.textureId)
            }
            throw e
        }
        tiles = partialTiles.toTypedArray()
    }

    fun draw(
        program: Int,
        aPositionHandle: Int,
        aTexCoordHandle: Int,
        mvpMatrix: FloatArray,
        uMvpMatrixHandle: Int
    ) {
        GLES20.glUseProgram(program)

        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glEnableVertexAttribArray(aTexCoordHandle)

        GLES20.glUniformMatrix4fv(uMvpMatrixHandle, 1, false, mvpMatrix, 0)

        for (tile in tiles) {
            // Bind texture to unit 0 (must set active unit explicitly to avoid relying on GL state)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tile.textureId)

            GLES20.glVertexAttribPointer(
                aPositionHandle, 2, GLES20.GL_FLOAT, false, 0, tile.vertexBuffer
            )
            GLES20.glVertexAttribPointer(
                aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, tile.texCoordBuffer
            )

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }

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

    private fun loadTexture(bitmap: Bitmap): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]

        if (textureId == 0) {
            throw RuntimeException("Failed to generate texture ID")
        }

        try {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            GLUtil.checkGLError("texture upload")

            return textureId
        } catch (e: Throwable) {
            GLES20.glDeleteTextures(1, textureIds, 0)
            throw e
        }
    }

    private data class Tile(
        val textureId: Int,
        val vertexBuffer: FloatBuffer,
        val texCoordBuffer: FloatBuffer
    )

    override fun toString(): String {
        return "GLPicture(size=${width}x${height}, tiles=${cols}x${rows})"
    }
}
