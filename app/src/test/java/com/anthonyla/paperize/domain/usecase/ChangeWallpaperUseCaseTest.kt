package com.anthonyla.paperize.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import com.anthonyla.paperize.core.EmptyAlbumException
import com.anthonyla.paperize.core.NoValidWallpaperException
import com.anthonyla.paperize.core.constants.Constants
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
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeWallpaperUseCaseTest {

    private val repository = mockk<WallpaperRepository>(relaxed = true)
    private val renderer = mockk<com.anthonyla.paperize.core.util.WallpaperRenderer>()
    private val useCase = ChangeWallpaperUseCase(
        context = mockk<Context>(relaxed = true),
        wallpaperRepository = repository,
        settingsRepository = mockk<SettingsRepository>(relaxed = true),
        renderer = renderer
    )

    @Test
    fun `complete records current then synchronizes exact queue item`() = runTest {
        val prepared = preparedWallpaper()
        coEvery {
            repository.ensureWallpaperQueue(prepared.albumId, ScreenType.LOCK, false)
        } returns Result.Success(Unit)

        useCase.complete(prepared, ScreenType.LOCK)

        coVerifyOrder {
            repository.setCurrentWallpaper(
                prepared.albumId,
                ScreenType.LOCK,
                prepared.wallpaperId
            )
            repository.ensureWallpaperQueue(prepared.albumId, ScreenType.LOCK, false)
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

    @Test
    fun `complete specific records selected item and respects shuffle`() = runTest {
        coEvery {
            repository.ensureWallpaperQueue("album", ScreenType.HOME, true)
        } returns Result.Success(Unit)

        useCase.completeSpecific("album", ScreenType.HOME, "selected", shuffle = true)

        coVerifyOrder {
            repository.setCurrentWallpaper("album", ScreenType.HOME, "selected")
            repository.ensureWallpaperQueue("album", ScreenType.HOME, true)
            repository.removeWallpaperFromQueue("album", ScreenType.HOME, "selected")
        }
    }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun `cancelled rendering restores the dequeued item without retrying`() = runTest {
        val wallpaper = com.anthonyla.paperize.domain.model.Wallpaper.empty("image", "album")
        coEvery { repository.getAndDequeueWallpaper("album", ScreenType.HOME) } returns wallpaper
        coEvery { renderer.render(wallpaper, ScreenType.HOME, any()) } throws kotlinx.coroutines.CancellationException()
        try {
            useCase("album", ScreenType.HOME)
        } finally {
            coVerify(exactly = 1) { repository.getAndDequeueWallpaper("album", ScreenType.HOME) }
            coVerify(exactly = 1) { repository.restoreWallpaperToQueueFront("album", ScreenType.HOME, "image") }
        }
    }

    @Test
    fun `unreadable single image album is not reported as empty`() = runTest {
        val wallpaper = com.anthonyla.paperize.domain.model.Wallpaper.empty("image", "album")
        var dequeues = 0
        coEvery { repository.getAndDequeueWallpaper("album", ScreenType.HOME) } answers {
            if (dequeues++ % 2 == 0) null else wallpaper
        }
        coEvery { repository.ensureWallpaperQueue("album", ScreenType.HOME, any()) } returns Result.Success(Unit)
        coEvery { renderer.render(wallpaper, ScreenType.HOME, any()) } returns null

        assertTrue((useCase("album", ScreenType.HOME) as Result.Error).exception is NoValidWallpaperException)
        coVerify(exactly = Constants.MAX_WALLPAPER_LOAD_RETRIES) { renderer.render(wallpaper, ScreenType.HOME, any()) }
    }

    @Test
    fun `empty album stops after rebuilding once`() = runTest {
        coEvery { repository.getAndDequeueWallpaper("album", ScreenType.HOME) } returns null
        coEvery { repository.ensureWallpaperQueue("album", ScreenType.HOME, any()) } returns Result.Success(Unit)

        assertTrue((useCase("album", ScreenType.HOME) as Result.Error).exception is EmptyAlbumException)
        coVerify(exactly = 1) { repository.ensureWallpaperQueue("album", ScreenType.HOME, any()) }
        coVerify(exactly = 0) { renderer.render(any(), any(), any()) }
    }

    private fun preparedWallpaper() = PreparedWallpaper(
        bitmap = mockk<Bitmap>(relaxed = true),
        albumId = "album",
        screenType = ScreenType.HOME,
        wallpaperId = "wallpaper",
        shuffle = false
    )
}
