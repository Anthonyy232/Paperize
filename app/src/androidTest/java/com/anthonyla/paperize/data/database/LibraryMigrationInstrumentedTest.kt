package com.anthonyla.paperize.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.anthonyla.paperize.core.ScreenType
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LibraryMigrationInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun everyLegacyVersionPreservesLibraryAndQueue() = runBlocking {
        for (version in 1..3) {
            val name = "migration-test-$version"
            context.deleteDatabase(name)
            try {
                createLegacyDatabase(name, version)
                val db = Room.databaseBuilder(context, PaperizeDatabase::class.java, name)
                    .addMigrations(*LIBRARY_MIGRATIONS).build()
                try {
                    assertEquals("My album", db.albumDao().getAlbumById("album")?.name)
                    assertEquals("content://image", db.albumDao().getAlbumById("album")?.coverUri)
                    val image = db.wallpaperDao().getWallpaperById("image")!!
                    assertEquals("folder", image.folderId)
                    assertEquals("photo.jpg", image.fileName)
                    assertEquals(7, image.displayOrder)
                    assertEquals(42L, image.addedAt)
                    assertEquals("content://image", db.folderDao().getFolderById("folder")?.coverUri)
                    assertEquals(listOf("image"), db.wallpaperQueueDao().getQueueItems("album", ScreenType.HOME).map { it.wallpaperId })
                    assertEquals(if (version >= 3) "image" else null, db.wallpaperCurrentDao().getCurrentWallpaper("album", ScreenType.HOME)?.id)
                    db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
                    db.albumDao().deleteAlbumById("album")
                    assertNull(db.wallpaperDao().getWallpaperById("image"))
                    assertTrue(db.wallpaperQueueDao().getQueueItems("album", ScreenType.HOME).isEmpty())
                } finally { db.close() }
            } finally { context.deleteDatabase(name) }
        }
    }

    private fun createLegacyDatabase(name: String, version: Int) {
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.anthonyla.paperize.data.database.PaperizeDatabase/4.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database").getJSONArray("entities") }
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.setForeignKeyConstraintsEnabled(true)
            for (index in 0 until schema.length()) {
                val entity = schema.getJSONObject(index)
                val table = entity.getString("tableName")
                if (table == "wallpaper_current" && version < 3) continue
                var create = entity.getString("createSql").replace("\${TABLE_NAME}", table)
                // Verified against the version 1, 2 and 3 entities in Git history.
                if (table == "wallpapers") {
                    create = if (version == 1) create.replace("`mediaType` TEXT NOT NULL, ", "")
                    else create.replace("PRIMARY KEY(`id`)", "`cropOffsetX` REAL, `cropOffsetY` REAL, `cropScale` REAL, PRIMARY KEY(`id`)")
                }
                db.execSQL(create)
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (i in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL("INSERT INTO albums VALUES ('album', 'My album', 'content://image', 1, 2)")
            db.execSQL("INSERT INTO folders (id, albumId, name, uri, coverUri, dateModified, displayOrder, addedAt) VALUES ('folder', 'album', 'Photos', 'content://tree', 'content://image', 3, 0, 4)")
            val mediaColumn = if (version > 1) ", mediaType" else ""
            val mediaValue = if (version > 1) ", 'IMAGE'" else ""
            db.execSQL("INSERT INTO wallpapers (id, albumId, folderId, uri, fileName, dateModified, displayOrder, sourceType, addedAt$mediaColumn) VALUES ('image', 'album', 'folder', 'content://image', 'photo.jpg', 5, 7, 'FOLDER', 42$mediaValue)")
            db.execSQL("INSERT INTO wallpaper_queue VALUES (1, 'album', 'image', 'HOME', 2, 6)")
            if (version >= 3) db.execSQL("INSERT INTO wallpaper_current VALUES ('album', 'HOME', 'image', 9)")
            db.version = version
        }
    }
}
