package com.anthonyla.paperize.domain.usecase

import android.util.Log
import com.anthonyla.paperize.core.EmptyAlbumException
import com.anthonyla.paperize.core.NoValidWallpaperException
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.runCatchingCancellable
import com.anthonyla.paperize.domain.model.PreparedWallpaper
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.domain.source.WallpaperRenderer
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Prepares the next wallpaper without marking it as current.
 *
 * The queue item is dequeued while it is decoded, but callers must invoke [complete] only after
 * WallpaperManager confirms the bitmap was applied. If the platform rejects it, [restore] puts
 * the item back at the front of the queue.
 */
class ChangeWallpaperUseCase @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val settingsRepository: SettingsRepository,
    private val renderer: WallpaperRenderer
) {
    suspend operator fun invoke(albumId: String, screenType: ScreenType): PreparedWallpaper {
        val settings = settingsRepository.getScheduleSettings()
        repeat(Constants.MAX_WALLPAPER_LOAD_RETRIES) {
            currentCoroutineContext().ensureActive()
            val candidate = dequeueNext(albumId, screenType, settings.shuffleEnabled)
            try {
                val bitmap = renderer.render(candidate, screenType, settings)
                if (bitmap != null) {
                    return PreparedWallpaper(bitmap, albumId, screenType, candidate.id, settings.shuffleEnabled)
                }
            } catch (e: CancellationException) {
                // Cancelling preparation must not consume the selected queue item.
                withContext(NonCancellable) {
                    try {
                        wallpaperRepository.restoreWallpaperToQueueFront(albumId, screenType, candidate.id)
                    } catch (restoreError: Exception) {
                        Log.e(TAG, "Failed to restore cancelled wallpaper", restoreError)
                    }
                }
                throw e
            } catch (_: Exception) {
                // Unreadable images are skipped for this cycle; refresh owns permanent pruning.
            }
        }
        throw NoValidWallpaperException()
    }

    /** Dequeue the next item, rebuilding an empty queue once. Throws [EmptyAlbumException] if none remain. */
    suspend fun dequeueNext(albumId: String, screenType: ScreenType, shuffle: Boolean): Wallpaper =
        wallpaperRepository.getAndDequeueWallpaper(albumId, screenType) ?: run {
            wallpaperRepository.ensureWallpaperQueue(albumId, screenType, shuffle)
            wallpaperRepository.getAndDequeueWallpaper(albumId, screenType)
                ?: throw EmptyAlbumException()
        }

    /**
     * Record a successfully applied wallpaper and keep the target screen queue in sync.
     *
     * [screenType] can differ from the prepared queue when one HOME item was atomically applied to
     * both screens. The exact item is removed from LOCK rather than blindly dequeuing its head.
     */
    suspend fun complete(
        prepared: PreparedWallpaper,
        screenType: ScreenType = prepared.screenType
    ) = completeSpecific(
        albumId = prepared.albumId,
        screenType = screenType,
        wallpaperId = prepared.wallpaperId,
        shuffle = prepared.shuffle
    )

    /** Record a user-selected wallpaper and remove that exact item from the next-change queue. */
    suspend fun completeSpecific(
        albumId: String,
        screenType: ScreenType,
        wallpaperId: String,
        shuffle: Boolean
    ) {
        runCatchingCancellable { wallpaperRepository.setCurrentWallpaper(albumId, screenType, wallpaperId) }
            .onFailure {
                Log.e(TAG, "Applied wallpaper could not be recorded as current", it)
                return
            }

        runCatchingCancellable {
            wallpaperRepository.ensureWallpaperQueue(albumId, screenType, shuffle)
            // Build first when this is the first synchronized use of a screen queue, then remove
            // the exact applied item. This prevents the just-applied wallpaper from being
            // reintroduced at the head of a freshly built queue.
            wallpaperRepository.removeWallpaperFromQueue(albumId, screenType, wallpaperId)
        }.onFailure { Log.w(TAG, "Queue sync failed; it will rebuild on the next change", it) }
    }

    /** Restore a prepared item after WallpaperManager rejected it. */
    suspend fun restore(prepared: PreparedWallpaper) {
        runCatchingCancellable {
            wallpaperRepository.restoreWallpaperToQueueFront(prepared.albumId, prepared.screenType, prepared.wallpaperId)
        }.onFailure { Log.e(TAG, "Failed to restore rejected wallpaper to its queue", it) }
    }

    private companion object {
        const val TAG = "ChangeWallpaperUseCase"
    }
}
