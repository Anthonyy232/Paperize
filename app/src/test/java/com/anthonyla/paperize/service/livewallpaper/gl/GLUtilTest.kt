package com.anthonyla.paperize.service.livewallpaper.gl

import android.opengl.GLES20
import android.util.Log
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class GLUtilTest {
    @Before fun setUp() {
        mockkStatic(GLES20::class, Log::class)
        every { Log.e(any(), any()) } returns 0
        every { GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER) } returns 11
        every { GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER) } returns 12
        every { GLES20.glShaderSource(any(), any()) } just Runs
        every { GLES20.glCompileShader(any()) } just Runs
        every { GLES20.glGetShaderiv(any(), any(), any(), any()) } answers {
            thirdArg<IntArray>()[0] = 1
        }
        every { GLES20.glDeleteShader(any()) } just Runs
        every { GLES20.glCreateProgram() } returns 13
        every { GLES20.glAttachShader(any(), any()) } just Runs
        every { GLES20.glLinkProgram(any()) } just Runs
        every { GLES20.glGetError() } returns GLES20.GL_NO_ERROR
        every { GLES20.glDeleteProgram(any()) } just Runs
    }

    @After fun tearDown() = unmockkAll()

    @Test fun `fragment compilation failure releases both shaders`() {
        every { GLES20.glGetShaderiv(12, GLES20.GL_COMPILE_STATUS, any(), 0) } answers {
            thirdArg<IntArray>()[0] = 0
        }
        every { GLES20.glGetShaderInfoLog(12) } returns "Invalid shader"

        assertThrows(RuntimeException::class.java) { GLUtil.createProgram("vertex", "fragment") }

        verify(exactly = 1) { GLES20.glDeleteShader(11); GLES20.glDeleteShader(12) }
        verify(exactly = 0) { GLES20.glCreateProgram() }
    }

    @Test fun `link failure releases the program and both shaders`() {
        every { GLES20.glGetProgramiv(13, GLES20.GL_LINK_STATUS, any(), 0) } answers {
            thirdArg<IntArray>()[0] = 0
        }
        every { GLES20.glGetProgramInfoLog(13) } returns "Incompatible shaders"

        assertThrows(RuntimeException::class.java) { GLUtil.createProgram("vertex", "fragment") }

        verify(exactly = 1) {
            GLES20.glDeleteProgram(13)
            GLES20.glDeleteShader(11)
            GLES20.glDeleteShader(12)
        }
    }

    @Test fun `attach failure releases the program before linking`() {
        every { GLES20.glGetError() } returns GLES20.GL_INVALID_OPERATION

        assertThrows(RuntimeException::class.java) { GLUtil.createProgram("vertex", "fragment") }

        verify(exactly = 1) {
            GLES20.glDeleteProgram(13)
            GLES20.glDeleteShader(11)
            GLES20.glDeleteShader(12)
        }
        verify(exactly = 0) { GLES20.glLinkProgram(any()) }
    }
}
