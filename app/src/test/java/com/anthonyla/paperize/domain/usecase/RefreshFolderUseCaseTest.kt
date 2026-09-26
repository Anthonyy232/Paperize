package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.source.DocumentSource
import com.anthonyla.paperize.domain.source.SourceFolder
import com.anthonyla.paperize.domain.source.SourceImage
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RefreshFolderUseCaseTest {
    private val albums = mockk<AlbumRepository>()
    private val documents = mockk<DocumentSource>()
    private val refresh = RefreshFolderUseCase(albums, documents)

    @Test fun `refresh assigns membership and reports actual inserted count`() = runTest {
        every { albums.getFolderById("folder") } returns flowOf(Folder.empty("folder", "album").copy(uri = "tree"))
        coEvery { documents.readFolder("tree", any()) } returns SourceFolder("Photos", listOf(
            SourceImage("existing", "old.png", 1L), SourceImage("new", "new.png", 2L)
        ))
        coEvery { albums.addWallpapersToAlbum("album", any(), any()) } returns Result.Success(1)
        assertEquals(Result.Success(1), refresh("folder"))
        coVerify { albums.addWallpapersToAlbum("album", match {
            it.size == 2 && it.all { image -> image.albumId == "album" && image.folderId == "folder" }
        }, any()) }
    }

    @Test fun `failed scan preserves existing data and cancellation propagates`() = runTest {
        every { albums.getFolderById("folder") } returns flowOf(Folder.empty("folder", "album").copy(uri = "tree"))
        coEvery { documents.readFolder("tree", any()) } throws SecurityException()
        assertTrue(refresh("folder") is Result.Error)
        coEvery { documents.readFolder("tree", any()) } throws CancellationException()
        try { refresh("folder"); fail("Expected cancellation") } catch (_: CancellationException) { }
        coVerify(exactly = 0) { albums.addWallpapersToAlbum(any(), any(), any()) }
    }
}
