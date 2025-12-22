package com.anthonyla.paperize.service.livewallpaper.gl

import android.opengl.GLSurfaceView
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
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
        const val RENDERMODE_WHEN_DIRTY = 0
        const val RENDERMODE_CONTINUOUSLY = 1
    }

    /**
     * Abstract engine that supports OpenGL ES rendering.
     * Subclasses should override onCreateEngine() to return an instance.
     */
    abstract inner class GLEngine : Engine() {

        private var glThread: GLThread? = null
        private var renderer: GLSurfaceView.Renderer? = null
        private var eglContextClientVersion = 2
        private var internalRenderMode = RENDERMODE_WHEN_DIRTY

        // EGL configuration
        private var eglConfigChooser: EGLConfigChooser? = null

        /**
         * Set the renderer for this engine.
         * Must be called before surface is created.
         */
        fun setRenderer(renderer: GLSurfaceView.Renderer) {
            this.renderer = renderer
        }

        /**
         * Set the OpenGL ES context client version (2 or 3).
         * Must be called before surface is created.
         */
        fun setEGLContextClientVersion(version: Int) {
            this.eglContextClientVersion = version
        }

        /**
         * Set the EGL config chooser.
         */
        fun setEGLConfigChooser(configChooser: EGLConfigChooser) {
            this.eglConfigChooser = configChooser
        }

        /**
         * Simplified EGL config chooser for RGB888 with no depth/stencil.
         */
        fun setEGLConfigChooser(
            redSize: Int,
            greenSize: Int,
            blueSize: Int,
            alphaSize: Int,
            depthSize: Int,
            stencilSize: Int
        ) {
            this.eglConfigChooser = SimpleEGLConfigChooser(
                redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize, eglContextClientVersion
            )
        }

        /**
         * Set the render mode: RENDERMODE_WHEN_DIRTY or RENDERMODE_CONTINUOUSLY.
         */
        fun setRenderMode(mode: Int) {
            this.internalRenderMode = mode
        }

        /**
         * Request a render. Only has effect if render mode is RENDERMODE_WHEN_DIRTY.
         */
        fun requestRender() {
            glThread?.requestRender()
        }

        /**
         * Queue a runnable to be run on the GL thread.
         */
        fun queueEvent(runnable: Runnable) {
            glThread?.queueEvent(runnable)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)

            val configChooser = eglConfigChooser ?: SimpleEGLConfigChooser(
                8, 8, 8, 0, 0, 0, eglContextClientVersion
            )

            glThread = GLThread(
                holder = holder,
                renderer = renderer ?: throw IllegalStateException("Renderer not set"),
                configChooser = configChooser,
                eglContextClientVersion = eglContextClientVersion
            ).apply {
                renderMode = this@GLEngine.internalRenderMode
                start()
            }
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

    /**
     * GL rendering thread.
     */
    private class GLThread(
        private val holder: SurfaceHolder,
        private val renderer: GLSurfaceView.Renderer,
        private val configChooser: EGLConfigChooser,
        private val eglContextClientVersion: Int
    ) : Thread("GLThread") {

        @Volatile var renderMode = RENDERMODE_WHEN_DIRTY

        private val eventQueue = mutableListOf<Runnable>()
        private val lock = Object()

        @Volatile private var shouldExit = false
        @Volatile private var paused = false
        @Volatile private var hasSurface = true
        @Volatile private var width = 0
        @Volatile private var height = 0
        @Volatile private var requestRender = true
        @Volatile private var sizeChanged = true

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

        fun queueEvent(runnable: Runnable) {
            synchronized(eventQueue) {
                eventQueue.add(runnable)
            }
            requestRender()
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

                    synchronized(lock) {
                        while (true) {
                            if (shouldExit) {
                                cleanupGL()
                                return
                            }

                            // Process events
                            synchronized(eventQueue) {
                                if (eventQueue.isNotEmpty()) {
                                    event = eventQueue.removeAt(0)
                                    break
                                }
                            }

                            // Check if we should render
                            val readyToDraw = hasSurface && !paused
                            if (readyToDraw && (renderMode == RENDERMODE_CONTINUOUSLY || requestRender)) {
                                requestRender = false
                                break
                            }

                            // Wait for event
                            lock.wait()
                        }
                    }

                    // Execute event
                    event?.let {
                        it.run()
                        continue
                    }

                    // Handle size change
                    if (sizeChanged) {
                        gl?.let { renderer.onSurfaceChanged(it, width, height) }
                        sizeChanged = false
                    }

                    // Render frame
                    gl?.let { renderer.onDrawFrame(it) }

                    // Swap buffers
                    egl?.let { eglInstance ->
                        if (!eglInstance.eglSwapBuffers(eglDisplay, eglSurface)) {
                            Log.w(TAG, "eglSwapBuffers failed")
                        }
                    }

                    // Yield to avoid hogging CPU
                    sleep(1)
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

            val eglConfig = configChooser.chooseConfig(egl!!, eglDisplay!!)

            val attribList = intArrayOf(
                0x3098, // EGL_CONTEXT_CLIENT_VERSION
                eglContextClientVersion,
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

    /**
     * Interface for choosing EGL configuration.
     */
    interface EGLConfigChooser {
        fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig
    }

    /**
     * Simple EGL config chooser that picks the first config matching the requirements.
     */
    private class SimpleEGLConfigChooser(
        private val redSize: Int,
        private val greenSize: Int,
        private val blueSize: Int,
        private val alphaSize: Int,
        private val depthSize: Int,
        private val stencilSize: Int,
        private val eglContextClientVersion: Int
    ) : EGLConfigChooser {

        override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
            val renderableType = if (eglContextClientVersion >= 3) {
                0x0040 // EGL_OPENGL_ES3_BIT
            } else {
                0x0004 // EGL_OPENGL_ES2_BIT
            }

            // Try with EGL_RENDERABLE_TYPE
            var configSpec = intArrayOf(
                EGL10.EGL_RED_SIZE, redSize,
                EGL10.EGL_GREEN_SIZE, greenSize,
                EGL10.EGL_BLUE_SIZE, blueSize,
                EGL10.EGL_ALPHA_SIZE, alphaSize,
                EGL10.EGL_DEPTH_SIZE, depthSize,
                EGL10.EGL_STENCIL_SIZE, stencilSize,
                0x3142, renderableType, // EGL_RENDERABLE_TYPE
                EGL10.EGL_NONE
            )

            val numConfig = IntArray(1)
            if (!egl.eglChooseConfig(display, configSpec, null, 0, numConfig)) {
                Log.w(TAG, "eglChooseConfig failed with EGL_RENDERABLE_TYPE, retrying without it")
                // Fallback: Try without EGL_RENDERABLE_TYPE
                configSpec = intArrayOf(
                    EGL10.EGL_RED_SIZE, redSize,
                    EGL10.EGL_GREEN_SIZE, greenSize,
                    EGL10.EGL_BLUE_SIZE, blueSize,
                    EGL10.EGL_ALPHA_SIZE, alphaSize,
                    EGL10.EGL_DEPTH_SIZE, depthSize,
                    EGL10.EGL_STENCIL_SIZE, stencilSize,
                    EGL10.EGL_NONE
                )
                if (!egl.eglChooseConfig(display, configSpec, null, 0, numConfig)) {
                    throw RuntimeException("eglChooseConfig failed")
                }
            }

            if (numConfig[0] <= 0) {
                throw RuntimeException("No EGL configs match")
            }

            val configs = arrayOfNulls<EGLConfig>(numConfig[0])
            if (!egl.eglChooseConfig(display, configSpec, configs, numConfig[0], numConfig)) {
                throw RuntimeException("eglChooseConfig failed")
            }

            return configs[0] ?: throw RuntimeException("No config chosen")
        }
    }
}
