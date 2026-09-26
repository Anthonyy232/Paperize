package com.anthonyla.paperize.presentation.screens.home

import android.content.Context
import androidx.lifecycle.ViewModelStore
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.domain.model.AppSettings
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.service.worker.WallpaperScheduler
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val settings = mockk<SettingsRepository>()
    private val albums = mockk<AlbumRepository>()
    private val wallpapers = mockk<WallpaperRepository>(relaxed = true)
    private val scheduler = mockk<WallpaperScheduler>(relaxed = true)
    private val stored = MutableStateFlow(ScheduleSettings(homeEnabled = true, homeAlbumId = "old"))
    private val store = ViewModelStore()
    private lateinit var viewModel: HomeViewModel

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { albums.getAlbumSummaries() } returns flowOf(emptyList())
        every { settings.getScheduleSettingsFlow() } returns stored
        every { settings.getAppSettingsFlow() } returns flowOf(AppSettings())
        every { settings.getWallpaperModeFlow() } returns flowOf(WallpaperMode.STATIC)
        coEvery { settings.getWallpaperMode() } returns WallpaperMode.STATIC
        coEvery { settings.getScheduleSettings() } answers { stored.value }
        coEvery { settings.updateEnableChanger(any()) } answers { stored.value = stored.value.copy(enableChanger = firstArg()) }
        coEvery { settings.updateHomeAlbumId(any()) } answers { stored.value = stored.value.copy(homeAlbumId = firstArg()) }
        coEvery { settings.updateScheduleSettings(any<(ScheduleSettings) -> ScheduleSettings>()) } answers {
            stored.value = firstArg<(ScheduleSettings) -> ScheduleSettings>()(stored.value)
            stored.value
        }
        every { wallpapers.getCurrentWallpaperFlow(any(), any()) } returns flowOf(null)
        viewModel = HomeViewModel(mockk<Context>(), albums, mockk(), settings, scheduler, wallpapers)
        store.put("home", viewModel)
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test fun `delayed effect edits preserve a newer pause and album selection`() = runTest {
        stored.value = stored.value.copy(enableChanger = true)
        val draft = stored.value.copy(homeEffects = stored.value.homeEffects.copy(blurPercentage = 60))
        viewModel.updateScheduleSettingsDebounced(draft)
        viewModel.toggleWallpaperChanger(false)
        runCurrent()
        viewModel.selectHomeAlbum(AlbumSummary.empty("new"))
        advanceUntilIdle()

        assertFalse(stored.value.enableChanger)
        assertEquals("new", stored.value.homeAlbumId)
        assertEquals(60, stored.value.homeEffects.blurPercentage)
    }

    @Test fun `disabling one screen clears only its current selection`() = runTest {
        stored.value = stored.value.copy(lockEnabled = true, lockAlbumId = "lock")
        viewModel.updateScheduleSettings(stored.value.copy(homeEnabled = false))
        advanceUntilIdle()
        assertNull(stored.value.homeAlbumId)
        assertEquals("lock", stored.value.lockAlbumId)
    }

    @Test fun `rapid edits keep scheduling side effects in persistence order`() = runTest {
        val firstSchedule = CompletableDeferred<Unit>()
        val scheduled = mutableListOf<Boolean>()
        coEvery { scheduler.updateSchedules(any(), any(), any()) } coAnswers {
            val snapshot = firstArg<ScheduleSettings>()
            if (!snapshot.enableChanger) firstSchedule.await()
            scheduled.add(snapshot.enableChanger)
        }
        viewModel.updateScheduleSettings(stored.value.copy(homeIntervalMinutes = 45))
        runCurrent()
        viewModel.toggleWallpaperChanger(true, onlyIfNotScheduled = true)
        runCurrent()
        assertFalse(stored.value.enableChanger)
        firstSchedule.complete(Unit)
        advanceUntilIdle()
        assertTrue(stored.value.enableChanger)
        assertEquals(listOf(false, true), scheduled)
    }

    @Test fun `effect edits do not restart preview subscriptions but album changes do`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.currentHomeWallpaperUri.collect() }
        runCurrent()
        stored.value = stored.value.copy(homeEffects = stored.value.homeEffects.copy(blurPercentage = 60))
        runCurrent()
        verify(exactly = 1) { wallpapers.getCurrentWallpaperFlow("old", ScreenType.HOME) }
        verify(exactly = 1) { wallpapers.getCurrentWallpaperFlow("old", ScreenType.BOTH) }

        stored.value = stored.value.copy(homeAlbumId = "new")
        runCurrent()
        verify(exactly = 1) { wallpapers.getCurrentWallpaperFlow("new", ScreenType.HOME) }
        verify(exactly = 1) { wallpapers.getCurrentWallpaperFlow("new", ScreenType.BOTH) }
    }
}
