package com.anthonyla.paperize.service.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.anthonyla.paperize.core.EmptyAlbumException
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.util.setBitmapChecked
import com.anthonyla.paperize.domain.model.PreparedWallpaper
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.usecase.ChangeWallpaperUseCase
import com.anthonyla.paperize.domain.usecase.ReapplyEffectsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WallpaperChangeOutcome(val changed: Boolean = false, val emptyAlbum: Boolean = false)

/** Call while holding WallpaperChangeLock so applying and resetting schedules stay ordered. */
class WallpaperController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val wallpaperManager: WallpaperManager,
    private val prepare: ChangeWallpaperUseCase,
    private val render: ReapplyEffectsUseCase,
    private val settingsRepository: SettingsRepository
) {
    suspend fun change(screen: ScreenType, settings: ScheduleSettings): WallpaperChangeOutcome = when (screen) {
        ScreenType.LIVE -> {
            context.sendBroadcast(Intent(Constants.ACTION_RELOAD_WALLPAPER).setPackage(context.packageName))
            WallpaperChangeOutcome(changed = true)
        }
        ScreenType.HOME -> changeSelected(settings.homeAlbumId, screen)
        ScreenType.LOCK -> changeSelected(settings.lockAlbumId, screen)
        ScreenType.BOTH -> {
            val home = settings.homeAlbumId
            if (home != null && home == settings.lockAlbumId && !settings.separateSchedules) {
                changeSynchronized(home, settings)
            } else {
                combine(changeSelected(home, ScreenType.HOME), changeSelected(settings.lockAlbumId, ScreenType.LOCK))
            }
        }
    }

    private suspend fun changeSelected(albumId: String?, screen: ScreenType): WallpaperChangeOutcome {
        if (albumId == null) return WallpaperChangeOutcome()
        val prepared = prepareOrDisable(albumId, screen) ?: return WallpaperChangeOutcome(emptyAlbum = true)
        applyPrepared(prepared, screen)
        return WallpaperChangeOutcome(changed = true)
    }

    private suspend fun changeSynchronized(albumId: String, settings: ScheduleSettings): WallpaperChangeOutcome {
        val prepared = prepareOrDisable(albumId, ScreenType.BOTH) ?: return WallpaperChangeOutcome(emptyAlbum = true)
        if (settings.sameStaticPresentation()) {
            applyPrepared(prepared, ScreenType.BOTH)
        } else {
            applyPrepared(prepared, ScreenType.HOME)
            val lockBitmap = render(albumId, ScreenType.LOCK, prepared.wallpaperId).getOrThrow()
            applyBitmap(lockBitmap, ScreenType.LOCK) { prepare.complete(prepared, ScreenType.LOCK) }
        }
        return WallpaperChangeOutcome(changed = true)
    }

    private suspend fun prepareOrDisable(albumId: String, screen: ScreenType): PreparedWallpaper? {
        try {
            return prepare(albumId, if (screen == ScreenType.BOTH) ScreenType.HOME else screen).getOrThrow()
        } catch (_: EmptyAlbumException) {
            settingsRepository.clearEmptyAlbumSelection(albumId, screen)
            return null
        }
    }

    private suspend fun applyPrepared(prepared: PreparedWallpaper, screen: ScreenType) {
        var accepted = false
        try {
            currentCoroutineContext().ensureActive()
            wallpaperManager.setBitmapChecked(prepared.bitmap, screen.flags())
            accepted = true
            // Once Android accepts the bitmap, cancellation must not leave our current item stale.
            withContext(NonCancellable) {
                screen.staticScreens().forEach { prepare.complete(prepared, it) }
            }
        } catch (e: Exception) {
            if (!accepted) withContext(NonCancellable) { prepare.restore(prepared) }
            throw e
        } finally {
            prepared.bitmap.recycle()
        }
    }

    suspend fun applySpecific(albumId: String, wallpaperId: String, screen: ScreenType, settings: ScheduleSettings) {
        val targets = if (screen == ScreenType.BOTH && !settings.sameStaticPresentation()) {
            listOf(ScreenType.HOME, ScreenType.LOCK)
        } else listOf(screen)
        for (target in targets) {
            val renderScreen = if (target == ScreenType.BOTH) ScreenType.HOME else target
            val bitmap = render(albumId, renderScreen, wallpaperId).getOrThrow()
            applyBitmap(bitmap, target) {
                target.staticScreens().forEach {
                    prepare.completeSpecific(albumId, it, wallpaperId, settings.shuffleEnabled)
                }
            }
        }
    }

    suspend fun reapply(screen: ScreenType, settings: ScheduleSettings): WallpaperChangeOutcome {
        if (screen == ScreenType.LIVE) return WallpaperChangeOutcome()
        var outcome = WallpaperChangeOutcome()
        for (target in screen.staticScreens()) {
            val albumId = if (target == ScreenType.HOME) settings.homeAlbumId else settings.lockAlbumId
            if (albumId == null) continue
            val bitmap = render(albumId, target).getOrNull()
            val result = if (bitmap == null) changeSelected(albumId, target) else {
                applyBitmap(bitmap, target)
                WallpaperChangeOutcome(changed = true)
            }
            outcome = combine(outcome, result)
        }
        return outcome
    }

    private suspend fun applyBitmap(bitmap: Bitmap, screen: ScreenType, onApplied: suspend () -> Unit = {}) {
        try {
            currentCoroutineContext().ensureActive()
            wallpaperManager.setBitmapChecked(bitmap, screen.flags())
            withContext(NonCancellable) { onApplied() }
        } finally {
            bitmap.recycle()
        }
    }

    private fun combine(first: WallpaperChangeOutcome, second: WallpaperChangeOutcome) = WallpaperChangeOutcome(
        changed = first.changed || second.changed, emptyAlbum = first.emptyAlbum || second.emptyAlbum
    )

    private fun ScheduleSettings.sameStaticPresentation() =
        homeEffects == lockEffects && homeScalingType == lockScalingType && !homeScrollingEnabled

    private fun ScreenType.staticScreens() = if (this == ScreenType.BOTH) listOf(ScreenType.HOME, ScreenType.LOCK) else listOf(this)

    private fun ScreenType.flags(): Int = when (this) {
        ScreenType.HOME -> WallpaperManager.FLAG_SYSTEM
        ScreenType.LOCK -> WallpaperManager.FLAG_LOCK
        ScreenType.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        ScreenType.LIVE -> error("Live wallpaper is applied by its engine")
    }
}
