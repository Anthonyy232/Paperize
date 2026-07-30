package com.anthonyla.paperize.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.domain.model.PreparedWallpaper
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChangeWallpaperUseCaseTest {

    private val repository = mockk<WallpaperRepository>(relaxed = true)
    private val useCase = ChangeWallpaperUseCase(
        context = mockk<Context>(relaxed = true),
        wallpaperRepository = repository,
        settingsRepository = mockk<SettingsRepository>(relaxed = true)
    )

    @Test
    fun `complete records current then synchronizes exact queue item`() = runTest {
        val prepared = preparedWallpaper()
        coEvery {
            repository.getNextWallpaperInQueue(prepared.albumId, ScreenType.LOCK)
        } returns null
        coEvery {
            repository.buildWallpaperQueue(prepared.albumId, ScreenType.LOCK, false)
        } returns Result.Success(Unit)

        useCase.complete(prepared, ScreenType.LOCK)

        coVerifyOrder {
            repository.setCurrentWallpaper(
                prepared.albumId,
                ScreenType.LOCK,
                prepared.wallpaperId
            )
            repository.getNextWallpaperInQueue(prepared.albumId, ScreenType.LOCK)
            repository.buildWallpaperQueue(prepared.albumId, ScreenType.LOCK, false)
            repository.removeWallpaperFromQueue(
                prepared.albumId,
                ScreenType.LOCK,
                prepared.wallpaperId
            )
        }
    }

    @Test
    fun `restore returns rejected item to its original queue`() = runTest {
        val prepared = preparedWallpaper()

        useCase.restore(prepared)

        coVerify(exactly = 1) {
            repository.restoreWallpaperToQueueFront(
                prepared.albumId,
                prepared.screenType,
                prepared.wallpaperId
            )
        }
        coVerify(exactly = 0) {
            repository.setCurrentWallpaper(any(), any(), any())
        }
    }

    private fun preparedWallpaper() = PreparedWallpaper(
        bitmap = mockk<Bitmap>(relaxed = true),
        albumId = "album",
        screenType = ScreenType.HOME,
        wallpaperId = "wallpaper",
        shuffle = false
    )
}
