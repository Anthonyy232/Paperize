package com.anthonyla.paperize.presentation.screens.album_view

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.navigation.toRoute
import com.anthonyla.paperize.presentation.common.navigation.AlbumRoute
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.usecase.DeleteAlbumUseCase
import com.anthonyla.paperize.domain.usecase.ImportWallpapersUseCase
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumViewViewModelTest {
    private val albums = mockk<AlbumRepository>()
    private val imports = mockk<ImportWallpapersUseCase>()
    private val delete = mockk<DeleteAlbumUseCase>()
    private val store = ViewModelStore()
    private lateinit var viewModel: AlbumViewViewModel

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { albums.getAlbumById("album") } returns flowOf(Album.empty("album"))
        val savedState = SavedStateHandle()
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every { savedState.toRoute<AlbumRoute>() } returns AlbumRoute("album")
        viewModel = AlbumViewViewModel(savedState, albums, imports, delete)
        store.put("album", viewModel)
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test fun `failed import restores idle and shows a retry message`() = runTest {
        coEvery { imports.addImages(any(), any(), any()) } throws IllegalStateException("Disk full")
        viewModel.addWallpapers(listOf("image"))
        advanceUntilIdle()
        assertEquals(ImportProgress.Idle, viewModel.importProgress.value)
        assertEquals(R.string.import_failed, viewModel.message.value)
    }

    @Test fun `cancellation restores idle without reporting an error`() = runTest {
        coEvery { imports.addImages(any(), any(), any()) } coAnswers { awaitCancellation() }
        viewModel.addWallpapers(listOf("image"))
        viewModel.addFolder("folder")
        runCurrent()
        viewModel.cancelImport()
        advanceUntilIdle()
        assertEquals(ImportProgress.Idle, viewModel.importProgress.value)
        assertNull(viewModel.message.value)
        coVerify(exactly = 1) { imports.addImages(any(), any(), any()) }
        coVerify(exactly = 0) { imports.addFolder(any(), any(), any(), any()) }
    }

    @Test fun `cancelling before import starts also restores idle`() = runTest {
        viewModel.addWallpapers(listOf("image"))
        viewModel.cancelImport()
        advanceUntilIdle()
        assertEquals(ImportProgress.Idle, viewModel.importProgress.value)
        assertNull(viewModel.message.value)
    }

    @Test fun `navigation waits for successful album deletion and cleanup`() = runTest {
        val completion = CompletableDeferred<Result<Unit>>()
        coEvery { delete("album") } coAnswers { completion.await() }
        viewModel.deleteAlbum()
        runCurrent()
        assertTrue(viewModel.isDeleting.value)
        assertFalse(viewModel.albumDeleted.value)
        viewModel.deleteAlbum()
        completion.complete(Result.Success(Unit))
        advanceUntilIdle()
        assertTrue(viewModel.albumDeleted.value)
        assertFalse(viewModel.isDeleting.value)
        coVerify(exactly = 1) { delete("album") }
        coVerify(exactly = 0) { albums.deleteAlbum(any()) }
    }

    @Test fun `failed deletion keeps the album open`() = runTest {
        coEvery { delete("album") } returns Result.Error(IllegalStateException())
        viewModel.deleteAlbum()
        advanceUntilIdle()
        assertFalse(viewModel.albumDeleted.value)
        assertFalse(viewModel.isDeleting.value)
        assertEquals(R.string.delete_album_failed, viewModel.message.value)
    }

    @Test fun `partial removal keeps failed items selected`() = runTest {
        viewModel.toggleWallpaperSelection("wallpaper")
        viewModel.toggleFolderSelection("folder")
        coEvery { albums.removeWallpapersFromAlbum("album", listOf("wallpaper")) } returns Result.Success(Unit)
        coEvery { albums.removeFolderFromAlbum("album", "folder") } returns Result.Error(IllegalStateException())
        viewModel.deleteSelected()
        advanceUntilIdle()
        assertTrue(viewModel.selectedWallpapers.value.isEmpty())
        assertEquals(setOf("folder"), viewModel.selectedFolders.value)
        assertEquals(R.string.delete_items_failed, viewModel.message.value)
    }

}
