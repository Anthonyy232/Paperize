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
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.source.WallpaperRenderer
import com.anthonyla.paperize.domain.usecase.ChangeWallpaperUseCase
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

    @Test fun continuingFromAPickedWallpaperFollowsAlbumOrderAndWraps() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        db.wallpaperDao().insertWallpapers((1..5).map {
            testWallpaper("$it", "album").copy(uri = "content://$it", displayOrder = it).toEntity()
        })
        val change = ChangeWallpaperUseCase(wallpapers, SettingsRepositoryImpl(context), object : WallpaperRenderer {
            override suspend fun render(wallpaper: Wallpaper, screen: ScreenType, settings: ScheduleSettings) = null
        })
        suspend fun next() = change.dequeueNext("album", ScreenType.HOME, shuffle = false).id

        change.completeSpecific("album", ScreenType.HOME, "3", shuffle = false, startSequenceHere = true)
        assertEquals(listOf("4", "5", "1"), List(3) { next() })

        change.completeSpecific("album", ScreenType.HOME, "5", shuffle = false, startSequenceHere = true)
        assertEquals("1", next())
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

    @Test fun syncFolderWallpapersRemovesOnlyThatFoldersMissingFilesAndAddsNewOnes() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        albums.addFolderToAlbum("album", testFolder("folder", "album").copy(
            wallpapers = (1..5).map { testWallpaper("image-$it", "album").copy(folderId = "folder", uri = "content://photo-$it") }
        ))
        albums.addWallpapersToAlbum("album", listOf(testWallpaper("direct", "album").copy(uri = "content://photo-3")))

        val scanned = listOf(1, 2, 4, 5, 6).map { testWallpaper("image-$it", "album").copy(folderId = "folder", uri = "content://photo-$it") }
        val added = albums.syncFolderWallpapers("album", "folder", scanned)

        assertEquals(1, added)
        assertEquals(
            listOf(1, 2, 4, 5, 6).map { "content://photo-$it" }.toSet(),
            db.wallpaperDao().getUrisInCollection("album", "folder").toSet()
        )
        assertEquals(listOf("content://photo-3"), db.wallpaperDao().getUrisInCollection("album", null))
    }

    @Test fun syncFolderWallpapersKeepsRowsWhenScanIsEmpty() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        albums.addFolderToAlbum("album", testFolder("folder", "album").copy(
            wallpapers = listOf(testWallpaper("image-1", "album").copy(folderId = "folder", uri = "content://photo-1"))
        ))

        assertEquals(0, albums.syncFolderWallpapers("album", "folder", emptyList()))
        assertEquals(1, db.wallpaperDao().getUrisInCollection("album", "folder").size)
    }

    @Test fun removedFolderImageStaysOutOfRescansUntilRestoredInPlace() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        val images = (1..3).map { testWallpaper("image-$it", "album").copy(folderId = "folder", uri = "content://photo-$it") }
        albums.addFolderToAlbum("album", testFolder("folder", "album").copy(wallpapers = images))
        val removed = albums.removeWallpaper("image-2")!!

        assertEquals(0, albums.syncFolderWallpapers("album", "folder", images))
        val folder = albums.getFolderById("folder").first()!!
        assertEquals(setOf("image-1", "image-3"), folder.wallpapers.map { it.id }.toSet())
        assertEquals(listOf(removed), folder.removedWallpapers)

        albums.restoreWallpaper(removed)
        assertEquals(listOf("image-1", "image-2", "image-3"), db.wallpaperDao().getOrderedWallpaperIdsByAlbum("album"))
        assertTrue(albums.getFolderById("folder").first()!!.removedWallpapers.isEmpty())
    }

    @Test fun rescanForgetsRemovedImagesWhoseFilesAreGone() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        val images = (1..2).map { testWallpaper("image-$it", "album").copy(folderId = "folder", uri = "content://photo-$it") }
        albums.addFolderToAlbum("album", testFolder("folder", "album").copy(wallpapers = images))
        albums.removeWallpaper("image-1")

        albums.syncFolderWallpapers("album", "folder", images.drop(1))
        assertTrue(albums.getFolderById("folder").first()!!.removedWallpapers.isEmpty())
        // The file coming back later is a new image, not a removed one.
        assertEquals(1, albums.syncFolderWallpapers("album", "folder", images))
    }

    @Test fun undoingADirectImageImportedAgainAddsNoDuplicate() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        val image = testWallpaper("image", "album").copy(uri = "content://photo")
        albums.addWallpapersToAlbum("album", listOf(image))
        val removed = albums.removeWallpaper("image")!!
        albums.addWallpapersToAlbum("album", listOf(image.copy(id = "again")))

        albums.restoreWallpaper(removed)
        assertEquals(listOf("again"), db.wallpaperDao().getWallpaperIdsByAlbum("album"))
    }
}
