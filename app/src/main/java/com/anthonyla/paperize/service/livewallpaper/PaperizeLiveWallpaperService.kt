package com.anthonyla.paperize.service.livewallpaper

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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.util.isValid
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.service.livewallpaper.gl.GLCompatibility
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.anthonyla.paperize.R

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
        private var hasShownParallaxWarning = false

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

        private val screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    handleScreenOff()
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            Log.d(TAG, "Engine created")

            // Enable offset notifications to receive scroll events
            setOffsetNotificationsEnabled(true)

            // Enable touch events for double-tap gesture detection
            setTouchEventsEnabled(true)

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
                override suspend fun openDownloadedCurrentArtwork(): ImageLoader = withContext(Dispatchers.IO) {
                    val mode = settingsRepository.getWallpaperMode()
                    val settings = settingsRepository.getScheduleSettings()
                    
                    // Live Wallpaper only operates in LIVE mode
                    // In STATIC mode, the static wallpaper worker handles HOME/LOCK screens separately
                    if (mode != com.anthonyla.paperize.core.WallpaperMode.LIVE) {
                        Log.d(TAG, "App is in STATIC mode, Live Wallpaper not active")
                        return@withContext EmptyImageLoader
                    }
                    
                    val albumId = settings.liveAlbumId

                    if (albumId == null) {
                        Log.w(TAG, "No live album ID set")
                        return@withContext EmptyImageLoader
                    }

                    // Check if queue exists, build it if empty
                    val queueCheck = wallpaperRepository.getNextWallpaperInQueue(albumId, ScreenType.LIVE)
                    if (queueCheck == null) {
                        wallpaperRepository.buildWallpaperQueue(albumId, ScreenType.LIVE, settings.shuffleEnabled)
                    }
                    
                    var wallpaper: Wallpaper? = null
                    var maxRetries = Constants.MAX_WALLPAPER_LOAD_RETRIES // Prevent infinite loop
                    var queueRebuildAttempts = 0

                    while (wallpaper == null && maxRetries > 0) {
                        // Atomically get and remove from queue
                        val candidate = wallpaperRepository.getAndDequeueWallpaper(albumId, ScreenType.LIVE)

                        if (candidate == null) {
                            queueRebuildAttempts++
                            if (queueRebuildAttempts > Constants.MAX_QUEUE_REBUILD_ATTEMPTS) {
                                Log.w(TAG, "No wallpapers in album $albumId after retries")
                                return@withContext EmptyImageLoader
                            }

                            // Rebuild queue
                            wallpaperRepository.buildWallpaperQueue(albumId, ScreenType.LIVE, settings.shuffleEnabled)
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
                        return@withContext EmptyImageLoader
                    }

                    // Peek at queue to see if it needs refilling (not dequeuing, just checking)
                    val nextInQueue = wallpaperRepository.getNextWallpaperInQueue(albumId, ScreenType.LIVE)
                    if (nextInQueue == null) {
                        wallpaperRepository.buildWallpaperQueue(albumId, ScreenType.LIVE, settings.shuffleEnabled)
                    }

                    try {
                        val uri = wallpaper.uri.toUri()
                        ContentUriImageLoader(contentResolver, uri)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating image loader", e)
                        EmptyImageLoader
                    }
                }
            }

            lifecycle.addObserver(renderController)
            setEGLContextClientVersion(Constants.GL_ES_VERSION)
            setEGLConfigChooser(8, 8, 8, 0, 0, 0)
            setRenderer(renderer)
            setRenderMode(RENDERMODE_WHEN_DIRTY)
            requestRender()

            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            // Trigger initial load
            renderController.reloadCurrentArtwork(com.anthonyla.paperize.service.livewallpaper.renderer.ReloadImmediate)

            // Observe settings changes
            observeSettings()

            // Register broadcast receiver for reload
            val filter = IntentFilter(Constants.ACTION_RELOAD_WALLPAPER)
            ContextCompat.registerReceiver(
                applicationContext,
                reloadReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            // Register screen-off receiver for wallpaper change on screen off
            val screenOffFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            ContextCompat.registerReceiver(
                applicationContext,
                screenOffReceiver,
                screenOffFilter,
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
                }.catch { e ->
                    Log.e(TAG, "Error observing settings", e)
                }.collect { (settings, mode) ->
                    // Only process settings in LIVE mode
                    // In STATIC mode, the static wallpaper worker handles HOME/LOCK screens
                    if (mode != com.anthonyla.paperize.core.WallpaperMode.LIVE) {
                        return@collect
                    }
                    
                    val albumId = settings.liveAlbumId
                    val effects = settings.liveEffects
                    val scalingType = settings.liveScalingType

                    renderer.updateEffects(effects)
                    renderer.updateScalingType(scalingType)
                    renderer.updateAdaptiveBrightness(settings.adaptiveBrightness)
                    
                    // Show Toast warning if parallax is enabled but device has offset issues
                    if (effects.enableParallax && !hasShownParallaxWarning && GLCompatibility.shouldWarnAboutParallax()) {
                        hasShownParallaxWarning = true
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                applicationContext,
                                R.string.parallax_may_not_work,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    
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
            Log.d(TAG, "onOffsetsChanged: xOffset=$xOffset, xOffsetStep=$xOffsetStep, xPixelOffset=$xPixelOffset")
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
                Log.e(TAG, "Error unregistering reload receiver", e)
            }
            try {
                applicationContext.unregisterReceiver(screenOffReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering screen-off receiver", e)
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
                
                // Always check liveEffects since double-tap is only available in live wallpaper mode
                val doubleTapEnabled = settings.liveEffects.enableDoubleTap

                if (doubleTapEnabled) {
                    renderController.reloadCurrentArtwork(com.anthonyla.paperize.service.livewallpaper.renderer.ReloadImmediate)
                }
            }
        }

        private fun handleScreenOff() {
            engineScope.launch {
                val settings = settingsRepository.getScheduleSettings()
                if (settings.liveEffects.enableChangeOnScreenOn) {
                    Log.d(TAG, "Screen off - changing wallpaper")
                    // Use forceReload to bypass visibility check and load while screen is off
                    renderController.forceReloadCurrentArtwork()
                }
            }
        }
    }
}
