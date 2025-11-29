package com.anthonyla.paperize.service.livewallpaper.renderer

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Abstract controller for managing wallpaper loading and rendering lifecycle.
 * Handles visibility changes, load orchestration, and coordination between
 * background loading and GL thread rendering.
 */
abstract class PaperizeRenderController(
    private val renderer: PaperizeWallpaperRenderer,
    private val callbacks: Callbacks,
    private val controllerScope: CoroutineScope
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "PaperizeRenderController"
        private const val RELOAD_THROTTLE_MS = 250L
    }

    /**
     * Callbacks for GL thread operations.
     */
    interface Callbacks {
        fun queueEventOnGlThread(event: () -> Unit)
        fun requestRender()
    }

    @Volatile
    var visible: Boolean = false
        set(value) {
            field = value
            if (value) {
                // When becoming visible, execute any pending reload
                if (hasPendingReload) {
                    hasPendingReload = false
                    reloadCurrentArtwork(ReloadImmediate)
                }
            }
        }

    @Volatile
    private var isLoading = false

    @Volatile
    private var hasPendingReload = false

    private var throttleJob: Job? = null

    /**
     * Abstract method to open the current wallpaper for loading.
     * Implemented by the service to integrate with queue system.
     *
     * @return ImageLoader for the current wallpaper
     */
    protected abstract suspend fun openDownloadedCurrentArtwork(): ImageLoader

    /**
     * Reload the current wallpaper.
     *
     * @param reason Reload reason (immediate or queued)
     */
    fun reloadCurrentArtwork(reason: ReloadReason = ReloadQueued) {
        Log.d(TAG, "reloadCurrentArtwork: reason=$reason, visible=$visible, loading=$isLoading")

        // If not visible, defer reload until visible
        if (!visible) {
            hasPendingReload = true
            return
        }

        // If already loading, skip
        if (isLoading) {
            Log.d(TAG, "Already loading, skipping reload")
            return
        }

        when (reason) {
            is ReloadImmediate -> {
                // Cancel any throttled reload and execute immediately
                throttleJob?.cancel()
                executeReload()
            }
            is ReloadQueued -> {
                // Throttle reloads to avoid excessive processing
                throttleJob?.cancel()
                throttleJob = controllerScope.launch {
                    delay(RELOAD_THROTTLE_MS)
                    executeReload()
                }
            }
        }
    }

    /**
     * Execute wallpaper reload.
     */
    private fun executeReload() {
        isLoading = true

        controllerScope.launch {
            try {
                // Get image loader from service
                val loader = openDownloadedCurrentArtwork()

                // Queue wallpaper loading on renderer
                // Renderer will load bitmap in background and upload to GPU on GL thread
                renderer.queueWallpaper(loader)

            } catch (e: Exception) {
                Log.e(TAG, "Error reloading wallpaper", e)
            } finally {
                isLoading = false
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "onStart")
        // Lifecycle started - ready for operation
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "onStop")
        // Lifecycle stopped - pause operations
        throttleJob?.cancel()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Log.d(TAG, "onDestroy")
        throttleJob?.cancel()
    }
}

/**
 * Sealed class representing reload reason.
 */
sealed class ReloadReason

/**
 * Immediate reload (triggered by user action like double-tap or broadcast).
 */
object ReloadImmediate : ReloadReason()

/**
 * Queued reload (throttled to avoid excessive processing).
 */
object ReloadQueued : ReloadReason()
