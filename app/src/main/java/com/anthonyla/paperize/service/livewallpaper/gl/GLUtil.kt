package com.anthonyla.paperize.service.livewallpaper.gl

import android.opengl.GLES20
import android.util.Log

object GLUtil {

    private const val TAG = "GLUtil"

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            throw RuntimeException("Failed to create shader")
        }

        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)

        if (compiled[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            val shaderType = if (type == GLES20.GL_VERTEX_SHADER) "vertex" else "fragment"
            throw RuntimeException("Shader compilation failed ($shaderType):\n$log")
        }

        return shader
    }

    private fun createProgram(vertexShader: Int, fragmentShader: Int): Int {
        val program = GLES20.glCreateProgram()
        if (program == 0) {
            throw RuntimeException("Failed to create program")
        }

        try {
            GLES20.glAttachShader(program, vertexShader)
            checkGLError("glAttachShader (vertex)")

            GLES20.glAttachShader(program, fragmentShader)
            checkGLError("glAttachShader (fragment)")

            GLES20.glLinkProgram(program)

            val linked = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)

            if (linked[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(program)
                throw RuntimeException("Program linking failed:\n$log")
            }

            return program
        } catch (e: Throwable) {
            GLES20.glDeleteProgram(program)
            throw e
        }
    }

    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        try {
            val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            try {
                return createProgram(vertexShader, fragmentShader)
            } finally {
                GLES20.glDeleteShader(fragmentShader)
            }
        } finally {
            GLES20.glDeleteShader(vertexShader)
        }
    }

    fun checkGLError(operation: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            val errorMsg = "GL error after $operation: 0x${Integer.toHexString(error)}"
            Log.e(TAG, errorMsg)
            throw RuntimeException(errorMsg)
        }
    }

    fun getMaxTextureSize(): Int {
        val maxSize = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxSize, 0)
        return maxSize[0]
    }

    fun deleteProgram(program: Int) {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            checkGLError("glDeleteProgram")
        }
    }

    fun deleteTexture(texture: Int) {
        if (texture != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
            checkGLError("glDeleteTextures")
        }
    }

    fun deleteFramebuffer(framebuffer: Int) {
        if (framebuffer != 0) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
            checkGLError("glDeleteFramebuffers")
        }
    }
}
