package com.anthonyla.paperize.service.livewallpaper.renderer

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * All calls run on the engine's main dispatcher; its scope owns cancellation.
 * [openCurrentArtwork] returns null while no wallpaper is readable (e.g. storage still mounting
 * after boot), which keeps what is shown. A failed selection is retried with a growing delay.
 */
class PaperizeRenderController(
    private val queueWallpaper: (ImageLoader, Boolean) -> Unit,
    private val controllerScope: CoroutineScope,
    private val openCurrentArtwork: suspend () -> ImageLoader?
) {
    var visible = false
        set(value) {
            field = value
            if (value && hasPendingReload) reloadCurrentArtwork()
        }

    private var isLoading = false
    private var hasPendingReload = false
    private var retryJob: Job? = null
    private var retryDelay = FIRST_RETRY_DELAY

    /** Deferred until the wallpaper is visible and any running selection has finished. */
    fun reloadCurrentArtwork() {
        if (!visible || isLoading) {
            hasPendingReload = true
            return
        }
        hasPendingReload = false
        executeReload()
    }

    /** Prepare the next wallpaper while the screen is dark, without crossfading. */
    fun forceReloadCurrentArtwork() {
        if (isLoading) return
        hasPendingReload = false
        executeReload(skipCrossfade = true)
    }

    private fun executeReload(skipCrossfade: Boolean = false) {
        retryJob?.cancel()
        isLoading = true
        controllerScope.launch {
            try {
                val loader = openCurrentArtwork()
                if (loader == null) {
                    retryLater()
                } else {
                    retryDelay = FIRST_RETRY_DELAY
                    if (isActive && !hasPendingReload) queueWallpaper(loader, skipCrossfade)
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e("PaperizeRenderController", "Error reloading wallpaper", e)
                    retryLater()
                }
            } finally {
                isLoading = false
                if (isActive && hasPendingReload && visible) {
                    reloadCurrentArtwork()
                }
            }
        }
    }

    private fun retryLater() {
        retryJob = controllerScope.launch {
            delay(retryDelay)
            retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY)
            reloadCurrentArtwork()
        }
    }

    private companion object {
        val FIRST_RETRY_DELAY = 1.seconds
        val MAX_RETRY_DELAY = 1.minutes
    }
}
