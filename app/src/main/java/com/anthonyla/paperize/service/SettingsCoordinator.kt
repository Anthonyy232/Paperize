package com.anthonyla.paperize.service

import android.content.Context
import android.util.Log
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.runCatchingCancellable
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.service.livewallpaper.PaperizeLiveWallpaperService
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService
import com.anthonyla.paperize.service.worker.WallpaperScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Persists wallpaper settings edits and applies their side effects: schedules, queues, and
 * immediate wallpaper changes. Edits run one at a time, in call order.
 */
@Singleton
class SettingsCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val albumRepository: AlbumRepository,
    private val scheduler: WallpaperScheduler
) {
    private val mutex = Mutex()

    suspend fun selectAlbum(screen: ScreenType, albumId: String?) = mutex.withLock {
        settingsRepository.updateAlbumId(screen, albumId)
        val mode = settingsRepository.getWallpaperMode()
        var settings = settingsRepository.getScheduleSettings()
        if (albumId == null && settings.activeScreens(mode).isEmpty()) {
            settingsRepository.updateEnableChanger(false)
            settings = settings.copy(enableChanger = false)
        }
        scheduler.updateSchedules(settings, mode)
        if (albumId != null && settings.enableChanger && settings.hasRequiredAlbums(mode) && mode == WallpaperMode.STATIC) {
            val active = settings.activeScreens(mode)
            WallpaperChangeService.changeWallpaper(context, if (ScreenType.BOTH in active) ScreenType.BOTH else screen)
        }
    }

    /**
     * Pause or resume rotation. Resuming keeps any countdown that is still scheduled.
     * Returns true when live mode is on but Paperize is not the active live wallpaper.
     */
    suspend fun setChangerEnabled(enabled: Boolean): Boolean = mutex.withLock {
        settingsRepository.updateEnableChanger(enabled)
        val settings = settingsRepository.getScheduleSettings()
        val mode = settingsRepository.getWallpaperMode()
        scheduler.updateSchedules(settings, mode, onlyIfNotScheduled = true)
        enabled && mode == WallpaperMode.LIVE && settings.hasRequiredAlbums(mode) &&
            !PaperizeLiveWallpaperService.isActive(context)
    }

    suspend fun updateScheduleSettings(draft: ScheduleSettings) = mutex.withLock {
        lateinit var previous: ScheduleSettings
        val updated = settingsRepository.updateScheduleSettings { current ->
            previous = current
            // Album selection and pause/resume have their own actions. A delayed effect edit
            // must not overwrite changes those actions made after this draft was captured.
            draft.copy(
                enableChanger = current.enableChanger,
                homeAlbumId = if (draft.homeEnabled) current.homeAlbumId else null,
                lockAlbumId = if (draft.lockEnabled) current.lockAlbumId else null,
                liveAlbumId = current.liveAlbumId
            ).validate()
        }
        if (previous.shuffleEnabled != updated.shuffleEnabled) {
            // Stale queues only delay the new order until they drain; keep the settings edit.
            runCatchingCancellable { wallpaperRepository.clearAllQueues() }
                .onFailure { Log.w(TAG, "Could not clear queues after shuffle change", it) }
        }
        val mode = settingsRepository.getWallpaperMode()
        if (updated.hasSchedulingChanges(previous)) scheduler.updateSchedules(updated, mode)
        if (updated.hasDisplayChanges(previous) && updated.enableChanger &&
            updated.hasRequiredAlbums(mode) && mode == WallpaperMode.STATIC
        ) {
            WallpaperChangeService.reapplyEffects(context)
        }
    }

    /** Change every screen the current settings rotate, or reload the live wallpaper. */
    fun changeWallpaperNow() = WallpaperChangeService.changeWallpaper(context)

    /** Stop rotation if the user replaced Paperize's live wallpaper outside the app. */
    suspend fun disableIfLiveWallpaperReplaced() = mutex.withLock {
        val settings = settingsRepository.getScheduleSettings()
        if (settingsRepository.getWallpaperMode() == WallpaperMode.LIVE && settings.liveAlbumId != null &&
            settings.enableChanger && !PaperizeLiveWallpaperService.isActive(context)
        ) {
            settingsRepository.updateAlbumId(ScreenType.LIVE, null)
            settingsRepository.updateEnableChanger(false)
            scheduler.cancelAllWallpaperChanges()
        }
    }

    /** Delete the library and settings; a non-null [mode] keeps appearance settings and switches mode. */
    suspend fun resetData(mode: WallpaperMode?) = mutex.withLock {
        // Stop both the visible live timer and background jobs before deleting their source.
        settingsRepository.updateEnableChanger(false)
        scheduler.cancelAllWallpaperChanges()
        albumRepository.deleteAllAlbums()
        if (mode == null) {
            settingsRepository.clearAllSettings()
        } else {
            settingsRepository.clearScheduleSettings()
            settingsRepository.updateWallpaperMode(mode)
        }
    }

    private companion object {
        const val TAG = "SettingsCoordinator"
    }
}
