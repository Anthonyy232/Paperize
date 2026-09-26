package com.anthonyla.paperize.service.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import com.anthonyla.paperize.core.EmptyAlbumException
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.domain.model.PreparedWallpaper
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.usecase.ChangeWallpaperUseCase
import com.anthonyla.paperize.domain.usecase.ReapplyEffectsUseCase
import io.mockk.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class WallpaperControllerTest {
    private val manager = mockk<WallpaperManager>()
    private val prepare = mockk<ChangeWallpaperUseCase>(relaxed = true)
    private val render = mockk<ReapplyEffectsUseCase>()
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val controller = WallpaperController(mockk<Context>(), manager, prepare, render, settingsRepository)
    private val settings = ScheduleSettings(homeAlbumId = "album", lockAlbumId = "album")
    private val bitmap = mockk<Bitmap>(relaxed = true)
    private val prepared = PreparedWallpaper(bitmap, "album", ScreenType.HOME, "image", false)

    @Test fun `matching screens use one write and commit both only after acceptance`() = runTest {
        coEvery { prepare("album", ScreenType.HOME) } returns Result.Success(prepared)
        every { manager.setBitmap(bitmap, null, true, 3) } returns 1
        assertEquals(WallpaperChangeOutcome(changed = true), controller.change(ScreenType.BOTH, settings))
        coVerifyOrder {
            manager.setBitmap(bitmap, null, true, 3)
            prepare.complete(prepared, ScreenType.HOME)
            prepare.complete(prepared, ScreenType.LOCK)
            bitmap.recycle()
        }
        coVerify(exactly = 0) { prepare.restore(any()) }
    }

    @Test fun `platform rejection restores prepared item and releases bitmap`() = runTest {
        coEvery { prepare("album", ScreenType.HOME) } returns Result.Success(prepared)
        every { manager.setBitmap(bitmap, null, true, 1) } returns 0
        try { controller.change(ScreenType.HOME, settings); fail("Expected rejection") } catch (_: IOException) { }
        coVerify(exactly = 1) { prepare.restore(prepared) }
        coVerify(exactly = 0) { prepare.complete(any(), any()) }
        verify(exactly = 1) { bitmap.recycle() }
    }

    @Test fun `failed lock write preserves successful home commit`() = runTest {
        val lockBitmap = mockk<Bitmap>(relaxed = true)
        coEvery { prepare("album", ScreenType.HOME) } returns Result.Success(prepared)
        coEvery { render("album", ScreenType.LOCK, "image") } returns Result.Success(lockBitmap)
        every { manager.setBitmap(bitmap, null, true, 1) } returns 1
        every { manager.setBitmap(lockBitmap, null, true, 2) } throws IOException("Rejected")
        try {
            controller.change(ScreenType.BOTH, settings.copy(homeScrollingEnabled = true))
            fail("Expected rejection")
        } catch (_: IOException) { }
        coVerify(exactly = 1) { prepare.complete(prepared, ScreenType.HOME) }
        coVerify(exactly = 0) { prepare.complete(prepared, ScreenType.LOCK) }
        coVerify(exactly = 0) { prepare.restore(any()) }
        verify { bitmap.recycle(); lockBitmap.recycle() }
    }

    @Test fun `cancellation after platform acceptance still records the applied wallpaper`() = runTest {
        coEvery { prepare("album", ScreenType.HOME) } returns Result.Success(prepared)
        lateinit var change: Job
        every { manager.setBitmap(bitmap, null, true, 1) } answers { change.cancel(); 1 }
        change = launch { controller.change(ScreenType.HOME, settings) }
        change.join()
        assertTrue(change.isCancelled)
        coVerify(exactly = 1) { prepare.complete(prepared, ScreenType.HOME) }
        coVerify(exactly = 0) { prepare.restore(any()) }
        verify { bitmap.recycle() }
    }

    @Test fun `empty home album does not disable a valid lock album`() = runTest {
        val active = settings.copy(lockAlbumId = "lock", enableChanger = true, homeEnabled = true, lockEnabled = true)
        coEvery { prepare("album", ScreenType.HOME) } returns Result.Error(EmptyAlbumException("Empty"))
        val lock = prepared.copy(albumId = "lock", screenType = ScreenType.LOCK)
        coEvery { prepare("lock", ScreenType.LOCK) } returns Result.Success(lock)
        every { manager.setBitmap(bitmap, null, true, 2) } returns 1
        assertEquals(WallpaperChangeOutcome(changed = true, emptyAlbum = true), controller.change(ScreenType.BOTH, active))
        coVerify { settingsRepository.clearEmptyAlbumSelection("album", ScreenType.HOME) }
    }
    @Test fun `specific selection commits both screens without advancing a queue`() = runTest {
        coEvery { render("album", ScreenType.HOME, "selected") } returns Result.Success(bitmap)
        every { manager.setBitmap(bitmap, null, true, 3) } returns 1
        controller.applySpecific("album", "selected", ScreenType.BOTH, settings)
        coVerify(exactly = 0) { prepare(any(), any()) }
        coVerify { prepare.completeSpecific("album", ScreenType.HOME, "selected", false) }
        coVerify { prepare.completeSpecific("album", ScreenType.LOCK, "selected", false) }
        verify { bitmap.recycle() }
    }

    @Test fun `reapplying a readable current image does not consume the next item`() = runTest {
        coEvery { render("album", ScreenType.HOME, null) } returns Result.Success(bitmap)
        every { manager.setBitmap(bitmap, null, true, 1) } returns 1
        assertTrue(controller.reapply(ScreenType.HOME, settings).changed)
        coVerify(exactly = 0) { prepare(any(), any()) }
        coVerify(exactly = 0) { prepare.complete(any(), any()) }
        verify { bitmap.recycle() }
    }

}
