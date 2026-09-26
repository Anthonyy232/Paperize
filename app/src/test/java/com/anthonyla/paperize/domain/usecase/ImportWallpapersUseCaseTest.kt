package com.anthonyla.paperize.domain.usecase

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.WallpaperSourceType
import com.anthonyla.paperize.core.util.ScannedImage
import com.anthonyla.paperize.core.util.getFileName
import com.anthonyla.paperize.core.util.scanFolderImages
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ImportWallpapersUseCaseTest {
    private val context = mockk<Context>()
    private val resolver = mockk<ContentResolver>(relaxed = true)
    private val repository = mockk<AlbumRepository>()
    private val useCase = ImportWallpapersUseCase(context, repository)
    private val album = Album.empty("album")

    @Before fun setUp() {
        every { context.contentResolver } returns resolver
        every { repository.getAlbumById("album") } returns flowOf(album)
        mockkStatic(Uri::class)
        mockkStatic(DocumentFile::class)
        mockkStatic("com.anthonyla.paperize.core.util.ExtensionsKt")
    }

    @After fun tearDown() = unmockkAll()

    private fun uri(value: String): Uri {
        val result = mockk<Uri>()
        every { Uri.parse(value) } returns result
        every { result.toString() } returns value
        every { result.getFileName(context) } returns "$value.jpg"
        return result
    }

    @Test fun `import skips duplicates and appends after highest order`() = runTest {
        val existing = Wallpaper.empty("existing", "album").copy(uri = "existing", displayOrder = 10)
        every { repository.getAlbumById("album") } returns flowOf(album.copy(wallpapers = listOf(existing)))
        val newUri = uri("new")
        uri("existing")
        coEvery { repository.addWallpapersToAlbum(any(), any(), any()) } returns Result.Success(Unit)

        assertTrue(useCase.addImages("album", listOf("existing", "new", "new")) { _, _ -> })

        coVerify {
            repository.addWallpapersToAlbum("album", match {
                it.size == 1 && it.single().uri == "new" && it.single().displayOrder == 11
            }, any())
        }
        verify(exactly = 1) { resolver.takePersistableUriPermission(newUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    @Test fun `already imported images do not write again`() = runTest {
        uri("existing")
        every { repository.getAlbumById("album") } returns flowOf(
            album.copy(wallpapers = listOf(Wallpaper.empty().copy(uri = "existing")))
        )
        assertFalse(useCase.addImages("album", listOf("existing")) { _, _ -> })
        coVerify(exactly = 0) { repository.addWallpapersToAlbum(any(), any(), any()) }
    }

    @Test fun `permission failure does not save an inaccessible image`() = runTest {
        val selected = uri("blocked")
        every { resolver.takePersistableUriPermission(selected, any()) } throws SecurityException("No grant")
        try {
            useCase.addImages("album", listOf("blocked")) { _, _ -> }
            fail("Expected permission error")
        } catch (_: SecurityException) { }
        coVerify(exactly = 0) { repository.addWallpapersToAlbum(any(), any(), any()) }
    }

    @Test fun `folder import keeps scanned metadata and source type`() = runTest {
        val tree = uri("tree")
        val image = uri("image")
        coEvery { tree.scanFolderImages(context, any()) } returns listOf(ScannedImage(image, "photo.png", 42L))
        val document = mockk<DocumentFile>()
        every { DocumentFile.fromTreeUri(context, tree) } returns document
        every { document.name } returns "Photos"
        coEvery { repository.addFolderToAlbum(any(), any(), any()) } returns Result.Success(Unit)

        assertTrue(useCase.addFolder("album", "tree", {}, { _, _ -> }))

        coVerify { repository.addFolderToAlbum("album", match { folder ->
            folder.name == "Photos" && folder.wallpapers.single().let {
                it.folderId == folder.id && it.sourceType == WallpaperSourceType.FOLDER &&
                    it.dateModified == 42L && it.fileName == "photo.png"
            }
        }, any()) }
    }

    @Test fun `existing folder restores read access without duplicating it`() = runTest {
        val tree = uri("tree")
        every { repository.getAlbumById("album") } returns flowOf(
            album.copy(folders = listOf(Folder.empty().copy(uri = "tree")))
        )
        assertFalse(useCase.addFolder("album", "tree", {}, { _, _ -> }))
        verify { resolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        coVerify(exactly = 0) { tree.scanFolderImages(any(), any()) }
        coVerify(exactly = 0) { repository.addFolderToAlbum(any(), any(), any()) }
    }

    @Test fun `failed and cancelled scans do not save a partial folder`() = runTest {
        val tree = uri("tree")
        for (failure in listOf(java.io.IOException("Unavailable"), CancellationException("Cancelled"))) {
            coEvery { tree.scanFolderImages(context, any()) } throws failure
            try {
                useCase.addFolder("album", "tree", {}, { _, _ -> })
                fail("Expected scan failure")
            } catch (actual: Exception) {
                assertEquals(failure.javaClass, actual.javaClass)
                assertEquals(failure.message, actual.message)
            }
        }
        coVerify(exactly = 0) { repository.addFolderToAlbum(any(), any(), any()) }
    }

}
