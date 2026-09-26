package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.WallpaperSourceType
import com.anthonyla.paperize.domain.model.Album
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

class ImportWallpapersUseCaseTest {
    private val documents = mockk<DocumentSource>()
    private val repository = mockk<AlbumRepository>()
    private val useCase = ImportWallpapersUseCase(documents, repository)

    @Test fun `image imports retain permission once and preserve metadata`() = runTest {
        coEvery { documents.retainReadPermission("image") } just Runs
        coEvery { documents.readImage("image") } returns SourceImage("image", "photo.jpg", 42L)
        coEvery { repository.addWallpapersToAlbum(any(), any(), any()) } returns Result.Success(1)
        assertTrue(useCase.addImages("album", listOf("image", "image")) { _, _ -> })
        coVerify(exactly = 1) { documents.retainReadPermission("image") }
        coVerify { repository.addWallpapersToAlbum("album", match {
            it.single().let { image -> image.uri == "image" && image.dateModified == 42L && image.folderId == null }
        }, any()) }
    }

    @Test fun `permission failure does not save an inaccessible image`() = runTest {
        coEvery { documents.retainReadPermission("blocked") } throws SecurityException("No grant")
        try {
            useCase.addImages("album", listOf("blocked")) { _, _ -> }
            fail("Expected permission error")
        } catch (_: SecurityException) { }
        coVerify(exactly = 0) { repository.addWallpapersToAlbum(any(), any(), any()) }
    }

    @Test fun `folder import keeps metadata and assigns membership`() = runTest {
        coEvery { documents.retainReadPermission("tree") } just Runs
        every { repository.getAlbumById("album") } returns flowOf(Album.empty("album"))
        coEvery { documents.readFolder("tree", any()) } returns SourceFolder("Photos", listOf(SourceImage("image", "photo.png", 42L)))
        coEvery { repository.addFolderToAlbum(any(), any(), any()) } returns Result.Success(true)
        assertTrue(useCase.addFolder("album", "tree", {}, { _, _ -> }))
        coVerify { repository.addFolderToAlbum("album", match { folder ->
            folder.name == "Photos" && folder.wallpapers.single().let {
                it.folderId == folder.id && it.albumId == "album" && it.sourceType == WallpaperSourceType.FOLDER &&
                    it.dateModified == 42L && it.fileName == "photo.png"
            }
        }, any()) }
    }

    @Test fun `existing folder restores access without rescanning`() = runTest {
        coEvery { documents.retainReadPermission("tree") } just Runs
        every { repository.getAlbumById("album") } returns flowOf(Album.empty("album").copy(
            folders = listOf(Folder.empty().copy(uri = "tree"))
        ))
        assertFalse(useCase.addFolder("album", "tree", {}, { _, _ -> }))
        coVerify { documents.retainReadPermission("tree") }
        coVerify(exactly = 0) { documents.readFolder(any(), any()) }
    }

    @Test fun `failed and cancelled scans do not save a partial folder`() = runTest {
        coEvery { documents.retainReadPermission("tree") } just Runs
        every { repository.getAlbumById("album") } returns flowOf(Album.empty("album"))
        for (failure in listOf(java.io.IOException("Unavailable"), CancellationException("Cancelled"))) {
            coEvery { documents.readFolder("tree", any()) } throws failure
            try {
                useCase.addFolder("album", "tree", {}, { _, _ -> })
                fail("Expected scan failure")
            } catch (actual: Exception) { assertSame(failure, actual) }
        }
        coVerify(exactly = 0) { repository.addFolderToAlbum(any(), any(), any()) }
    }
}
