package com.anthonyla.paperize.service.livewallpaper.renderer

import android.util.Log
import com.anthonyla.paperize.core.constants.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** All calls run on the engine's main dispatcher; its scope owns cancellation. */
class PaperizeRenderController(
    private val queueWallpaper: (ImageLoader, Boolean) -> Unit,
    private val controllerScope: CoroutineScope,
    private val openCurrentArtwork: suspend () -> ImageLoader
) {
    var visible = false
        set(value) {
            field = value
            if (value && hasPendingReload) reloadCurrentArtwork(immediate = true, force = pendingForce)
        }

    private var isLoading = false
    private var hasPendingReload = false
    private var pendingForce = false
    private var throttleJob: Job? = null

    /**
     * [force] runs the reload while the engine is hidden. A manual change requested from the app
     * arrives while the launcher is covered, and deferring it until the engine becomes visible made
     * the button look broken: the wallpaper only advanced once the user returned home.
     */
    fun reloadCurrentArtwork(immediate: Boolean = false, force: Boolean = false) {
        if ((!visible && !force) || isLoading) {
            hasPendingReload = true
            pendingForce = pendingForce || force
            return
        }

        hasPendingReload = false
        pendingForce = false
        throttleJob?.cancel()
        if (immediate) {
            executeReload()
        } else {
            throttleJob = controllerScope.launch {
                delay(Constants.RELOAD_THROTTLE_MS)
                if (visible || force) executeReload() else hasPendingReload = true
            }
        }
    }

    /** Prepare the next wallpaper while the screen is dark, without crossfading. */
    fun forceReloadCurrentArtwork() {
        if (isLoading) return
        hasPendingReload = false
        pendingForce = false
        throttleJob?.cancel()
        executeReload(skipCrossfade = true)
    }

    private fun executeReload(skipCrossfade: Boolean = false) {
        isLoading = true
        controllerScope.launch {
            try {
                val loader = openCurrentArtwork()
                if (isActive && !hasPendingReload) {
                    queueWallpaper(loader, skipCrossfade)
                }
            } catch (e: Exception) {
                if (isActive) Log.e("PaperizeRenderController", "Error reloading wallpaper", e)
            } finally {
                isLoading = false
                if (isActive && hasPendingReload && (visible || pendingForce)) {
                    reloadCurrentArtwork(immediate = true, force = pendingForce)
                }
            }
        }
    }
}
