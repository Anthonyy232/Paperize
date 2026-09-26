package com.anthonyla.paperize.domain.usecase

import android.net.Uri
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.WallpaperSourceType
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshFolderUseCaseTest {
    private val albums = mockk<AlbumRepository>()
    private val wallpapers = mockk<WallpaperRepository>()
    private val refresh = RefreshFolderUseCase(albums, wallpapers)
    private val tree = mockk<Uri>()
    private val existing = Wallpaper.empty("existing", "album").copy(uri = "existing", displayOrder = 9)
    private val folder = MutableStateFlow<Folder?>(
        Folder.empty("folder", "album").copy(uri = "tree", wallpapers = listOf(existing))
    )

    @Before fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse("tree") } returns tree
        every { albums.getFolderById("folder") } returns folder
        coEvery { albums.addWallpapersToAlbum(any(), any(), any()) } returns Result.Success(Unit)
    }

    @After fun tearDown() = unmockkAll()

    @Test fun `new images append with correct membership and source metadata`() = runTest {
        val discovered = Wallpaper.empty("new").copy(uri = "new", sourceType = WallpaperSourceType.FOLDER)
        coEvery { wallpapers.scanFolderForWallpapers(tree) } returns Result.Success(listOf(existing, discovered))
        assertEquals(Result.Success(1), refresh("folder"))
        coVerify { albums.addWallpapersToAlbum("album", match {
            it.size == 1 && it.single().let { image ->
                image.albumId == "album" && image.folderId == "folder" && image.displayOrder == 10 &&
                    image.sourceType == WallpaperSourceType.FOLDER
            }
        }, any()) }
    }

    @Test fun `unchanged folder avoids unnecessary writes`() = runTest {
        coEvery { wallpapers.scanFolderForWallpapers(tree) } returns Result.Success(listOf(existing))
        assertEquals(Result.Success(0), refresh("folder"))
        coVerify(exactly = 0) { albums.addWallpapersToAlbum(any(), any(), any()) }
    }

    @Test fun `inaccessible folder preserves its current contents`() = runTest {
        coEvery { wallpapers.scanFolderForWallpapers(tree) } returns Result.Error(SecurityException("No access"))
        assertTrue(refresh("folder") is Result.Error)
        coVerify(exactly = 0) { albums.addWallpapersToAlbum(any(), any(), any()) }
        coVerify(exactly = 0) { albums.removeFolderFromAlbum(any(), any()) }
    }

    @Test fun `cancelled refresh propagates cancellation`() = runTest {
        coEvery { wallpapers.scanFolderForWallpapers(tree) } throws CancellationException()
        try { refresh("folder"); fail("Expected cancellation") } catch (_: CancellationException) { }
        coVerify(exactly = 0) { albums.addWallpapersToAlbum(any(), any(), any()) }
    }

    @Test fun `folder deleted during a scan is not recreated`() = runTest {
        coEvery { wallpapers.scanFolderForWallpapers(tree) } coAnswers {
            folder.value = null
            Result.Success(listOf(Wallpaper.empty("new")))
        }
        assertEquals(Result.Success(0), refresh("folder"))
        coVerify(exactly = 0) { albums.addWallpapersToAlbum(any(), any(), any()) }
    }

    @Test fun `overlapping manual and background refreshes import a discovery once`() = runTest {
        val releaseScan = CompletableDeferred<Unit>()
        var scans = 0
        coEvery { wallpapers.scanFolderForWallpapers(tree) } coAnswers {
            scans++
            releaseScan.await()
            Result.Success(listOf(existing, Wallpaper.empty("new").copy(uri = "new")))
        }
        coEvery { albums.addWallpapersToAlbum(any(), any(), any()) } coAnswers {
            folder.value = folder.value!!.copy(wallpapers = listOf(existing, Wallpaper.empty("new").copy(uri = "new")))
            Result.Success(Unit)
        }
        val manual = async { refresh("folder") }
        val background = async { refresh("folder") }
        runCurrent()
        assertEquals(2, scans)
        releaseScan.complete(Unit)
        assertEquals(Result.Success(1), manual.await())
        assertEquals(Result.Success(0), background.await())
        coVerify(exactly = 1) { albums.addWallpapersToAlbum(any(), any(), any()) }
    }
}
