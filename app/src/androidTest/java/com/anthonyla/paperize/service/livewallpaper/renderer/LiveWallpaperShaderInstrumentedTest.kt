package com.anthonyla.paperize.service.livewallpaper.renderer

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anthonyla.paperize.service.livewallpaper.gl.GLUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveWallpaperShaderInstrumentedTest {
    private lateinit var display: EGLDisplay
    private lateinit var context: EGLContext
    private lateinit var surface: EGLSurface

    @Before
    fun createGlContext() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        assertTrue(display != EGL14.EGL_NO_DISPLAY)
        assertTrue(EGL14.eglInitialize(display, IntArray(2), 0, IntArray(2), 0))

        val configs = arrayOfNulls<EGLConfig>(1)
        val configCount = IntArray(1)
        assertTrue(
            EGL14.eglChooseConfig(
                display,
                intArrayOf(
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE
                ),
                0,
                configs,
                0,
                configs.size,
                configCount,
                0
            )
        )
        val config = requireNotNull(configs.first())
        context = EGL14.eglCreateContext(
            display,
            config,
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
            0
        )
        surface = EGL14.eglCreatePbufferSurface(
            display,
            config,
            intArrayOf(EGL14.EGL_WIDTH, SIZE, EGL14.EGL_HEIGHT, SIZE, EGL14.EGL_NONE),
            0
        )
        assertTrue(EGL14.eglMakeCurrent(display, surface, surface, context))
        GLES20.glViewport(0, 0, SIZE, SIZE)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    @After
    fun destroyGlContext() {
        EGL14.eglMakeCurrent(
            display,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        EGL14.eglDestroySurface(display, surface)
        EGL14.eglDestroyContext(display, context)
        EGL14.eglTerminate(display)
    }

    @Test
    fun everyLiveWallpaperShaderCompilesAndLinksOnDevice() {
        listOf(
            GLShaders.SIMPLE_FRAGMENT_SHADER,
            GLShaders.BLUR_HORIZONTAL_FRAGMENT_SHADER,
            GLShaders.BLUR_VERTICAL_FRAGMENT_SHADER,
            GLShaders.EFFECTS_FRAGMENT_SHADER
        ).forEach { fragmentShader ->
            val program = GLUtil.createProgram(GLShaders.VERTEX_SHADER, fragmentShader)
            assertTrue(program != 0)
            GLES20.glDeleteProgram(program)
        }
    }

    @Test
    fun liveColorEffectsChangeRenderedPixels() {
        val sourceColor = Color.rgb(200, 100, 40)
        val texture = createTexture(solidBitmap(sourceColor))
        val program = GLUtil.createProgram(GLShaders.VERTEX_SHADER, GLShaders.EFFECTS_FRAGMENT_SHADER)

        try {
            drawEffects(program, texture)
            val baseline = readPixel(SIZE / 2, SIZE / 2)
            assertTrue(Color.red(baseline) in 195..205)
            assertTrue(Color.green(baseline) in 95..105)

            drawEffects(program, texture, darken = 1f)
            assertTrue(Color.red(readPixel(SIZE / 2, SIZE / 2)) <= 2)

            drawEffects(program, texture, grayscale = 1f)
            val gray = readPixel(SIZE / 2, SIZE / 2)
            assertTrue(kotlin.math.abs(Color.red(gray) - Color.green(gray)) <= 2)
            assertTrue(kotlin.math.abs(Color.green(gray) - Color.blue(gray)) <= 2)

            drawEffects(program, texture, adaptiveBrightness = 0.5f)
            val dimmed = readPixel(SIZE / 2, SIZE / 2)
            assertTrue(Color.red(dimmed) in 95..105)

            drawEffects(program, texture, vignette = 1f)
            val center = Color.red(readPixel(SIZE / 2, SIZE / 2))
            val corner = Color.red(readPixel(1, 1))
            assertTrue("Expected vignette corner $corner below center $center", corner < center)
        } finally {
            GLES20.glDeleteProgram(program)
            GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
        }
    }

    @Test
    fun liveBlurShadersSoftenHorizontalAndVerticalBoundaries() {
        val horizontal = boundaryBitmap(horizontalBoundary = true)
        val vertical = boundaryBitmap(horizontalBoundary = false)
        val horizontalTexture = createTexture(horizontal)
        val verticalTexture = createTexture(vertical)

        val horizontalProgram = GLUtil.createProgram(
            GLShaders.VERTEX_SHADER,
            GLShaders.BLUR_HORIZONTAL_FRAGMENT_SHADER
        )
        val verticalProgram = GLUtil.createProgram(
            GLShaders.VERTEX_SHADER,
            GLShaders.BLUR_VERTICAL_FRAGMENT_SHADER
        )
        try {
            drawBlur(horizontalProgram, horizontalTexture)
            assertTrue(Color.red(readPixel(SIZE / 2, SIZE / 2)) in 10..245)

            drawBlur(verticalProgram, verticalTexture)
            assertTrue(Color.red(readPixel(SIZE / 2, SIZE / 2)) in 10..245)
        } finally {
            GLES20.glDeleteProgram(horizontalProgram)
            GLES20.glDeleteProgram(verticalProgram)
            GLES20.glDeleteTextures(2, intArrayOf(horizontalTexture, verticalTexture), 0)
        }
    }

    private fun drawEffects(
        program: Int,
        texture: Int,
        darken: Float = 0f,
        vignette: Float = 0f,
        grayscale: Float = 0f,
        adaptiveBrightness: Float = 1f
    ) {
        prepareDraw(program, texture)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "u_alpha"), 1f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "u_darkenFactor"), darken)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "u_vignetteFactor"), vignette)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "u_grayscaleFactor"), grayscale)
        GLES20.glUniform1f(
            GLES20.glGetUniformLocation(program, "u_adaptiveBrightnessFactor"),
            adaptiveBrightness
        )
        finishDraw(program)
    }

    private fun drawBlur(program: Int, texture: Int) {
        prepareDraw(program, texture)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "u_resolution"), SIZE.toFloat(), SIZE.toFloat())
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "u_blurRadius"), 4f)
        finishDraw(program)
    }

    private fun prepareDraw(program: Int, texture: Int) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "u_texture"), 0)

        val identity = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
        GLES20.glUniformMatrix4fv(
            GLES20.glGetUniformLocation(program, "u_mvpMatrix"),
            1,
            false,
            identity,
            0
        )

        val position = GLES20.glGetAttribLocation(program, "a_position")
        val textureCoordinate = GLES20.glGetAttribLocation(program, "a_texCoord")
        GLES20.glEnableVertexAttribArray(position)
        GLES20.glEnableVertexAttribArray(textureCoordinate)
        GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, floatBuffer(VERTICES))
        GLES20.glVertexAttribPointer(
            textureCoordinate,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            floatBuffer(TEXTURE_COORDINATES)
        )
    }

    private fun finishDraw(program: Int) {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glFinish()
        GLES20.glDisableVertexAttribArray(GLES20.glGetAttribLocation(program, "a_position"))
        GLES20.glDisableVertexAttribArray(GLES20.glGetAttribLocation(program, "a_texCoord"))
        GLUtil.checkGLError("live wallpaper shader draw")
    }

    private fun createTexture(bitmap: Bitmap): Int {
        val texture = IntArray(1)
        GLES20.glGenTextures(1, texture, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        if (!bitmap.isRecycled) bitmap.recycle()
        return texture[0]
    }

    private fun readPixel(x: Int, y: Int): Int {
        val buffer = ByteBuffer.allocateDirect(4)
        GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
        return Color.argb(
            buffer.get(3).toInt() and 0xff,
            buffer.get(0).toInt() and 0xff,
            buffer.get(1).toInt() and 0xff,
            buffer.get(2).toInt() and 0xff
        )
    }

    private fun solidBitmap(color: Int): Bitmap =
        Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    private fun boundaryBitmap(horizontalBoundary: Boolean): Bitmap =
        Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until SIZE) {
                for (x in 0 until SIZE) {
                    val white = if (horizontalBoundary) x >= SIZE / 2 else y >= SIZE / 2
                    setPixel(x, y, if (white) Color.WHITE else Color.BLACK)
                }
            }
        }

    private fun floatBuffer(values: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(values.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(values)
                position(0)
            }

    private companion object {
        const val SIZE = 64
        val VERTICES = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        val TEXTURE_COORDINATES = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)
    }
}
