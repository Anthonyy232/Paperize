package com.anthonyla.paperize.service.livewallpaper

import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.usesVisibleLiveTimer
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.service.livewallpaper.gl.GLWallpaperService
import com.anthonyla.paperize.service.livewallpaper.renderer.ContentUriImageLoader
import com.anthonyla.paperize.service.livewallpaper.renderer.EmptyImageLoader
import com.anthonyla.paperize.service.livewallpaper.renderer.PaperizeWallpaperRenderer
import com.anthonyla.paperize.service.livewallpaper.renderer.PaperizeRenderController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
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
import android.widget.Toast
import com.anthonyla.paperize.R

@AndroidEntryPoint
class PaperizeLiveWallpaperService : GLWallpaperService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var wallpaperRepository: WallpaperRepository

    companion object {
        private const val TAG = "PaperizeLiveWallpaper"
    }

    override fun onCreateEngine(): Engine {
        return PaperizeLiveWallpaperEngine()
    }

    inner class PaperizeLiveWallpaperEngine : GLEngine(),
        PaperizeWallpaperRenderer.Callbacks {

        private lateinit var renderer: PaperizeWallpaperRenderer
        private lateinit var renderController: PaperizeRenderController
        private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private var currentAlbumId: String? = null
        @Volatile private var currentWallpaper: Wallpaper? = null
        private var observedScalingType: ScalingType? = null
        private var hasShownParallaxWarning = false
        private var engineVisible = false
        private var latestSettings = ScheduleSettings()
        private var latestWallpaperMode = WallpaperMode.STATIC
        private var liveIntervalJob: Job? = null

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
                    renderController.reloadCurrentArtwork(immediate = true)
                    restartLiveIntervalTimer()
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

            setOffsetNotificationsEnabled(true)

            setTouchEventsEnabled(true)

            renderer = PaperizeWallpaperRenderer(applicationContext, this)
            renderController = PaperizeRenderController(renderer::queueWallpaper, engineScope) {
                withContext(Dispatchers.IO) {
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

                    val queueCheck = wallpaperRepository.getNextWallpaperInQueue(albumId, ScreenType.LIVE)
                    if (queueCheck == null) {
                        wallpaperRepository.ensureWallpaperQueue(albumId, ScreenType.LIVE, settings.shuffleEnabled)
                    }
                    
                    var wallpaper: Wallpaper? = null
                    var maxRetries = Constants.MAX_WALLPAPER_LOAD_RETRIES // Prevent infinite loop
                    var queueRebuildAttempts = 0

                    while (wallpaper == null && maxRetries > 0) {
                        val candidate = wallpaperRepository.getAndDequeueWallpaper(albumId, ScreenType.LIVE)

                        if (candidate == null) {
                            queueRebuildAttempts++
                            if (queueRebuildAttempts > Constants.MAX_QUEUE_REBUILD_ATTEMPTS) {
                                Log.w(TAG, "No wallpapers in album $albumId after retries")
                                return@withContext EmptyImageLoader
                            }

                            wallpaperRepository.ensureWallpaperQueue(albumId, ScreenType.LIVE, settings.shuffleEnabled)
                            continue
                        }

                        val uri = candidate.uri.toUri()
                        if (uri.isValid(contentResolver)) {
                            wallpaper = candidate
                        } else {
                            // Skip this cycle — do not permanently delete.
                            // A transient permission or storage issue should not remove it from the album.
                            // AlbumRefreshWorker handles pruning of truly invalid URIs on its daily scan.
                            maxRetries--
                        }
                    }

                    if (wallpaper == null) {
                        Log.w(TAG, "No valid wallpaper found after retries")
                        return@withContext EmptyImageLoader
                    }

                    currentWallpaper = wallpaper

                    val nextInQueue = wallpaperRepository.getNextWallpaperInQueue(albumId, ScreenType.LIVE)
                    if (nextInQueue == null) {
                        wallpaperRepository.ensureWallpaperQueue(albumId, ScreenType.LIVE, settings.shuffleEnabled)
                    }

                    ContentUriImageLoader(contentResolver, wallpaper.uri.toUri(), settings.liveScalingType)
                }
            }

            setRenderer(renderer)
            requestRender()

            renderController.reloadCurrentArtwork(immediate = true)

            observeSettings()

            val filter = IntentFilter(Constants.ACTION_RELOAD_WALLPAPER)
            ContextCompat.registerReceiver(
                applicationContext,
                reloadReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

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
                    val timerConfigurationChanged =
                        latestWallpaperMode != mode ||
                            latestSettings.enableChanger != settings.enableChanger ||
                            latestSettings.liveAlbumId != settings.liveAlbumId ||
                            latestSettings.liveIntervalMinutes != settings.liveIntervalMinutes
                    latestSettings = settings
                    latestWallpaperMode = mode
                    if (timerConfigurationChanged) restartLiveIntervalTimer()

                    if (mode != com.anthonyla.paperize.core.WallpaperMode.LIVE) {
                        return@collect
                    }
                    
                    val albumId = settings.liveAlbumId
                    val effects = settings.liveEffects
                    val scalingType = settings.liveScalingType

                    val albumChanged = albumId != currentAlbumId
                    if (albumChanged) {
                        currentWallpaper = null
                    }

                    renderer.updateEffects(effects)
                    renderer.updateScalingType(scalingType)
                    renderer.updateAdaptiveBrightness(settings.adaptiveBrightness)

                    val scalingChanged =
                        observedScalingType != null && observedScalingType != scalingType
                    observedScalingType = scalingType

                    if (scalingChanged && !albumChanged) {
                        currentWallpaper?.let { wallpaper ->
                            Log.d(TAG, "Live scaling changed; reloading current wallpaper without advancing")
                            renderer.queueWallpaper(
                                ContentUriImageLoader(
                                    contentResolver,
                                    wallpaper.uri.toUri(),
                                    scalingType
                                )
                            )
                        }
                    }
                    
                    if (effects.enableParallax && !hasShownParallaxWarning && GLCompatibility.shouldWarnAboutParallax()) {
                        hasShownParallaxWarning = true
                        Toast.makeText(
                            applicationContext,
                            R.string.parallax_may_not_work,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    
                    if (albumChanged) {
                        Log.d(TAG, "Album changed from $currentAlbumId to $albumId, reloading")
                        currentAlbumId = albumId
                        renderController.reloadCurrentArtwork()
                    }
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            engineVisible = visible
            renderController.visible = visible
            if (visible) {
                // Re-evaluate draw-time effects such as adaptive brightness after
                // configuration changes while the wallpaper was hidden.
                requestRender()
            }
            restartLiveIntervalTimer()
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

            renderer.cancelLoading()

            queueEvent {
                renderer.destroy()
            }

            engineScope.cancel()
            super.onDestroy()
        }

        override fun queueEventOnGlThread(event: () -> Unit): Boolean = queueEvent(event)

        private fun handleDoubleTap() {
            engineScope.launch {
                val settings = settingsRepository.getScheduleSettings()
                
                val doubleTapEnabled = settings.liveEffects.enableDoubleTap

                if (doubleTapEnabled) {
                    renderController.reloadCurrentArtwork(immediate = true)
                    restartLiveIntervalTimer()
                }
            }
        }

        private fun handleScreenOff() {
            engineScope.launch {
                val settings = settingsRepository.getScheduleSettings()
                if (settings.liveEffects.enableChangeOnScreenOff) {
                    Log.d(TAG, "Screen off - changing wallpaper")
                    // Use forceReload to bypass visibility check and load while screen is off
                    renderController.forceReloadCurrentArtwork()
                }
            }
        }

        private fun restartLiveIntervalTimer() {
            liveIntervalJob?.cancel()
            liveIntervalJob = null

            val intervalMinutes = latestSettings.liveIntervalMinutes
            val shouldRun =
                engineVisible &&
                    !isPreview &&
                    latestWallpaperMode == WallpaperMode.LIVE &&
                    latestSettings.enableChanger &&
                    latestSettings.liveAlbumId != null &&
                    usesVisibleLiveTimer(intervalMinutes)
            if (!shouldRun) return

            liveIntervalJob = engineScope.launch {
                val intervalMillis = intervalMinutes.toLong() * 60_000L
                while (isActive) {
                    delay(intervalMillis)
                    Log.d(TAG, "Visible live interval elapsed; changing wallpaper")
                    renderController.reloadCurrentArtwork(
                        immediate = true
                    )
                }
            }
        }
    }
}
