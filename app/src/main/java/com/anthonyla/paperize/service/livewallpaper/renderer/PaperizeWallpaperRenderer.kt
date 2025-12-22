package com.anthonyla.paperize.service.livewallpaper.renderer
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.ScalingType

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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import java.nio.FloatBuffer
import android.opengl.Matrix
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
        // Time-based crossfade duration in milliseconds (consistent across all refresh rates)
        private const val CROSSFADE_DURATION_MS = Constants.CROSSFADE_DURATION_MS
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

    // Display refresh rate (for future optimizations)
    private var displayRefreshRate = 60f

    // Shader programs
    private var simpleProgram = 0
    private var blurHorizontalProgram = 0
    private var blurVerticalProgram = 0
    private var effectsProgram = 0

    // Attribute/uniform locations (for effects program)
    private var aPositionHandle = 0
    private var aTexCoordHandle = 0
    private var uTextureHandle = 0
    private var uMvpMatrixHandle = 0
    private var uAlphaHandle = 0
    private var uDarkenFactorHandle = 0
    private var uVignetteFactorHandle = 0
    private var uGrayscaleFactorHandle = 0
    private var uAdaptiveBrightnessFactorHandle = 0

    // Cached uniform locations for horizontal blur program
    private var blurHPositionHandle = 0
    private var blurHTexCoordHandle = 0
    private var blurHMvpMatrixHandle = 0
    private var blurHTextureHandle = 0
    private var blurHResolutionHandle = 0
    private var blurHRadiusHandle = 0

    // Cached uniform locations for vertical blur program
    private var blurVPositionHandle = 0
    private var blurVTexCoordHandle = 0
    private var blurVMvpMatrixHandle = 0
    private var blurVTextureHandle = 0
    private var blurVResolutionHandle = 0
    private var blurVRadiusHandle = 0

    // Geometry buffers
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

    // Framebuffers for two-pass blur (need two for proper blur + effects pipeline)
    private var blurFbo1 = 0
    private var blurTexture1 = 0
    private var blurFbo2 = 0
    private var blurTexture2 = 0

    // Current wallpaper
    private var currentPicture: GLPicture? = null

    // Crossfade state (time-based)
    private var nextPicture: GLPicture? = null
    private var crossfadeProgress = 0f
    private var crossfadeStartTimeNanos = 0L

    // Effects
    @Volatile private var currentEffects = WallpaperEffects()

    // Parallax
    @Volatile private var normalOffsetX = 0.5f

    // Scaling
    @Volatile private var currentScalingType = ScalingType.FILL

    // Adaptive brightness (from ScheduleSettings)
    @Volatile private var adaptiveBrightnessEnabled = false

    // Matrices
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val identityMatrix = FloatArray(16)

    // Coroutine scope for background loading
    private val loadingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentLoadJob: Job? = null

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

        // Initialize identity matrix
        Matrix.setIdentityM(identityMatrix, 0)

        Log.d(TAG, "Surface created successfully")
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: ${width}x${height}")

        surfaceWidth = width
        surfaceHeight = height

        GLES20.glViewport(0, 0, width, height)

        // Recreate framebuffers for blur at new resolution
        createBlurFramebuffers(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        // Clear screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Prevent division by zero in shaders if surface dimensions are invalid
        if (surfaceWidth <= 0 || surfaceHeight <= 0) {
            return
        }

        val current = currentPicture
        val next = nextPicture

        if (current == null && next == null) {
            // No wallpaper to display
            return
        }

        // Determine if we need blur
        val blurRadius = if (currentEffects.enableBlur) {
            (currentEffects.blurPercentage / 100.0f) * Constants.MAX_BLUR_RADIUS
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
            // Safety: If crossfade just started but timer wasn't set, set it now
            if (crossfadeStartTimeNanos == 0L) {
                crossfadeStartTimeNanos = System.nanoTime()
            }

            // Draw next picture with its own alpha
            drawPictureWithEffects(picture, crossfadeProgress, blurRadius)

            // Update crossfade progress using time-based calculation
            // This ensures consistent animation duration regardless of refresh rate (60Hz, 90Hz, 120Hz, etc.)
            val currentTimeNanos = System.nanoTime()
            val elapsedMs = (currentTimeNanos - crossfadeStartTimeNanos) / 1_000_000f
            crossfadeProgress = (elapsedMs / CROSSFADE_DURATION_MS).coerceIn(0f, 1f)

            if (crossfadeProgress >= 1.0f) {
                // Crossfade complete
                currentPicture?.recycle()
                currentPicture = picture
                nextPicture = null
                crossfadeProgress = 0f
                crossfadeStartTimeNanos = 0L
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
        // Calculate MVP matrix for Center Crop + Parallax
        calculateMvpMatrix(picture, mvpMatrix)

        if (blurRadius > Constants.BLUR_MIN_THRESHOLD && currentEffects.enableBlur) {
            // Two-pass blur pipeline
            drawWithBlur(picture, alpha, blurRadius)
        } else {
            // No blur - direct render with color effects
            drawWithColorEffects(picture, alpha)
        }
    }

    /**
     * Draw picture with proper 3-pass blur + effects pipeline.
     * Pass 1: Source texture → Horizontal blur → FBO1
     * Pass 2: FBO1 texture → Vertical blur → FBO2
     * Pass 3: FBO2 texture → Color effects → Screen
     */
    private fun drawWithBlur(picture: GLPicture, alpha: Float, blurRadius: Float) {
        // Pass 1: Horizontal blur (source → FBO1)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, blurFbo1)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(blurHorizontalProgram)
        GLES20.glUniform2f(blurHResolutionHandle, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        GLES20.glUniform1f(blurHRadiusHandle, blurRadius)
        GLES20.glUniform1i(blurHTextureHandle, 0)  // Bind texture unit 0

        picture.draw(blurHorizontalProgram, blurHPositionHandle, blurHTexCoordHandle, mvpMatrix, blurHMvpMatrixHandle)

        // Pass 2: Vertical blur (FBO1 → FBO2)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, blurFbo2)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(blurVerticalProgram)
        GLES20.glUniform2f(blurVResolutionHandle, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        GLES20.glUniform1f(blurVRadiusHandle, blurRadius)

        // Bind horizontal blur result as input texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blurTexture1)
        GLES20.glUniform1i(blurVTextureHandle, 0)

        // Draw full-screen quad for vertical blur pass (Identity matrix)
        drawQuad(blurVPositionHandle, blurVTexCoordHandle, blurVMvpMatrixHandle, identityMatrix)

        // Pass 3: Color effects (FBO2 → Screen)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)

        GLES20.glUseProgram(effectsProgram)

        // Set color effect uniforms - respect enable flags
        GLES20.glUniform1f(uAlphaHandle, alpha)
        GLES20.glUniform1f(
            uDarkenFactorHandle,
            if (currentEffects.enableDarken) currentEffects.darkenPercentage / Constants.PERCENTAGE_DIVISOR else 0f
        )
        GLES20.glUniform1f(
            uVignetteFactorHandle,
            if (currentEffects.enableVignette) currentEffects.vignettePercentage / Constants.PERCENTAGE_DIVISOR else 0f
        )
        GLES20.glUniform1f(
            uGrayscaleFactorHandle,
            if (currentEffects.enableGrayscale) currentEffects.grayscalePercentage / Constants.PERCENTAGE_DIVISOR else 0f
        )
        GLES20.glUniform1f(uAdaptiveBrightnessFactorHandle, picture.brightnessFactor)

        // Bind the fully blurred texture as input
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blurTexture2)
        GLES20.glUniform1i(uTextureHandle, 0)

        drawQuad(aPositionHandle, aTexCoordHandle, uMvpMatrixHandle, identityMatrix)
    }

    /**
     * Draw picture with color effects only (no blur).
     */
    private fun drawWithColorEffects(picture: GLPicture, alpha: Float) {
        // Render to screen with effects
        GLES20.glUseProgram(effectsProgram)

        // Set uniforms - respect enable flags for all effects
        GLES20.glUniform1f(uAlphaHandle, alpha)
        GLES20.glUniform1f(
            uDarkenFactorHandle,
            if (currentEffects.enableDarken) currentEffects.darkenPercentage / Constants.PERCENTAGE_DIVISOR else 0f
        )
        GLES20.glUniform1f(
            uVignetteFactorHandle,
            if (currentEffects.enableVignette) currentEffects.vignettePercentage / Constants.PERCENTAGE_DIVISOR else 0f
        )
        GLES20.glUniform1f(
            uGrayscaleFactorHandle,
            if (currentEffects.enableGrayscale) currentEffects.grayscalePercentage / Constants.PERCENTAGE_DIVISOR else 0f
        )
        GLES20.glUniform1f(uAdaptiveBrightnessFactorHandle, picture.brightnessFactor)

        picture.draw(effectsProgram, aPositionHandle, aTexCoordHandle, mvpMatrix, uMvpMatrixHandle)
    }

    /**
     * Calculate MVP matrix for Center Crop scaling and Parallax.
     */
    private fun calculateMvpMatrix(picture: GLPicture, matrix: FloatArray) {
        val viewWidth = surfaceWidth.toFloat()
        val viewHeight = surfaceHeight.toFloat()
        val imageWidth = picture.width.toFloat()
        val imageHeight = picture.height.toFloat()

        if (viewWidth == 0f || viewHeight == 0f || imageWidth == 0f || imageHeight == 0f) {
            Matrix.setIdentityM(matrix, 0)
            return
        }

        // 1. Calculate scale based on ScalingType
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight

        val (finalScaleX, finalScaleY) = when (currentScalingType) {
            ScalingType.FILL -> {
                val scale = kotlin.math.max(scaleX, scaleY)
                Pair(scale, scale)
            }
            ScalingType.FIT -> {
                val scale = kotlin.math.min(scaleX, scaleY)
                Pair(scale, scale)
            }
            ScalingType.STRETCH -> {
                Pair(scaleX, scaleY)
            }
            ScalingType.NONE -> {
                Pair(1f, 1f)
            }
        }

        var effectiveScaleX = finalScaleX
        var effectiveScaleY = finalScaleY

        val parallaxEnabled = currentEffects.enableParallax && currentEffects.parallaxIntensity > 0
        val parallaxIntensity = if (parallaxEnabled) currentEffects.parallaxIntensity / 100f else 0f

        // If parallax is enabled, ensure we have enough width to scroll (overscan).
        // If image fits perfectly, apply artificial zoom based on intensity.
        if (parallaxEnabled) {
            val currentWidth = imageWidth * effectiveScaleX
            // Target at least 20% overscan at max intensity
            val minExtraWidth = viewWidth * parallaxIntensity * 0.2f
            
            if ((currentWidth - viewWidth) < minExtraWidth) {
                // Zoom in to create scrollable area
                val targetWidth = viewWidth + minExtraWidth
                // Prevent division by zero
                if (currentWidth > 0) {
                    val zoomFactor = targetWidth / currentWidth
                    effectiveScaleX *= zoomFactor
                    effectiveScaleY *= zoomFactor
                }
            }
        }

        val scaledWidth = imageWidth * effectiveScaleX
        val scaledHeight = imageHeight * effectiveScaleY

        // 2. Calculate parallax offset
        // Available scroll range is the difference between scaled image width and screen width
        val extraWidth = kotlin.math.max(0f, scaledWidth - viewWidth)
        
        // Calculate offset based on scroll position (0.0 = left, 1.0 = right)
        // Center (0.5) is 0 offset
        // Reverting to: `maxParallaxOffset = extraWidth`.
        // And relying on the "Zoom" logic to create that width if needed.
        val maxParallaxOffset = extraWidth
        val parallaxOffset = maxParallaxOffset * (0.5f - normalOffsetX)
        
        // Verbose logging removed to avoid per-frame log spam

        // 3. Construct Matrix
        // We use an orthographic projection matching the screen dimensions
        // Left: -width/2, Right: width/2, Bottom: -height/2, Top: height/2
        // This makes 0,0 the center of the screen
        Matrix.orthoM(projectionMatrix, 0, -viewWidth / 2f, viewWidth / 2f, -viewHeight / 2f, viewHeight / 2f, -1f, 1f)

        // Set view matrix (camera) - identity is fine for 2D
        Matrix.setIdentityM(viewMatrix, 0)

        // Combine Projection * View
        Matrix.multiplyMM(matrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Apply Model transformations
        // Translate for parallax
        Matrix.translateM(matrix, 0, parallaxOffset, 0f, 0f)
        
        // Scale to match image size * crop scale
        // The quad is -1 to 1 (size 2), so we need to scale it to match image dimensions
        // Actually, we want to map the quad (-1..1) to the image size (-w/2..w/2)
        Matrix.scaleM(matrix, 0, scaledWidth / 2f, scaledHeight / 2f, 1f)
    }

    /**
     * Draw a full-screen quad with current buffers.
     */
    private fun drawQuad(aPositionHandle: Int, aTexCoordHandle: Int, uMvpMatrixHandle: Int, mvpMatrix: FloatArray) {
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glEnableVertexAttribArray(aTexCoordHandle)

        GLES20.glUniformMatrix4fv(uMvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glVertexAttribPointer(aPositionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glVertexAttribPointer(aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTexCoordHandle)
    }

    /**
     * Compile all shader programs and cache uniform locations.
     */
    private fun compileShaders() {
        // Simple program
        simpleProgram = GLUtil.createProgram(GLShaders.VERTEX_SHADER, GLShaders.SIMPLE_FRAGMENT_SHADER)

        // Blur programs
        blurHorizontalProgram = GLUtil.createProgram(GLShaders.VERTEX_SHADER, GLShaders.BLUR_HORIZONTAL_FRAGMENT_SHADER)
        blurVerticalProgram = GLUtil.createProgram(GLShaders.VERTEX_SHADER, GLShaders.BLUR_VERTICAL_FRAGMENT_SHADER)

        // Effects program
        effectsProgram = GLUtil.createProgram(GLShaders.VERTEX_SHADER, GLShaders.EFFECTS_FRAGMENT_SHADER)

        // Cache uniform/attribute locations for effects program
        aPositionHandle = GLES20.glGetAttribLocation(effectsProgram, "a_position")
        aTexCoordHandle = GLES20.glGetAttribLocation(effectsProgram, "a_texCoord")
        uTextureHandle = GLES20.glGetUniformLocation(effectsProgram, "u_texture")
        uMvpMatrixHandle = GLES20.glGetUniformLocation(effectsProgram, "u_mvpMatrix")
        uAlphaHandle = GLES20.glGetUniformLocation(effectsProgram, "u_alpha")
        uDarkenFactorHandle = GLES20.glGetUniformLocation(effectsProgram, "u_darkenFactor")
        uVignetteFactorHandle = GLES20.glGetUniformLocation(effectsProgram, "u_vignetteFactor")
        uGrayscaleFactorHandle = GLES20.glGetUniformLocation(effectsProgram, "u_grayscaleFactor")
        uAdaptiveBrightnessFactorHandle = GLES20.glGetUniformLocation(effectsProgram, "u_adaptiveBrightnessFactor")

        Log.d(TAG, "Effects uniform locations: alpha=$uAlphaHandle, darken=$uDarkenFactorHandle, " +
                "vignette=$uVignetteFactorHandle, grayscale=$uGrayscaleFactorHandle, adaptiveBrightness=$uAdaptiveBrightnessFactorHandle")

        // Cache uniform/attribute locations for horizontal blur program
        blurHPositionHandle = GLES20.glGetAttribLocation(blurHorizontalProgram, "a_position")
        blurHTexCoordHandle = GLES20.glGetAttribLocation(blurHorizontalProgram, "a_texCoord")
        blurHTextureHandle = GLES20.glGetUniformLocation(blurHorizontalProgram, "u_texture")
        blurHMvpMatrixHandle = GLES20.glGetUniformLocation(blurHorizontalProgram, "u_mvpMatrix")
        blurHResolutionHandle = GLES20.glGetUniformLocation(blurHorizontalProgram, "u_resolution")
        blurHRadiusHandle = GLES20.glGetUniformLocation(blurHorizontalProgram, "u_blurRadius")

        // Cache uniform/attribute locations for vertical blur program
        blurVPositionHandle = GLES20.glGetAttribLocation(blurVerticalProgram, "a_position")
        blurVTexCoordHandle = GLES20.glGetAttribLocation(blurVerticalProgram, "a_texCoord")
        blurVTextureHandle = GLES20.glGetUniformLocation(blurVerticalProgram, "u_texture")
        blurVMvpMatrixHandle = GLES20.glGetUniformLocation(blurVerticalProgram, "u_mvpMatrix")
        blurVResolutionHandle = GLES20.glGetUniformLocation(blurVerticalProgram, "u_resolution")
        blurVRadiusHandle = GLES20.glGetUniformLocation(blurVerticalProgram, "u_blurRadius")

        Log.d(TAG, "Shaders compiled and uniforms cached")
    }

    /**
     * Create framebuffers for blur passes.
     * Two FBOs are needed for proper 3-pass blur + effects pipeline.
     */
    private fun createBlurFramebuffers(width: Int, height: Int) {
        // Delete old resources
        deleteBlurResources()

        // Create FBO1 (horizontal blur output)
        val (fbo1, tex1) = createFboWithTexture(width, height)
        blurFbo1 = fbo1
        blurTexture1 = tex1

        // Create FBO2 (vertical blur output)
        val (fbo2, tex2) = createFboWithTexture(width, height)
        blurFbo2 = fbo2
        blurTexture2 = tex2

        Log.d(TAG, "Blur framebuffers created: ${width}x${height}")
    }

    /**
     * Create an FBO with attached texture at specified dimensions.
     * @return Pair of (fboId, textureId)
     */
    private fun createFboWithTexture(width: Int, height: Int): Pair<Int, Int> {
        // Create texture
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
            width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )

        // Create framebuffer
        val fboIds = IntArray(1)
        GLES20.glGenFramebuffers(1, fboIds, 0)
        val fboId = fboIds[0]

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, textureId, 0
        )

        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer not complete: $status")
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        return Pair(fboId, textureId)
    }

    /**
     * Delete all blur-related GPU resources.
     */
    private fun deleteBlurResources() {
        if (blurFbo1 != 0) GLUtil.deleteFramebuffer(blurFbo1)
        if (blurFbo2 != 0) GLUtil.deleteFramebuffer(blurFbo2)
        if (blurTexture1 != 0) GLUtil.deleteTexture(blurTexture1)
        if (blurTexture2 != 0) GLUtil.deleteTexture(blurTexture2)

        blurFbo1 = 0
        blurFbo2 = 0
        blurTexture1 = 0
        blurTexture2 = 0
    }

    /**
     * Queue a new wallpaper for loading and display.
     * This method can be called from any thread.
     * Will wait for valid surface dimensions before loading.
     *
     * @param imageLoader Image loader to use
     * @param skipCrossfade If true, instantly swap wallpaper without crossfade animation
     */
    fun queueWallpaper(imageLoader: ImageLoader, skipCrossfade: Boolean = false) {
        // Cancel any existing loading job to prevent race conditions
        currentLoadJob?.cancel()

        currentLoadJob = loadingScope.launch {
            try {
                // Wait for valid surface dimensions (onSurfaceChanged may not have been called yet)
                val (width, height) = waitForSurfaceDimensions()

                Log.d(TAG, "Loading wallpaper... (skipCrossfade=$skipCrossfade)")
                val bitmap = imageLoader.load(width, height)

                if (bitmap != null) {
                    Log.d(TAG, "Wallpaper loaded: ${bitmap.width}x${bitmap.height}")

                    // Check for cancellation before uploading
                    if (!isActive) {
                        Log.d(TAG, "Loading cancelled, recycling bitmap")
                        bitmap.recycle()
                        return@launch
                    }

                    // Calculate adaptive brightness if enabled
                    val brightnessFactor = if (adaptiveBrightnessEnabled) {
                        val brightness = com.anthonyla.paperize.core.util.calculateBitmapBrightness(bitmap)
                        com.anthonyla.paperize.core.util.getAdaptiveBrightnessMultiplier(context, brightness)
                    } else {
                        1.0f
                    }

                    // Upload to GPU on GL thread
                    callbacks.queueEventOnGlThread {
                        uploadBitmap(bitmap, brightnessFactor, skipCrossfade)
                    }
                } else {
                    Log.w(TAG, "Failed to load wallpaper (null bitmap)")
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Wallpaper loading cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading wallpaper", e)
            }
        }
    }

    /**
     * Wait for valid surface dimensions.
     * Polls until surfaceWidth and surfaceHeight are non-zero or timeout.
     * Falls back to device display metrics if timeout occurs.
     *
     * @return Pair of (width, height)
     */
    private suspend fun waitForSurfaceDimensions(): Pair<Int, Int> {
        val maxWaitMs = Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS
        val pollIntervalMs = Constants.SURFACE_POLL_INTERVAL_MS
        var waitedMs = 0L

        while (surfaceWidth <= 0 || surfaceHeight <= 0) {
            if (waitedMs >= maxWaitMs) {
                // Use actual device display metrics as fallback instead of hardcoded values
                val displayMetrics = context.resources.displayMetrics
                val fallbackWidth = displayMetrics.widthPixels
                val fallbackHeight = displayMetrics.heightPixels
                Log.w(TAG, "Timeout waiting for surface dimensions, using display metrics: ${fallbackWidth}x${fallbackHeight}")
                return Pair(fallbackWidth, fallbackHeight)
            }
            kotlinx.coroutines.delay(pollIntervalMs)
            waitedMs += pollIntervalMs
        }

        return Pair(surfaceWidth, surfaceHeight)
    }

    /**
     * Upload a bitmap to GPU and start crossfade (or instant swap).
     * Must be called on GL thread.
     *
     * @param bitmap The bitmap to upload
     * @param brightnessFactor The adaptive brightness multiplier for this bitmap
     * @param skipCrossfade If true, instantly replace current wallpaper without animation
     */
    private fun uploadBitmap(bitmap: Bitmap, brightnessFactor: Float, skipCrossfade: Boolean = false) {
        // Validate bitmap before processing
        if (bitmap.isRecycled) {
            Log.e(TAG, "Cannot upload recycled bitmap")
            return
        }
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            Log.e(TAG, "Cannot upload bitmap with invalid dimensions: ${bitmap.width}x${bitmap.height}")
            bitmap.recycle()
            return
        }
        
        try {
            val picture = GLPicture(bitmap, brightnessFactor)

            // Recycle bitmap (no longer needed after GPU upload)
            bitmap.recycle()

            if (skipCrossfade) {
                // Instant swap - no animation (used for screen-off changes)
                currentPicture?.recycle()
                currentPicture = picture
                nextPicture = null
                crossfadeProgress = 0f
                crossfadeStartTimeNanos = 0L
                Log.d(TAG, "Wallpaper instantly swapped (no crossfade): $picture")
            } else {
                // Start crossfade with time-based animation
                nextPicture = picture
                crossfadeProgress = 0f
                crossfadeStartTimeNanos = System.nanoTime()
                Log.d(TAG, "Wallpaper uploaded to GPU with crossfade: $picture")
            }

            callbacks.requestRender()
        } catch (e: IllegalArgumentException) {
            // GLPicture validation failed (recycled bitmap or invalid dimensions)
            Log.e(TAG, "Failed to create GLPicture: ${e.message}")
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload bitmap to GPU", e)
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    /**
     * Update effects settings.
     * Can be called from any thread.
     */
    fun updateEffects(effects: WallpaperEffects) {
        this.currentEffects = effects
        Log.d(TAG, "Effects updated: blur=${effects.enableBlur}/${effects.blurPercentage}, " +
                "darken=${effects.enableDarken}/${effects.darkenPercentage}, " +
                "vignette=${effects.enableVignette}/${effects.vignettePercentage}, " +
                "grayscale=${effects.enableGrayscale}/${effects.grayscalePercentage}, " +
                "parallax=${effects.enableParallax}/${effects.parallaxIntensity}")
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
     * Update adaptive brightness setting.
     * Can be called from any thread.
     */
    fun updateAdaptiveBrightness(enabled: Boolean) {
        adaptiveBrightnessEnabled = enabled
        Log.d(TAG, "Adaptive brightness updated: $enabled")
    }

    /**
     * Update scaling type.
     * Can be called from any thread.
     */
    fun updateScalingType(scalingType: ScalingType) {
        if (currentScalingType != scalingType) {
            currentScalingType = scalingType
            Log.d(TAG, "Scaling type updated: $scalingType")
            callbacks.requestRender()
        }
    }

    /**
     * Cleanup resources.
     * Must be called on GL thread.
     */
    fun destroy() {
        loadingScope.cancel()
        currentLoadJob?.cancel()

        currentPicture?.recycle()
        currentPicture = null

        nextPicture?.recycle()
        nextPicture = null

        GLUtil.deleteProgram(simpleProgram)
        GLUtil.deleteProgram(blurHorizontalProgram)
        GLUtil.deleteProgram(blurVerticalProgram)
        GLUtil.deleteProgram(effectsProgram)

        deleteBlurResources()

        Log.d(TAG, "Renderer destroyed")
    }
}
