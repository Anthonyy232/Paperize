package com.anthonyla.paperize.service.livewallpaper.gl

import android.opengl.EGL14
import android.opengl.GLSurfaceView
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import com.anthonyla.paperize.core.constants.Constants
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL10

/**
 * Base class for OpenGL ES 2.0 based wallpaper services.
 * Manages EGL context, rendering thread, and surface lifecycle.
 * Based on GLSurfaceView but adapted for WallpaperService.Engine.
 */
abstract class GLWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "GLWallpaperService"
    }

    abstract inner class GLEngine : Engine() {

        @Volatile private var glThread: GLThread? = null
        private var renderer: GLSurfaceView.Renderer? = null
        /**
         * Set the renderer for this engine.
         * Must be called before surface is created.
         */
        fun setRenderer(renderer: GLSurfaceView.Renderer) {
            this.renderer = renderer
        }

        fun requestRender() {
            glThread?.requestRender()
        }

        fun queueEvent(runnable: Runnable): Boolean = glThread?.queueEvent(runnable) ?: false

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)

            glThread = GLThread(
                holder = holder,
                renderer = checkNotNull(renderer) { "Renderer not set" }
            ).apply { start() }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            glThread?.onWindowResize(width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            glThread?.requestExitAndWait()
            glThread = null
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                glThread?.onResume()
            } else {
                glThread?.onPause()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            glThread?.requestExitAndWait()
            glThread = null
        }
    }

    private class GLThread(
        private val holder: SurfaceHolder,
        private val renderer: GLSurfaceView.Renderer
    ) : Thread("GLThread") {

        private val eventQueue = ArrayDeque<Runnable>()
        // java.lang.Object is intentional: this monitor uses wait()/notifyAll(), which
        // Kotlin's Any does not expose directly.
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        private val lock = Object()

        private var shouldExit = false
        private var paused = false
        private var width = 0
        private var height = 0
        private var requestRender = true
        private var sizeChanged = true

        private var egl: EGL10? = null
        private var eglDisplay: EGLDisplay? = null
        private var eglSurface: EGLSurface? = null
        private var eglContext: EGLContext? = null
        private var gl: GL10? = null

        fun requestRender() {
            synchronized(lock) {
                requestRender = true
                lock.notifyAll()
            }
        }

        fun queueEvent(runnable: Runnable): Boolean = synchronized(lock) {
            if (shouldExit) return false
            eventQueue.add(runnable)
            requestRender = true
            lock.notifyAll()
            true
        }

        fun onWindowResize(width: Int, height: Int) {
            synchronized(lock) {
                this.width = width
                this.height = height
                sizeChanged = true
                requestRender = true
                lock.notifyAll()
            }
        }

        fun onPause() {
            synchronized(lock) {
                paused = true
                lock.notifyAll()
            }
        }

        fun onResume() {
            synchronized(lock) {
                paused = false
                requestRender = true
                lock.notifyAll()
            }
        }

        fun requestExitAndWait() {
            synchronized(lock) {
                shouldExit = true
                lock.notifyAll()
            }
            try {
                join()
            } catch (e: InterruptedException) {
                Log.w(TAG, "Thread join interrupted", e)
            }
        }

        override fun run() {
            try {
                initGL()

                while (true) {
                    var event: Runnable? = null
                    var newSize: Pair<Int, Int>? = null

                    synchronized(lock) {
                        while (true) {
                            val polled = eventQueue.removeFirstOrNull()
                            if (polled != null) {
                                event = polled
                                break
                            }

                            // Drain accepted events (including renderer cleanup) before releasing EGL.
                            if (shouldExit) return

                            if (!paused && width > 0 && height > 0 && requestRender) {
                                requestRender = false
                                if (sizeChanged) {
                                    newSize = width to height
                                    sizeChanged = false
                                }
                                break
                            }

                            lock.wait()
                        }
                    }

                    event?.let {
                        it.run()
                        continue
                    }

                    newSize?.let { (width, height) ->
                        gl?.let { renderer.onSurfaceChanged(it, width, height) }
                    }

                    gl?.let { renderer.onDrawFrame(it) }

                    egl?.let { eglInstance ->
                        if (!eglInstance.eglSwapBuffers(eglDisplay, eglSurface)) {
                            Log.w(TAG, "eglSwapBuffers failed")
                        }
                    }

                }
            } catch (e: Exception) {
                Log.e(TAG, "GL thread error", e)
            } finally {
                cleanupGL()
            }
        }

        private fun initGL() {
            egl = EGLContext.getEGL() as EGL10
            eglDisplay = egl?.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

            if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw RuntimeException("eglGetDisplay failed")
            }

            val version = IntArray(2)
            if (egl?.eglInitialize(eglDisplay, version) == false) {
                throw RuntimeException("eglInitialize failed")
            }

            val eglConfig = WallpaperEglConfigChooser().chooseConfig(egl!!, eglDisplay!!)

            val attribList = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, Constants.GL_ES_VERSION,
                EGL10.EGL_NONE
            )

            eglContext = egl?.eglCreateContext(
                eglDisplay,
                eglConfig,
                EGL10.EGL_NO_CONTEXT,
                attribList
            )

            if (eglContext == null || eglContext == EGL10.EGL_NO_CONTEXT) {
                throw RuntimeException("eglCreateContext failed")
            }

            eglSurface = egl?.eglCreateWindowSurface(eglDisplay, eglConfig, holder, null)

            if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
                throw RuntimeException("eglCreateWindowSurface failed")
            }

            if (egl?.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext) == false) {
                throw RuntimeException("eglMakeCurrent failed")
            }

            gl = eglContext?.gl as? GL10

            gl?.let { renderer.onSurfaceCreated(it, eglConfig) }
        }

        private fun cleanupGL() {
            egl?.let { eglInstance ->
                eglInstance.eglMakeCurrent(
                    eglDisplay,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT
                )

                eglSurface?.let {
                    eglInstance.eglDestroySurface(eglDisplay, it)
                }

                eglContext?.let {
                    eglInstance.eglDestroyContext(eglDisplay, it)
                }

                eglInstance.eglTerminate(eglDisplay)
            }

            egl = null
            eglDisplay = null
            eglSurface = null
            eglContext = null
            gl = null
        }
    }

}

internal class WallpaperEglConfigChooser : GLSurfaceView.EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        val colorSpecs = listOf(
            intArrayOf(EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8),
            intArrayOf(EGL10.EGL_RED_SIZE, 5, EGL10.EGL_GREEN_SIZE, 6, EGL10.EGL_BLUE_SIZE, 5),
            intArrayOf()
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        for (colors in colorSpecs) {
            val attributes = colors + intArrayOf(
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL10.EGL_NONE
            )
            if (egl.eglChooseConfig(display, attributes, configs, 1, count) && count[0] > 0) {
                return checkNotNull(configs[0])
            }
        }
        error("No compatible OpenGL ES 2 window config")
    }
}
