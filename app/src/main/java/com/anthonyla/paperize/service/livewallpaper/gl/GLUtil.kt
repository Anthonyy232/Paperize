package com.anthonyla.paperize.service.livewallpaper.gl

import android.opengl.GLES20
import android.util.Log

/**
 * Utilities for OpenGL ES operations including shader compilation,
 * program linking, and error checking.
 */
object GLUtil {

    private const val TAG = "GLUtil"

    /**
     * Compile a shader from source code.
     *
     * @param type Shader type (GLES20.GL_VERTEX_SHADER or GLES20.GL_FRAGMENT_SHADER)
     * @param source GLSL source code
     * @return Shader handle
     * @throws RuntimeException if compilation fails
     */
    fun compileShader(type: Int, source: String): Int {
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

    /**
     * Create a program from vertex and fragment shaders.
     *
     * @param vertexShader Compiled vertex shader handle
     * @param fragmentShader Compiled fragment shader handle
     * @return Program handle
     * @throws RuntimeException if linking fails
     */
    fun createProgram(vertexShader: Int, fragmentShader: Int): Int {
        val program = GLES20.glCreateProgram()
        if (program == 0) {
            throw RuntimeException("Failed to create program")
        }

        GLES20.glAttachShader(program, vertexShader)
        checkGLError("glAttachShader (vertex)")

        GLES20.glAttachShader(program, fragmentShader)
        checkGLError("glAttachShader (fragment)")

        GLES20.glLinkProgram(program)

        val linked = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)

        if (linked[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Program linking failed:\n$log")
        }

        return program
    }

    /**
     * Compile shaders and create a program in one call.
     *
     * @param vertexSource Vertex shader GLSL source
     * @param fragmentSource Fragment shader GLSL source
     * @return Program handle
     * @throws RuntimeException if compilation or linking fails
     */
    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)

        val program = createProgram(vertexShader, fragmentShader)

        // Shaders can be deleted after linking
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        return program
    }

    /**
     * Check for OpenGL errors and throw if one occurred.
     *
     * @param operation Description of the operation for error message
     * @throws RuntimeException if GL error occurred
     */
    fun checkGLError(operation: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            val errorMsg = "GL error after $operation: 0x${Integer.toHexString(error)}"
            Log.e(TAG, errorMsg)
            throw RuntimeException(errorMsg)
        }
    }

    /**
     * Get the maximum texture size supported by the GPU.
     *
     * @return Maximum texture dimension in pixels
     */
    fun getMaxTextureSize(): Int {
        val maxSize = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxSize, 0)
        return maxSize[0]
    }

    /**
     * Delete a shader program and check for errors.
     *
     * @param program Program handle to delete
     */
    fun deleteProgram(program: Int) {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            checkGLError("glDeleteProgram")
        }
    }

    /**
     * Delete a texture and check for errors.
     *
     * @param texture Texture handle to delete
     */
    fun deleteTexture(texture: Int) {
        if (texture != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
            checkGLError("glDeleteTextures")
        }
    }

    /**
     * Delete a framebuffer and check for errors.
     *
     * @param framebuffer Framebuffer handle to delete
     */
    fun deleteFramebuffer(framebuffer: Int) {
        if (framebuffer != 0) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
            checkGLError("glDeleteFramebuffers")
        }
    }
}
