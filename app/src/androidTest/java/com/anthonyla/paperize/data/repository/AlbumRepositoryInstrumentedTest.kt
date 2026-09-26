package com.anthonyla.paperize.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.data.database.PaperizeDatabase
import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.mapper.toEntity
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import dagger.Lazy
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
    private val wallpapers = WallpaperRepositoryImpl(context, db.wallpaperDao(), db.wallpaperQueueDao(), db.wallpaperCurrentDao())
    private val albums = AlbumRepositoryImpl(context, db, db.albumDao(), db.wallpaperDao(), db.folderDao(), Lazy { wallpapers })

    @After fun close() = db.close()

    @Test fun cancellingAChunkedFolderImportRollsBackAllRows() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        val folder = Folder.empty("folder", "album").copy(
            wallpapers = (0..599).map { Wallpaper.empty("image-$it", "album").copy(folderId = "folder") }
        )
        try {
            albums.addFolderToAlbum("album", folder) { saved, _ ->
                if (saved >= 500) throw CancellationException("User cancelled")
            }
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
        assertEquals(0, db.wallpaperDao().getWallpaperCountByAlbum("album"))
        assertNull(db.folderDao().getFolderWithWallpapers("folder").first())
        assertNull(db.albumDao().getAlbumById("album")?.coverUri)
    }

    @Test fun importingImagesUpdatesCoverAndRotationQueueTogether() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        val first = Wallpaper.empty("first", "album").copy(uri = "content://first")
        db.wallpaperDao().insertWallpaper(first.toEntity())
        db.wallpaperQueueDao().rebuildQueue("album", ScreenType.HOME, listOf("first"))
        db.folderDao().insertFolder(Folder.empty("folder", "album").toEntity())
        val second = first.copy(id = "second", uri = "content://second", folderId = "folder")

        assertEquals(Result.Success(Unit), albums.addWallpapersToAlbum("album", listOf(second)))
        assertTrue(db.wallpaperQueueDao().getQueueItems("album", ScreenType.HOME).isEmpty())
        assertEquals(2, db.wallpaperDao().getWallpaperCountByAlbum("album"))
        assertEquals("content://second", db.folderDao().getFolderById("folder")?.coverUri)
    }

    @Test fun removingCoverUsesOnlyWallpapersFromThatAlbum() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("one", "One", "content://shared"))
        db.albumDao().insertAlbum(AlbumEntity("two", "Two", "content://shared"))
        val cover = Wallpaper.empty("cover", "one").copy(uri = "content://shared")
        val replacement = cover.copy(id = "replacement", uri = "content://replacement", displayOrder = 1)
        db.wallpaperDao().insertWallpapers(listOf(cover, replacement, cover.copy(id = "other", albumId = "two")).map { it.toEntity() })

        assertEquals(Result.Success(Unit), albums.removeWallpapersFromAlbum("one", listOf("cover")))
        assertEquals("content://replacement", db.albumDao().getAlbumById("one")?.coverUri)
        assertEquals("content://shared", db.albumDao().getAlbumById("two")?.coverUri)
    }

    @Test fun coverRefreshUsesDisplayOrderAndClearsEmptyFolders() = runBlocking {
        db.albumDao().insertAlbum(AlbumEntity("album", "Album", null))
        db.folderDao().insertFolder(Folder.empty("folder", "album").toEntity())
        val later = Wallpaper.empty("later", "album").copy(folderId = "folder", uri = "content://later", displayOrder = 9)
        val first = later.copy(id = "first", uri = "content://first", displayOrder = 1)
        val direct = later.copy(id = "direct", folderId = null, uri = "content://direct", displayOrder = 99)
        db.wallpaperDao().insertWallpapers(listOf(later, first, direct).map { it.toEntity() })

        assertEquals(Result.Success(Unit), albums.refreshFolderCovers("album"))
        assertEquals("content://first", db.folderDao().getFolderById("folder")?.coverUri)
        assertEquals(Result.Success(Unit), albums.refreshAlbumCover("album"))
        assertEquals("content://direct", db.albumDao().getAlbumById("album")?.coverUri)

        db.wallpaperDao().deleteWallpapersByFolder("folder")
        assertEquals(Result.Success(Unit), albums.refreshFolderCovers("album"))
        assertNull(db.folderDao().getFolderById("folder")?.coverUri)
    }
}
