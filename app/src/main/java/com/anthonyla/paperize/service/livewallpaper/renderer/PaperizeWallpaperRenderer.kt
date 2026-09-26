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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicLong
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PaperizeWallpaperRenderer(
    private val context: Context,
    private val callbacks: Callbacks
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "PaperizeRenderer"
        private const val CROSSFADE_DURATION_MS = Constants.CROSSFADE_DURATION_MS
    }

    interface Callbacks {
        fun queueEventOnGlThread(event: () -> Unit): Boolean
        fun requestRender()
    }

    private val surfaceSize = MutableStateFlow<Pair<Int, Int>?>(null)
    private val surfaceWidth get() = surfaceSize.value?.first ?: 0
    private val surfaceHeight get() = surfaceSize.value?.second ?: 0

    private var blurProgram = 0
    private var effectsProgram = 0

    private var aPositionHandle = 0
    private var aTexCoordHandle = 0
    private var uTextureHandle = 0
    private var uMvpMatrixHandle = 0
    private var uAlphaHandle = 0
    private var uDarkenFactorHandle = 0
    private var uVignetteFactorHandle = 0
    private var uGrayscaleFactorHandle = 0
    private var uAdaptiveBrightnessFactorHandle = 0

    private var blurPositionHandle = 0
    private var blurTexCoordHandle = 0
    private var blurMvpMatrixHandle = 0
    private var blurTextureHandle = 0
    private var blurResolutionHandle = 0
    private var blurRadiusHandle = 0
    private var blurDirectionHandle = 0

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

    private var blurFbo1 = 0
    private var blurTexture1 = 0
    private var blurFbo2 = 0
    private var blurTexture2 = 0

    private var currentPicture: GLPicture? = null

    private var nextPicture: GLPicture? = null
    private var crossfadeStartTimeNanos = 0L

    @Volatile private var currentEffects = WallpaperEffects()

    @Volatile private var normalOffsetX = 0.5f

    @Volatile private var currentScalingType = ScalingType.FILL

    @Volatile private var adaptiveBrightnessEnabled = false

    // Retain the latest request, including images still decoding or waiting for upload,
    // so a fold/unfold cannot replace it with the previously displayed wallpaper.
    private var requestedImageLoader: ImageLoader? = null

    private val mvpMatrix = FloatArray(16)
    private val identityMatrix = FloatArray(16)

    private val loadingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentLoadJob: Job? = null
    private val loadGeneration = AtomicLong()

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        Log.d(TAG, "onSurfaceCreated")

        // A recreated surface has a new EGL context; old handles no longer belong to it.
        surfaceSize.value = null
        currentPicture = null
        nextPicture = null
        blurFbo1 = 0
        blurFbo2 = 0
        blurTexture1 = 0
        blurTexture2 = 0

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        compileShaders()

        vertexBuffer = GLGeometry.createFloatBuffer(GLGeometry.VERTICES)
        texCoordBuffer = GLGeometry.createFloatBuffer(GLGeometry.TEX_COORDS)

        Matrix.setIdentityM(identityMatrix, 0)

        Log.d(TAG, "Surface created successfully")
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: ${width}x${height}")

        val sizeChanged =
            surfaceWidth > 0 &&
                surfaceHeight > 0 &&
                (surfaceWidth != width || surfaceHeight != height)
        surfaceSize.value = width to height

        GLES20.glViewport(0, 0, width, height)

        deleteBlurResources()

        if (sizeChanged || currentPicture == null) {
            synchronized(this) {
                requestedImageLoader?.let { loader ->
                    queueWallpaper(loader, skipCrossfade = true)
                }
            }
        }
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (surfaceWidth <= 0 || surfaceHeight <= 0) {
            return
        }

        val current = currentPicture
        val next = nextPicture

        if (current == null && next == null) {
            return
        }

        val blurRadius = if (currentEffects.enableBlur) {
            (currentEffects.blurPercentage / 100.0f) * Constants.MAX_BLUR_RADIUS
        } else {
            0f
        }

        val crossfadeProgress = if (next != null) {
            val elapsedMs = (System.nanoTime() - crossfadeStartTimeNanos) / 1_000_000f
            (elapsedMs / CROSSFADE_DURATION_MS).coerceIn(0f, 1f)
        } else {
            0f
        }

        val crossfadeAlphas = GLGeometry.calculateCrossfadeAlphas(
            progress = crossfadeProgress,
            hasNextPicture = next != null
        )

        current?.let { picture ->
            drawPictureWithEffects(picture, crossfadeAlphas.current, blurRadius)
        }

        next?.let { picture ->
            drawPictureWithEffects(picture, crossfadeAlphas.next, blurRadius)

            if (crossfadeProgress >= 1.0f) {
                currentPicture?.recycle()
                currentPicture = picture
                nextPicture = null

                crossfadeStartTimeNanos = 0L
                Log.d(TAG, "Crossfade complete")
            } else {
                callbacks.requestRender()
            }
        }
    }

    private fun drawPictureWithEffects(picture: GLPicture, alpha: Float, blurRadius: Float) {
        calculateMvpMatrix(picture, mvpMatrix)

        if (blurRadius > Constants.BLUR_MIN_THRESHOLD) {
            drawWithBlur(picture, alpha, blurRadius)
        } else {
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
        if (blurFbo1 == 0) createBlurFramebuffers(surfaceWidth, surfaceHeight)

        // Pass 1: Horizontal blur (source → FBO1)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, blurFbo1)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(blurProgram)
        GLES20.glUniform2f(blurDirectionHandle, 1f, 0f)
        GLES20.glUniform2f(blurResolutionHandle, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        GLES20.glUniform1f(blurRadiusHandle, blurRadius)
        GLES20.glUniform1i(blurTextureHandle, 0)  // Bind texture unit 0

        picture.draw(blurProgram, blurPositionHandle, blurTexCoordHandle, mvpMatrix, blurMvpMatrixHandle)

        // Pass 2: Vertical blur (FBO1 → FBO2)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, blurFbo2)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blurTexture1)

        GLES20.glUniform2f(blurDirectionHandle, 0f, 1f)
        drawQuad(blurPositionHandle, blurTexCoordHandle, blurMvpMatrixHandle, identityMatrix)

        // Pass 3: Color effects (FBO2 → Screen)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)

        setEffectUniforms(picture, alpha)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blurTexture2)
        GLES20.glUniform1i(uTextureHandle, 0)

        drawQuad(aPositionHandle, aTexCoordHandle, uMvpMatrixHandle, identityMatrix)
    }

    private fun drawWithColorEffects(picture: GLPicture, alpha: Float) {
        setEffectUniforms(picture, alpha)
        picture.draw(effectsProgram, aPositionHandle, aTexCoordHandle, mvpMatrix, uMvpMatrixHandle)
    }

    private fun setEffectUniforms(picture: GLPicture, alpha: Float) {
        GLES20.glUseProgram(effectsProgram)

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
        GLES20.glUniform1f(uAdaptiveBrightnessFactorHandle, adaptiveBrightnessFactor(picture))

    }

    private fun calculateMvpMatrix(picture: GLPicture, matrix: FloatArray) {
        val viewWidth = surfaceWidth.toFloat()
        val viewHeight = surfaceHeight.toFloat()
        val imageWidth = picture.width.toFloat()
        val imageHeight = picture.height.toFloat()

        val transform = GLGeometry.calculateWallpaperTransform(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            scalingType = currentScalingType,
            parallaxEnabled = currentEffects.enableParallax,
            parallaxIntensity = currentEffects.parallaxIntensity,
            normalizedOffsetX = normalOffsetX
        )

        Matrix.orthoM(matrix, 0, -viewWidth / 2f, viewWidth / 2f, -viewHeight / 2f, viewHeight / 2f, -1f, 1f)
        Matrix.translateM(matrix, 0, transform.horizontalOffset, 0f, 0f)
        // The quad spans -1..1, so scale by half the displayed image dimensions.
        Matrix.scaleM(
            matrix,
            0,
            transform.scaledWidth / 2f,
            transform.scaledHeight / 2f,
            1f
        )
    }

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

    private fun compileShaders() {
        blurProgram = GLUtil.createProgram(GLShaders.VERTEX_SHADER, GLShaders.BLUR_FRAGMENT_SHADER)

        effectsProgram = GLUtil.createProgram(GLShaders.VERTEX_SHADER, GLShaders.EFFECTS_FRAGMENT_SHADER)

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

        blurPositionHandle = GLES20.glGetAttribLocation(blurProgram, "a_position")
        blurTexCoordHandle = GLES20.glGetAttribLocation(blurProgram, "a_texCoord")
        blurTextureHandle = GLES20.glGetUniformLocation(blurProgram, "u_texture")
        blurMvpMatrixHandle = GLES20.glGetUniformLocation(blurProgram, "u_mvpMatrix")
        blurResolutionHandle = GLES20.glGetUniformLocation(blurProgram, "u_resolution")
        blurRadiusHandle = GLES20.glGetUniformLocation(blurProgram, "u_blurRadius")
        blurDirectionHandle = GLES20.glGetUniformLocation(blurProgram, "u_direction")

        Log.d(TAG, "Shaders compiled and uniforms cached")
    }

    private fun createBlurFramebuffers(width: Int, height: Int) {
        deleteBlurResources()

        try {
            val (fbo1, tex1) = createFboWithTexture(width, height)
            blurFbo1 = fbo1
            blurTexture1 = tex1

            val (fbo2, tex2) = createFboWithTexture(width, height)
            blurFbo2 = fbo2
            blurTexture2 = tex2
        } catch (e: Throwable) {
            deleteBlurResources()
            throw e
        }

        Log.d(TAG, "Blur framebuffers created: ${width}x${height}")
    }

    /**
     * Create an FBO with attached texture at specified dimensions.
     * @return Pair of (fboId, textureId)
     */
    private fun createFboWithTexture(width: Int, height: Int): Pair<Int, Int> {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]

        val fboIds = IntArray(1)
        try {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
            )

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

            return Pair(fboId, textureId)
        } catch (e: Throwable) {
            GLES20.glDeleteFramebuffers(1, fboIds, 0)
            GLES20.glDeleteTextures(1, textureIds, 0)
            throw e
        } finally {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        }
    }

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

    /** Thread-safe request; waits for a surface and decodes off the GL thread. */
    @Synchronized
    fun queueWallpaper(imageLoader: ImageLoader, skipCrossfade: Boolean = false) {
        requestedImageLoader = imageLoader
        val generation = loadGeneration.incrementAndGet()
        currentLoadJob?.cancel()

        currentLoadJob = loadingScope.launch {
            try {
                val (width, height) = surfaceSize.filterNotNull().first()

                Log.d(TAG, "Loading wallpaper... (skipCrossfade=$skipCrossfade)")
                val bitmap = imageLoader.load(width, height)

                if (bitmap != null) {
                    Log.d(TAG, "Wallpaper loaded: ${bitmap.width}x${bitmap.height}")

                    if (!isActive) {
                        Log.d(TAG, "Loading cancelled, recycling bitmap")
                        bitmap.recycle()
                        return@launch
                    }

                    // Always retain source luminance. Whether adaptive brightness is enabled
                    // is evaluated at draw time so settings changes are immediate and do not
                    // advance the wallpaper queue.
                    val sourceBrightness =
                        com.anthonyla.paperize.core.util.BrightnessCalculator.calculateBitmapBrightness(bitmap)

                    val queued = callbacks.queueEventOnGlThread {
                        if (loadGeneration.get() == generation) {
                            uploadBitmap(bitmap, sourceBrightness, skipCrossfade)
                        } else {
                            bitmap.recycle()
                        }
                    }
                    if (!queued) bitmap.recycle()
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

    /** Consumes [bitmap] on the GL thread, then swaps or crossfades to it. */
    private fun uploadBitmap(
        bitmap: Bitmap,
        sourceBrightness: Float,
        skipCrossfade: Boolean = false
    ) {
        try {
            val picture = GLPicture(bitmap, sourceBrightness)

            if (skipCrossfade) {
                currentPicture?.recycle()
                nextPicture?.recycle() // Recycle any in-flight crossfade target
                currentPicture = picture
                nextPicture = null

                crossfadeStartTimeNanos = 0L
                Log.d(TAG, "Wallpaper instantly swapped (no crossfade): $picture")
            } else {
                nextPicture?.recycle() // Recycle interrupted crossfade target if any
                nextPicture = picture

                crossfadeStartTimeNanos = System.nanoTime()
                Log.d(TAG, "Wallpaper uploaded to GPU with crossfade: $picture")
            }

            callbacks.requestRender()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload bitmap to GPU", e)
        } finally {
            bitmap.recycle()
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
        val clampedOffset = offset.coerceIn(0f, 1f)
        if (normalOffsetX != clampedOffset) {
            normalOffsetX = clampedOffset
            callbacks.requestRender()
        }
    }

    /**
     * Update adaptive brightness setting.
     * Can be called from any thread.
     */
    fun updateAdaptiveBrightness(enabled: Boolean) {
        if (adaptiveBrightnessEnabled != enabled) {
            adaptiveBrightnessEnabled = enabled
            Log.d(TAG, "Adaptive brightness updated: $enabled")
            callbacks.requestRender()
        }
    }

    private fun adaptiveBrightnessFactor(picture: GLPicture): Float =
        if (adaptiveBrightnessEnabled) {
            com.anthonyla.paperize.core.util.getAdaptiveBrightnessMultiplier(
                context,
                picture.sourceBrightness
            )
        } else {
            1f
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
    @Synchronized
    fun cancelLoading() {
        loadGeneration.incrementAndGet()
        loadingScope.cancel()
        requestedImageLoader = null
    }

    fun destroy() {
        cancelLoading()

        currentPicture?.recycle()
        currentPicture = null

        nextPicture?.recycle()
        nextPicture = null

        GLUtil.deleteProgram(blurProgram)
        GLUtil.deleteProgram(effectsProgram)

        deleteBlurResources()

        Log.d(TAG, "Renderer destroyed")
    }
}
