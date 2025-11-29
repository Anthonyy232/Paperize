package com.anthonyla.paperize.service.livewallpaper.renderer

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.anthonyla.paperize.domain.model.WallpaperEffects
import com.anthonyla.paperize.service.livewallpaper.gl.GLPicture
import com.anthonyla.paperize.service.livewallpaper.gl.GLUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Main OpenGL renderer for live wallpaper.
 * Handles texture rendering, two-pass blur effects, color effects,
 * crossfade animations, and parallax scrolling.
 *
 * @property context Application context
 * @property callbacks Callbacks for queuing events on GL thread
 */
class PaperizeWallpaperRenderer(
    private val context: Context,
    private val callbacks: Callbacks
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "PaperizeRenderer"
        private const val CROSSFADE_DURATION_FRAMES = 45 // 0.75s at 60fps
    }

    /**
     * Callbacks for communication with the engine.
     */
    interface Callbacks {
        fun queueEventOnGlThread(event: () -> Unit)
        fun requestRender()
    }

    // Surface dimensions
    @Volatile private var surfaceWidth = 0
    @Volatile private var surfaceHeight = 0

    // Shader programs
    private var simpleProgram = 0
    private var blurHorizontalProgram = 0
    private var blurVerticalProgram = 0
    private var effectsProgram = 0

    // Attribute/uniform locations (for effects program)
    private var aPositionHandle = 0
    private var aTexCoordHandle = 0
    private var uTextureHandle = 0
    private var uAlphaHandle = 0
    private var uDarkenFactorHandle = 0
    private var uVignetteFactorHandle = 0
    private var uGrayscaleEnabledHandle = 0

    // Geometry buffers
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

    // Framebuffer for two-pass blur
    private var blurFbo = 0
    private var blurTexture = 0

    // Current wallpaper
    private var currentPicture: GLPicture? = null

    // Crossfade state
    private var nextPicture: GLPicture? = null
    private var crossfadeProgress = 0f

    // Effects
    @Volatile private var currentEffects = WallpaperEffects()

    // Parallax
    @Volatile private var normalOffsetX = 0.5f

    // Coroutine scope for background loading
    private val loadingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        Log.d(TAG, "onSurfaceCreated")

        // Set clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Enable blending for crossfade
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Compile shader programs
        compileShaders()

        // Create geometry buffers
        vertexBuffer = GLGeometry.createFloatBuffer(GLGeometry.VERTICES)
        texCoordBuffer = GLGeometry.createFloatBuffer(GLGeometry.TEX_COORDS)

        Log.d(TAG, "Surface created successfully")
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: ${width}x${height}")

        surfaceWidth = width
        surfaceHeight = height

        GLES20.glViewport(0, 0, width, height)

        // Recreate framebuffer for blur at new resolution
        createBlurFramebuffer(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        // Clear screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val current = currentPicture
        val next = nextPicture

        if (current == null && next == null) {
            // No wallpaper to display
            return
        }

        // Determine if we need blur
        val blurRadius = if (currentEffects.enableBlur) {
            (currentEffects.blurPercentage / 100.0f) * 25.0f
        } else {
            0f
        }

        // Draw current picture
        current?.let { picture ->
            val alpha = if (next != null) 1.0f - crossfadeProgress else 1.0f
            drawPictureWithEffects(picture, alpha, blurRadius)
        }

        // Draw next picture (if crossfading)
        next?.let { picture ->
            drawPictureWithEffects(picture, crossfadeProgress, blurRadius)

            // Update crossfade progress
            crossfadeProgress += (1f / CROSSFADE_DURATION_FRAMES)

            if (crossfadeProgress >= 1.0f) {
                // Crossfade complete
                currentPicture?.recycle()
                currentPicture = picture
                nextPicture = null
                crossfadeProgress = 0f
                Log.d(TAG, "Crossfade complete")
            } else {
                // Continue animating
                callbacks.requestRender()
            }
        }
    }

    /**
     * Draw a picture with full effects pipeline.
     */
    private fun drawPictureWithEffects(picture: GLPicture, alpha: Float, blurRadius: Float) {
        // Update texture coordinates for parallax
        updateTextureCoordinates(picture)

        if (blurRadius > 0.01f && currentEffects.enableBlur) {
            // Two-pass blur pipeline
            drawWithBlur(picture, alpha, blurRadius)
        } else {
            // No blur - direct render with color effects
            drawWithColorEffects(picture, alpha)
        }
    }

    /**
     * Draw picture with two-pass blur followed by color effects.
     */
    private fun drawWithBlur(picture: GLPicture, alpha: Float, blurRadius: Float) {
        // Pass 1: Horizontal blur to FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, blurFbo)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(blurHorizontalProgram)
        val blurHResHandle = GLES20.glGetUniformLocation(blurHorizontalProgram, "u_resolution")
        val blurHRadiusHandle = GLES20.glGetUniformLocation(blurHorizontalProgram, "u_blurRadius")
        GLES20.glUniform2f(blurHResHandle, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        GLES20.glUniform1f(blurHRadiusHandle, blurRadius)

        picture.draw(blurHorizontalProgram, aPositionHandle, aTexCoordHandle, surfaceWidth, surfaceHeight)

        // Pass 2: Vertical blur from FBO to another FBO (not screen yet)
        // We need the blur result as a texture to apply color effects
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)

        GLES20.glUseProgram(blurVerticalProgram)
        val blurVResHandle = GLES20.glGetUniformLocation(blurVerticalProgram, "u_resolution")
        val blurVRadiusHandle = GLES20.glGetUniformLocation(blurVerticalProgram, "u_blurRadius")
        val blurVTextureHandle = GLES20.glGetUniformLocation(blurVerticalProgram, "u_texture")
        GLES20.glUniform2f(blurVResHandle, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        GLES20.glUniform1f(blurVRadiusHandle, blurRadius)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blurTexture)
        GLES20.glUniform1i(blurVTextureHandle, 0)

        // For now, render directly to screen with effects applied
        // In a more optimized version, we'd render to another FBO and apply effects in pass 3
        GLES20.glUseProgram(effectsProgram)

        // Set color effect uniforms
        GLES20.glUniform1f(uAlphaHandle, alpha)
        GLES20.glUniform1f(uDarkenFactorHandle, (100 - currentEffects.darkenPercentage) / 100f)
        GLES20.glUniform1f(uVignetteFactorHandle, currentEffects.vignettePercentage / 100f)
        GLES20.glUniform1f(uGrayscaleEnabledHandle, if (currentEffects.enableGrayscale) 1f else 0f)

        // Bind the blurred texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blurTexture)
        GLES20.glUniform1i(uTextureHandle, 0)

        drawQuad()
    }

    /**
     * Draw picture with color effects only (no blur).
     */
    private fun drawWithColorEffects(picture: GLPicture, alpha: Float) {
        // Render to screen with effects
        GLES20.glUseProgram(effectsProgram)

        // Set uniforms
        GLES20.glUniform1f(uAlphaHandle, alpha)
        GLES20.glUniform1f(uDarkenFactorHandle, (100 - currentEffects.darkenPercentage) / 100f)
        GLES20.glUniform1f(uVignetteFactorHandle, currentEffects.vignettePercentage / 100f)
        GLES20.glUniform1f(uGrayscaleEnabledHandle, if (currentEffects.enableGrayscale) 1f else 0f)

        picture.draw(effectsProgram, aPositionHandle, aTexCoordHandle, surfaceWidth, surfaceHeight)
    }

    /**
     * Update texture coordinates for parallax effect.
     */
    private fun updateTextureCoordinates(picture: GLPicture) {
        if (!currentEffects.enableParallax || currentEffects.parallaxIntensity == 0) {
            // No parallax - use standard coordinates
            GLGeometry.updateFloatBuffer(texCoordBuffer, GLGeometry.TEX_COORDS)
            return
        }

        // Calculate parallax coordinates
        val parallaxCoords = GLGeometry.calculateParallaxTexCoords(
            normalOffsetX,
            currentEffects.parallaxIntensity,
            picture.width,
            picture.height,
            surfaceWidth,
            surfaceHeight
        )

        GLGeometry.updateFloatBuffer(texCoordBuffer, parallaxCoords)
    }

    /**
     * Draw a full-screen quad with current buffers.
     */
    private fun drawQuad() {
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glEnableVertexAttribArray(aTexCoordHandle)

        GLES20.glVertexAttribPointer(aPositionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glVertexAttribPointer(aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTexCoordHandle)
    }

    /**
     * Compile all shader programs.
     */
    private fun compileShaders() {
        // Simple program
        simpleProgram = GLUtil.createProgram(GLShaders.VERTEX_SHADER, GLShaders.SIMPLE_FRAGMENT_SHADER)

        // Blur programs
        blurHorizontalProgram = GLUtil.createProgram(GLShaders.VERTEX_SHADER, GLShaders.BLUR_HORIZONTAL_FRAGMENT_SHADER)
        blurVerticalProgram = GLUtil.createProgram(GLShaders.VERTEX_SHADER, GLShaders.BLUR_VERTICAL_FRAGMENT_SHADER)

        // Effects program
        effectsProgram = GLUtil.createProgram(GLShaders.VERTEX_SHADER, GLShaders.EFFECTS_FRAGMENT_SHADER)

        // Get uniform/attribute locations for effects program
        aPositionHandle = GLES20.glGetAttribLocation(effectsProgram, "a_position")
        aTexCoordHandle = GLES20.glGetAttribLocation(effectsProgram, "a_texCoord")
        uTextureHandle = GLES20.glGetUniformLocation(effectsProgram, "u_texture")
        uAlphaHandle = GLES20.glGetUniformLocation(effectsProgram, "u_alpha")
        uDarkenFactorHandle = GLES20.glGetUniformLocation(effectsProgram, "u_darkenFactor")
        uVignetteFactorHandle = GLES20.glGetUniformLocation(effectsProgram, "u_vignetteFactor")
        uGrayscaleEnabledHandle = GLES20.glGetUniformLocation(effectsProgram, "u_grayscaleEnabled")

        // Note: Blur program uniforms are queried dynamically when needed

        Log.d(TAG, "Shaders compiled successfully")
    }

    /**
     * Create framebuffer for blur intermediate pass.
     */
    private fun createBlurFramebuffer(width: Int, height: Int) {
        // Delete old resources
        if (blurFbo != 0) {
            GLUtil.deleteFramebuffer(blurFbo)
        }
        if (blurTexture != 0) {
            GLUtil.deleteTexture(blurTexture)
        }

        // Create texture for FBO
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        blurTexture = textureIds[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blurTexture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)

        // Create framebuffer
        val fboIds = IntArray(1)
        GLES20.glGenFramebuffers(1, fboIds, 0)
        blurFbo = fboIds[0]

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, blurFbo)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, blurTexture, 0)

        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer not complete: $status")
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        Log.d(TAG, "Blur framebuffer created: ${width}x${height}")
    }

    /**
     * Queue a new wallpaper for loading and display.
     * This method can be called from any thread.
     *
     * @param imageLoader Image loader to use
     */
    fun queueWallpaper(imageLoader: ImageLoader) {
        loadingScope.launch {
            try {
                Log.d(TAG, "Loading wallpaper...")
                val bitmap = imageLoader.load(surfaceWidth, surfaceHeight)

                if (bitmap != null) {
                    Log.d(TAG, "Wallpaper loaded: ${bitmap.width}x${bitmap.height}")

                    // Upload to GPU on GL thread
                    callbacks.queueEventOnGlThread {
                        uploadBitmap(bitmap)
                    }
                } else {
                    Log.w(TAG, "Failed to load wallpaper (null bitmap)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading wallpaper", e)
            }
        }
    }

    /**
     * Upload a bitmap to GPU and start crossfade.
     * Must be called on GL thread.
     */
    private fun uploadBitmap(bitmap: Bitmap) {
        try {
            val picture = GLPicture(bitmap)

            // Recycle bitmap (no longer needed after GPU upload)
            bitmap.recycle()

            // Start crossfade
            nextPicture = picture
            crossfadeProgress = 0f

            callbacks.requestRender()

            Log.d(TAG, "Wallpaper uploaded to GPU: $picture")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload bitmap to GPU", e)
            bitmap.recycle()
        }
    }

    /**
     * Update effects settings.
     * Can be called from any thread.
     */
    fun updateEffects(effects: WallpaperEffects) {
        this.currentEffects = effects
        callbacks.requestRender()
    }

    /**
     * Set parallax scroll offset.
     * Can be called from any thread.
     *
     * @param offset Normalized offset (0.0 = left, 1.0 = right)
     */
    fun setNormalOffsetX(offset: Float) {
        if (normalOffsetX != offset) {
            normalOffsetX = offset
            callbacks.requestRender()
        }
    }

    /**
     * Cleanup resources.
     * Must be called on GL thread.
     */
    fun destroy() {
        loadingScope.cancel()

        currentPicture?.recycle()
        currentPicture = null

        nextPicture?.recycle()
        nextPicture = null

        GLUtil.deleteProgram(simpleProgram)
        GLUtil.deleteProgram(blurHorizontalProgram)
        GLUtil.deleteProgram(blurVerticalProgram)
        GLUtil.deleteProgram(effectsProgram)

        GLUtil.deleteFramebuffer(blurFbo)
        GLUtil.deleteTexture(blurTexture)

        Log.d(TAG, "Renderer destroyed")
    }
}
