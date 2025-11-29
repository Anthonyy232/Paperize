package com.anthonyla.paperize.service.livewallpaper

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.service.livewallpaper.gl.GLWallpaperService
import com.anthonyla.paperize.service.livewallpaper.renderer.ContentUriImageLoader
import com.anthonyla.paperize.service.livewallpaper.renderer.EmptyImageLoader
import com.anthonyla.paperize.service.livewallpaper.renderer.ImageLoader
import com.anthonyla.paperize.service.livewallpaper.renderer.PaperizeWallpaperRenderer
import com.anthonyla.paperize.service.livewallpaper.renderer.PaperizeRenderController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.util.isValid
import com.anthonyla.paperize.domain.model.Wallpaper

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PaperizeLiveWallpaperEntryPoint {
    fun settingsRepository(): SettingsRepository
    fun wallpaperRepository(): WallpaperRepository
}

@AndroidEntryPoint
class PaperizeLiveWallpaperService : GLWallpaperService(), LifecycleOwner {

    companion object {
        private const val TAG = "PaperizeLiveWallpaper"
    }

    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onCreateEngine(): Engine {
        return PaperizeLiveWallpaperEngine()
    }

    inner class PaperizeLiveWallpaperEngine : GLEngine(),
        LifecycleOwner,
        SavedStateRegistryOwner,
        PaperizeRenderController.Callbacks,
        PaperizeWallpaperRenderer.Callbacks {

        private lateinit var settingsRepository: SettingsRepository
        private lateinit var wallpaperRepository: WallpaperRepository

        private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        private lateinit var renderer: PaperizeWallpaperRenderer
        private lateinit var renderController: PaperizeRenderController
        private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private var currentAlbumId: String? = null

        private val gestureDetector = GestureDetector(
            this@PaperizeLiveWallpaperService,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    handleDoubleTap()
                    return true
                }
            }
        )

        private val reloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Constants.ACTION_RELOAD_WALLPAPER) {
                    Log.d(TAG, "Received reload broadcast")
                    renderController.reloadCurrentArtwork(com.anthonyla.paperize.service.livewallpaper.renderer.ReloadImmediate)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            Log.d(TAG, "Engine created")

            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                PaperizeLiveWallpaperEntryPoint::class.java
            )
            settingsRepository = entryPoint.settingsRepository()
            wallpaperRepository = entryPoint.wallpaperRepository()

            renderer = PaperizeWallpaperRenderer(applicationContext, this)
            renderController = object : PaperizeRenderController(renderer, this@PaperizeLiveWallpaperEngine, engineScope) {
                override suspend fun openDownloadedCurrentArtwork(): ImageLoader {
                    val mode = settingsRepository.getWallpaperMode()
                    val settings = settingsRepository.getScheduleSettings()
                    
                    val (albumId, screenType) = if (mode == com.anthonyla.paperize.core.WallpaperMode.LIVE) {
                        Pair(settings.liveAlbumId, ScreenType.LIVE)
                    } else {
                        Pair(settings.homeAlbumId, ScreenType.HOME)
                    }

                    if (albumId == null) {
                        Log.w(TAG, "No album ID set for mode $mode")
                        return EmptyImageLoader
                    }

                    // Check if queue exists, build it if empty
                    val queueCheck = wallpaperRepository.getNextWallpaperInQueue(albumId, screenType)
                    if (queueCheck == null) {
                        wallpaperRepository.buildWallpaperQueue(albumId, screenType, settings.shuffleEnabled)
                    }
                    
                    var wallpaper: Wallpaper? = null
                    var maxRetries = 10 // Prevent infinite loop
                    var queueRebuildAttempts = 0

                    while (wallpaper == null && maxRetries > 0) {
                        // Atomically get and remove from queue
                        val candidate = wallpaperRepository.getAndDequeueWallpaper(albumId, screenType)

                        if (candidate == null) {
                            queueRebuildAttempts++
                            if (queueRebuildAttempts > 2) {
                                Log.w(TAG, "No wallpapers in album $albumId after retries")
                                return EmptyImageLoader
                            }

                            // Rebuild queue
                            wallpaperRepository.buildWallpaperQueue(albumId, screenType, settings.shuffleEnabled)
                            continue
                        }

                        // Validate URI
                        val uri = candidate.uri.toUri()
                        if (uri.isValid(contentResolver)) {
                            wallpaper = candidate
                        } else {
                            // Remove invalid wallpaper
                            wallpaperRepository.deleteWallpaper(candidate.id)
                            maxRetries--
                        }
                    }

                    if (wallpaper == null) {
                        Log.w(TAG, "No valid wallpaper found after retries")
                        return EmptyImageLoader
                    }

                    // Check if queue needs refilling
                    val queueSize = wallpaperRepository.getNextWallpaperInQueue(albumId, screenType)
                    if (queueSize == null) {
                        wallpaperRepository.buildWallpaperQueue(albumId, screenType, settings.shuffleEnabled)
                    }

                    return try {
                        val uri = wallpaper.uri.toUri()
                        ContentUriImageLoader(contentResolver, uri)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating image loader", e)
                        EmptyImageLoader
                    }
                }
            }

            lifecycle.addObserver(renderController)
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 0, 0, 0)
            setRenderer(renderer)
            setRenderMode(GLWallpaperService.RENDERMODE_WHEN_DIRTY)
            requestRender()

            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            // Trigger initial load
            renderController.reloadCurrentArtwork(com.anthonyla.paperize.service.livewallpaper.renderer.ReloadImmediate)

            // Observe settings changes
            observeSettings()

            // Register broadcast receiver
            val filter = IntentFilter(Constants.ACTION_RELOAD_WALLPAPER)
            ContextCompat.registerReceiver(
                applicationContext,
                reloadReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        private fun observeSettings() {
            engineScope.launch {
                combine(
                    settingsRepository.getScheduleSettingsFlow(),
                    settingsRepository.getWallpaperModeFlow()
                ) { settings, mode ->
                    Pair(settings, mode)
                }.collect { (settings, mode) ->
                    val (albumId, effects) = if (mode == com.anthonyla.paperize.core.WallpaperMode.LIVE) {
                        Pair(settings.liveAlbumId, settings.liveEffects)
                    } else {
                        Pair(settings.homeAlbumId, settings.homeEffects)
                    }

                    renderer.updateEffects(effects)

                    // Reload if album changed
                    if (albumId != currentAlbumId) {
                        Log.d(TAG, "Album changed from $currentAlbumId to $albumId, reloading")
                        currentAlbumId = albumId
                        renderController.reloadCurrentArtwork()
                    }
                }
            }
        }


        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            renderController.visible = visible
            super.onVisibilityChanged(visible)
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            renderer.setNormalOffsetX(xOffset)
        }

        override fun onTouchEvent(event: MotionEvent) {
            try {
                gestureDetector.onTouchEvent(event)
            } catch (e: Exception) {
                Log.w(TAG, "Error processing touch event", e)
            }
            super.onTouchEvent(event)
        }

        override fun onDestroy() {
            try {
                applicationContext.unregisterReceiver(reloadReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }

            // Cleanup renderer resources on GL thread
            queueEvent {
                renderer.destroy()
            }

            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            engineScope.cancel()
            super.onDestroy()
        }

        override fun queueEventOnGlThread(event: () -> Unit) {
            queueEvent(event)
        }

        private fun handleDoubleTap() {
            engineScope.launch {
                val settings = settingsRepository.getScheduleSettings()
                val mode = settingsRepository.getWallpaperMode()
                
                val doubleTapEnabled = if (mode == com.anthonyla.paperize.core.WallpaperMode.LIVE) {
                    settings.liveEffects.enableDoubleTap
                } else {
                    settings.homeEffects.enableDoubleTap
                }

                if (doubleTapEnabled) {
                    renderController.reloadCurrentArtwork(com.anthonyla.paperize.service.livewallpaper.renderer.ReloadImmediate)
                }
            }
        }
    }
}
