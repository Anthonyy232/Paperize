package com.anthonyla.paperize.presentation.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModelStore
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.domain.model.AppSettings
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.service.worker.WallpaperScheduler
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val settings = mockk<SettingsRepository>(relaxed = true)
    private val albums = mockk<AlbumRepository>()
    private val scheduler = mockk<WallpaperScheduler>(relaxed = true)
    private val store = ViewModelStore()
    private lateinit var viewModel: SettingsViewModel

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { settings.getAppSettingsFlow() } returns flowOf(AppSettings())
        every { settings.getWallpaperModeFlow() } returns flowOf(WallpaperMode.STATIC)
        viewModel = SettingsViewModel(settings, albums, scheduler)
        store.put("settings", viewModel)
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test fun `mode switch waits for deletion and persistence before navigating`() = runTest {
        val deletion = CompletableDeferred<Result<Unit>>()
        coEvery { albums.deleteAllAlbums() } coAnswers { deletion.await() }
        var completed = false
        viewModel.switchWallpaperMode(WallpaperMode.LIVE) { completed = true }
        runCurrent()
        assertTrue(viewModel.isResetting.value)
        assertFalse(completed)
        viewModel.switchWallpaperMode(WallpaperMode.STATIC)
        deletion.complete(Result.Success(Unit))
        advanceUntilIdle()
        coVerifyOrder {
            settings.updateEnableChanger(false)
            scheduler.cancelAllWallpaperChanges()
            albums.deleteAllAlbums()
            settings.clearScheduleSettings()
            settings.updateWallpaperMode(WallpaperMode.LIVE)
        }
        coVerify(exactly = 1) { albums.deleteAllAlbums() }
        assertTrue(completed)
        assertFalse(viewModel.isResetting.value)
    }

    @Test fun `failed deletion preserves settings and allows retry`() = runTest {
        coEvery { albums.deleteAllAlbums() } returns Result.Error(IllegalStateException("Database unavailable"))
        var completed = false
        viewModel.switchWallpaperMode(WallpaperMode.LIVE) { completed = true }
        advanceUntilIdle()
        assertTrue(viewModel.resetFailed.value)
        assertFalse(completed)
        coVerify(exactly = 0) { settings.clearScheduleSettings(); settings.updateWallpaperMode(any()) }

        coEvery { albums.deleteAllAlbums() } returns Result.Success(Unit)
        viewModel.resetAllData()
        advanceUntilIdle()
        assertFalse(viewModel.resetFailed.value)
        coVerify(exactly = 1) { settings.clearAllSettings() }
    }
}
