package com.anthonyla.paperize.presentation.screens.sort

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.navigation.toRoute
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.presentation.common.navigation.SortRoute
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SortViewModelTest {
    private val albums = mockk<AlbumRepository>()
    private val store = ViewModelStore()
    private val albumReady = CompletableDeferred<Album>()
    private lateinit var viewModel: SortViewModel

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        val savedState = SavedStateHandle()
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every { savedState.toRoute<SortRoute>() } returns SortRoute("album")
        every { albums.getAlbumById("album") } returns flow { emit(albumReady.await()) }
        viewModel = SortViewModel(savedState, albums)
        store.put("sort", viewModel)
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test fun `save waits for loading and completion and ignores duplicate requests`() = runTest {
        viewModel.saveChanges()
        runCurrent()
        coVerify(exactly = 0) { albums.reorderAlbum(any(), any(), any()) }
        albumReady.complete(Album.empty("album"))
        runCurrent()
        val completion = CompletableDeferred<Result<Unit>>()
        coEvery { albums.reorderAlbum(any(), any(), any()) } coAnswers { completion.await() }
        viewModel.saveChanges()
        runCurrent()
        assertTrue(viewModel.state.value.isSaving)
        assertFalse(viewModel.state.value.saved)
        viewModel.saveChanges()
        completion.complete(Result.Success(Unit))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.isSaving)
        coVerify(exactly = 1) { albums.reorderAlbum("album", emptyList(), emptyList()) }
    }

    @Test fun `failed save keeps screen open and permits retry`() = runTest {
        albumReady.complete(Album.empty("album"))
        runCurrent()
        coEvery { albums.reorderAlbum(any(), any(), any()) } returns Result.Error(IllegalStateException())
        viewModel.saveChanges()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.isSaving)
        assertNotNull(viewModel.state.value.error)
        coEvery { albums.reorderAlbum(any(), any(), any()) } returns Result.Success(Unit)
        viewModel.saveChanges()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.saved)
        assertNull(viewModel.state.value.error)
    }
}
