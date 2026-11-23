package com.anthonyla.paperize.service.livewallpaper

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.EmptyAlbumException
import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.util.isReadable
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.model.usesVisibleLiveTimer
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.usecase.ChangeWallpaperUseCase
import com.anthonyla.paperize.service.LiveWallpaperReload
import com.anthonyla.paperize.service.livewallpaper.gl.GLWallpaperService
import com.anthonyla.paperize.service.livewallpaper.renderer.ContentUriImageLoader
import com.anthonyla.paperize.service.livewallpaper.renderer.EmptyImageLoader
import com.anthonyla.paperize.service.livewallpaper.renderer.ImageLoader
import com.anthonyla.paperize.service.livewallpaper.renderer.PaperizeRenderController
import com.anthonyla.paperize.service.livewallpaper.renderer.PaperizeWallpaperRenderer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class PaperizeLiveWallpaperService : GLWallpaperService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var changeWallpaperUseCase: ChangeWallpaperUseCase
    @Inject lateinit var liveWallpaperReload: LiveWallpaperReload

    companion object {
        private const val TAG = "PaperizeLiveWallpaper"

        // Their stock launchers often do not report wallpaper scroll offsets.
        private val PARALLAX_ISSUE_MANUFACTURERS =
            setOf("SAMSUNG", "XIAOMI", "HUAWEI", "OPPO", "VIVO", "REALME", "ONEPLUS")

        private fun launcherMayIgnoreParallax(): Boolean =
            Build.MANUFACTURER.uppercase().let { maker -> PARALLAX_ISSUE_MANUFACTURERS.any(maker::contains) }

        fun isActive(context: Context): Boolean {
            // Checking from within the service itself (e.g. preview mode) means it is active.
            if (context is PaperizeLiveWallpaperService) return true
            return try {
                WallpaperManager.getInstance(context).wallpaperInfo?.component ==
                    ComponentName(context, PaperizeLiveWallpaperService::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking live wallpaper status", e)
                false
            }
        }
    }

    private val engines = mutableSetOf<PaperizeLiveWallpaperEngine>()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Adaptive brightness reads dark mode at draw time, so a theme switch needs a new frame.
        engines.forEach { it.requestRender() }
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
        private var latestSettings = ScheduleSettings()
        private var latestWallpaperMode = WallpaperMode.STATIC
        private var liveIntervalJob: Job? = null
        private var screenOffChangeJob: Job? = null
        private val engineVisible = MutableStateFlow(false)

        private val gestureDetector = GestureDetector(
            this@PaperizeLiveWallpaperService,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    handleDoubleTap()
                    return true
                }
            }
        )

        private val screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                handleScreenOff()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            Log.d(TAG, "Engine created")

            setOffsetNotificationsEnabled(true)

            setTouchEventsEnabled(true)

            renderer = PaperizeWallpaperRenderer(applicationContext, this)
            renderController =
                PaperizeRenderController(renderer::queueWallpaper, engineScope, ::openNextWallpaper)

            setRenderer(renderer)
            requestRender()
            engines += this

            observeSettings()

            engineScope.launch {
                liveWallpaperReload.requests.collect {
                    renderController.reloadCurrentArtwork()
                    restartLiveIntervalTimer()
                }
            }

            val screenOffFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            ContextCompat.registerReceiver(
                applicationContext,
                screenOffReceiver,
                screenOffFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        /** Null while no wallpaper is readable. Queue and database errors reach the controller. */
        private suspend fun openNextWallpaper(): ImageLoader? {
            // In STATIC mode the static wallpaper worker owns HOME/LOCK; the engine stays empty.
            if (settingsRepository.getWallpaperMode() != WallpaperMode.LIVE) return EmptyImageLoader
            val settings = settingsRepository.getScheduleSettings()
            val albumId = settings.liveAlbumId ?: return EmptyImageLoader

            return try {
                val loader = withContext(Dispatchers.IO) {
                    changeWallpaperUseCase.dequeueReadable(albumId, ScreenType.LIVE, settings.shuffleEnabled) { candidate ->
                        val uri = candidate.uri.toUri()
                        if (uri.isReadable(contentResolver)) {
                            currentWallpaper = candidate
                            ContentUriImageLoader(contentResolver, uri, settings.liveScalingType)
                        } else {
                            null
                        }
                    }
                }
                if (loader == null) Log.w(TAG, "No readable wallpaper in album $albumId")
                loader
            } catch (_: EmptyAlbumException) {
                Log.w(TAG, "No wallpapers in album $albumId")
                EmptyImageLoader
            }
        }

        private fun observeSettings() {
            engineScope.launch {
                combine(
                    settingsRepository.getScheduleSettingsFlow(),
                    settingsRepository.getWallpaperModeFlow()
                ) { settings, mode ->
                    Pair(settings, mode)
                }.collect { (settings, mode) ->
                    val timerConfigurationChanged =
                        latestWallpaperMode != mode ||
                            latestSettings.enableChanger != settings.enableChanger ||
                            latestSettings.liveAlbumId != settings.liveAlbumId ||
                            latestSettings.liveIntervalMinutes != settings.liveIntervalMinutes
                    latestSettings = settings
                    latestWallpaperMode = mode
                    if (timerConfigurationChanged) restartLiveIntervalTimer()

                    if (mode != WallpaperMode.LIVE) {
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
                    renderer.updateBlurFitBackground(settings.blurFitBackground)

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
                    
                    if (effects.enableParallax && !hasShownParallaxWarning && launcherMayIgnoreParallax()) {
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
            renderController.visible = visible
            engineVisible.value = visible
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
            renderer.setNormalOffsetX(xOffset)
        }

        override fun onTouchEvent(event: MotionEvent) {
            gestureDetector.onTouchEvent(event)
            super.onTouchEvent(event)
        }

        override fun onDestroy() {
            engines -= this
            applicationContext.unregisterReceiver(screenOffReceiver)

            renderer.cancelLoading()

            engineScope.cancel()
            super.onDestroy()
        }

        override fun queueEventOnGlThread(event: () -> Unit): Boolean = queueEvent(event)

        private fun handleDoubleTap() {
            engineScope.launch {
                val settings = settingsRepository.getScheduleSettings()
                
                val doubleTapEnabled = settings.liveEffects.enableDoubleTap

                if (doubleTapEnabled) {
                    renderController.reloadCurrentArtwork()
                    restartLiveIntervalTimer()
                }
            }
        }

        private fun handleScreenOff() {
            screenOffChangeJob?.cancel()
            screenOffChangeJob = engineScope.launch {
                val settings = settingsRepository.getScheduleSettings()
                if (settings.liveEffects.enableChangeOnScreenOff) {
                    // Some skins still draw the wallpaper through their screen-off animation; an always-on display never hides it.
                    val hidden = withTimeoutOrNull(2.seconds) { engineVisible.first { !it } } != null
                    if (!hidden && getSystemService(PowerManager::class.java).isInteractive) {
                        Log.d(TAG, "Screen turned back on before wallpaper hid - skipping change")
                        return@launch
                    }
                    Log.d(TAG, "Screen off - changing wallpaper")
                    renderController.forceReloadCurrentArtwork()
                }
            }
        }

        private fun restartLiveIntervalTimer() {
            liveIntervalJob?.cancel()
            liveIntervalJob = null

            val intervalMinutes = latestSettings.liveIntervalMinutes
            val shouldRun =
                renderController.visible &&
                    !isPreview &&
                    latestWallpaperMode == WallpaperMode.LIVE &&
                    latestSettings.enableChanger &&
                    latestSettings.liveAlbumId != null &&
                    usesVisibleLiveTimer(intervalMinutes)
            if (!shouldRun) return

            liveIntervalJob = engineScope.launch {
                while (isActive) {
                    delay(intervalMinutes.minutes)
                    Log.d(TAG, "Visible live interval elapsed; changing wallpaper")
                    renderController.reloadCurrentArtwork()
                }
            }
        }
    }
}
