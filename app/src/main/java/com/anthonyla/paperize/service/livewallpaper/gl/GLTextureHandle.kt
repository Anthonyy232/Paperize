package com.anthonyla.paperize.service.livewallpaper.gl

import android.util.Log

/**
 * Handle for an OpenGL texture that manages lifecycle and prevents leaks.
 * Textures must be explicitly recycled when no longer needed.
 *
 * @property textureId OpenGL texture ID
 * @property width Texture width in pixels
 * @property height Texture height in pixels
 */
class GLTextureHandle(
    val textureId: Int,
    val width: Int,
    val height: Int
) {

    companion object {
        private const val TAG = "GLTextureHandle"
    }

    @Volatile
    private var recycled = false

    /**
     * Recycle the texture and free GPU memory.
     * This must be called on the GL thread.
     */
    fun recycle() {
        if (!recycled) {
            GLUtil.deleteTexture(textureId)
            recycled = true
        }
    }

    /**
     * Check if this texture has been recycled.
     */
    fun isRecycled(): Boolean = recycled

    /**
     * Finalize method to detect leaked textures.
     * Note: Cannot call GL functions from finalizer (wrong thread).
     */
    @Suppress("removal")
    protected fun finalize() {
        if (!recycled) {
            Log.w(TAG, "Texture $textureId (${width}x${height}) was not recycled! This is a memory leak.")
        }
    }

    override fun toString(): String {
        return "GLTextureHandle(id=$textureId, size=${width}x${height}, recycled=$recycled)"
    }
}
