package com.anthonyla.paperize.data.repository

import com.anthonyla.paperize.testFolder
import com.anthonyla.paperize.testWallpaper
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.data.database.PaperizeDatabase
import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.mapper.toEntity
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlbumRepositoryInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, PaperizeDatabase::class.java).build()
    private val wallpapers = WallpaperRepositoryImpl(db)
    private val albums = AlbumRepositoryImpl(com.anthonyla.paperize.data.source.AndroidDocumentSource(context), db)

    @After fun close() = db.close()

    @Test fun cancellingAChunkedFolderImportRollsBackAllRows() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        val folder = testFolder("folder", "album").copy(
            wallpapers = (0..599).map { testWallpaper("image-$it", "album").copy(folderId = "folder", uri = "content://image-$it") }
        )
        try {
            albums.addFolderToAlbum("album", folder) { saved, _ ->
                if (saved >= 500) throw CancellationException("User cancelled")
            }
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
        assertEquals(0, db.wallpaperDao().getWallpaperIdsByAlbum("album").size)
        assertNull(db.folderDao().getFolderWithWallpapers("folder").first())
        assertNull(db.albumDao().getAlbumById("album")?.coverUri)
    }

    @Test fun importingImagesUpdatesCoverAndRotationQueueTogether() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        val first = testWallpaper("first", "album").copy(uri = "content://first")
        db.wallpaperDao().insertWallpapers(listOf(first.toEntity()))
        db.wallpaperQueueDao().rebuildQueue("album", ScreenType.HOME, listOf("first"))
        db.folderDao().insertFolder(testFolder("folder", "album").toEntity())
        val second = first.copy(id = "second", uri = "content://second", folderId = "folder")

        assertEquals(1, albums.addWallpapersToAlbum("album", listOf(second)))
        assertTrue(db.wallpaperQueueDao().getQueueItems("album", ScreenType.HOME).isEmpty())
        assertEquals(2, db.wallpaperDao().getWallpaperIdsByAlbum("album").size)
        assertEquals("content://second", db.folderDao().getFolderById("folder")?.coverUri)
    }

    @Test fun removingCoverUsesOnlyWallpapersFromThatAlbum() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("one", "One", "content://shared"))
        db.albumDao().insertAlbum(AlbumEntity("two", "Two", "content://shared"))
        val cover = testWallpaper("cover", "one").copy(uri = "content://shared")
        val replacement = cover.copy(id = "replacement", uri = "content://replacement", displayOrder = 1)
        db.wallpaperDao().insertWallpapers(listOf(cover, replacement, cover.copy(id = "other", albumId = "two")).map { it.toEntity() })

        albums.removeWallpapersFromAlbum("one", listOf("cover", "other"))
        assertEquals("content://replacement", db.albumDao().getAlbumById("one")?.coverUri)
        assertEquals("content://shared", db.albumDao().getAlbumById("two")?.coverUri)
        assertNotNull(db.wallpaperDao().getWallpaperById("other"))
    }

    @Test fun coverRefreshUsesDisplayOrderAndClearsEmptyFolders() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        db.folderDao().insertFolder(testFolder("folder", "album").toEntity())
        val later = testWallpaper("later", "album").copy(folderId = "folder", uri = "content://later", displayOrder = 9)
        val first = later.copy(id = "first", uri = "content://first", displayOrder = 1)
        val direct = later.copy(id = "direct", folderId = null, uri = "content://direct", displayOrder = 99)
        db.wallpaperDao().insertWallpapers(listOf(later, first, direct).map { it.toEntity() })

        db.folderDao().refreshFolderCovers("album")
        assertEquals("content://first", db.folderDao().getFolderById("folder")?.coverUri)
        assertEquals("content://direct", db.wallpaperDao().getAlbumCoverUri("album"))

        db.wallpaperDao().deleteAlbumWallpapers("album", listOf("later", "first"))
        db.folderDao().refreshFolderCovers("album")
        assertNull(db.folderDao().getFolderById("folder")?.coverUri)
    }

    @Test fun concurrentImportsDeduplicateInsideTheTransaction() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        val image = testWallpaper("one", "album").copy(uri = "content://same")
        val first = async(Dispatchers.IO) { albums.addWallpapersToAlbum("album", listOf(image)) }
        val second = async(Dispatchers.IO) { albums.addWallpapersToAlbum("album", listOf(image.copy(id = "two"))) }
        assertEquals(1, first.await() + second.await())
        assertEquals(1, db.wallpaperDao().getWallpaperIdsByAlbum("album").size)

        db.folderDao().insertFolder(testFolder("folder", "album").toEntity())
        assertEquals(1, albums.addWallpapersToAlbum("album", listOf(image.copy(id = "folder-image", folderId = "folder"))))
        db.folderDao().deleteAlbumFolder("album", "folder")
        assertEquals(0, albums.addWallpapersToAlbum("album", listOf(image.copy(id = "stale", folderId = "folder"))))
    }

    @Test fun concurrentQueueCreationKeepsBothScreensInSyncAndDoesNotRefillConsumedItems() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        albums.addWallpapersToAlbum("album", (1..8).map {
            testWallpaper("image-$it", "album").copy(uri = "content://image-$it")
        })
        val home = async(Dispatchers.IO) { wallpapers.ensureWallpaperQueue("album", ScreenType.HOME, true) }
        val lock = async(Dispatchers.IO) { wallpapers.ensureWallpaperQueue("album", ScreenType.LOCK, true) }
        home.await(); lock.await()
        val expected = db.wallpaperQueueDao().getQueueItems("album", ScreenType.HOME).map { it.wallpaperId }
        assertEquals(8, expected.toSet().size)
        assertEquals(expected, db.wallpaperQueueDao().getQueueItems("album", ScreenType.LOCK).map { it.wallpaperId })
        wallpapers.getAndDequeueWallpaper("album", ScreenType.HOME)
        wallpapers.ensureWallpaperQueue("album", ScreenType.HOME, true)
        assertEquals(expected.drop(1), db.wallpaperQueueDao().getQueueItems("album", ScreenType.HOME).map { it.wallpaperId })
    }
}
