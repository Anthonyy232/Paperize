package com.anthonyla.paperize.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Versions 1–3 used the same library tables, with extra crop columns in versions 2–3. */
val LIBRARY_MIGRATIONS: Array<Migration> = (1..3).map { version ->
    object : Migration(version, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Rebuilding the parent table would cascade-delete these rows; preserve them first.
            db.execSQL("CREATE TEMP TABLE saved_queue AS SELECT * FROM wallpaper_queue")
            db.execSQL("DROP TABLE wallpaper_queue")
            if (version >= 3) {
                db.execSQL("CREATE TEMP TABLE saved_current AS SELECT * FROM wallpaper_current")
                db.execSQL("DROP TABLE wallpaper_current")
            }
            db.execSQL("CREATE TABLE IF NOT EXISTS `wallpapers_new` (`id` TEXT NOT NULL, `albumId` TEXT NOT NULL, `folderId` TEXT, `uri` TEXT NOT NULL, `fileName` TEXT NOT NULL, `dateModified` INTEGER NOT NULL, `displayOrder` INTEGER NOT NULL, `sourceType` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, `mediaType` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`albumId`) REFERENCES `albums`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            val mediaType = if (version == 1) "'IMAGE'" else "mediaType"
            db.execSQL("""
                INSERT INTO wallpapers_new (id, albumId, folderId, uri, fileName, dateModified, displayOrder, sourceType, addedAt, mediaType)
                SELECT id, albumId, folderId, uri, fileName, dateModified, displayOrder, sourceType, addedAt, $mediaType FROM wallpapers
            """)
            db.execSQL("DROP TABLE wallpapers")
            db.execSQL("ALTER TABLE wallpapers_new RENAME TO wallpapers")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallpapers_albumId` ON `wallpapers` (`albumId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallpapers_folderId` ON `wallpapers` (`folderId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallpapers_uri` ON `wallpapers` (`uri`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `wallpaper_queue` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `albumId` TEXT NOT NULL, `wallpaperId` TEXT NOT NULL, `screenType` TEXT NOT NULL, `queuePosition` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, FOREIGN KEY(`albumId`) REFERENCES `albums`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`wallpaperId`) REFERENCES `wallpapers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallpaper_queue_albumId_screenType` ON `wallpaper_queue` (`albumId`, `screenType`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallpaper_queue_wallpaperId` ON `wallpaper_queue` (`wallpaperId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallpaper_queue_queuePosition` ON `wallpaper_queue` (`queuePosition`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `wallpaper_current` (`albumId` TEXT NOT NULL, `screenType` TEXT NOT NULL, `wallpaperId` TEXT NOT NULL, `appliedAt` INTEGER NOT NULL, PRIMARY KEY(`albumId`, `screenType`), FOREIGN KEY(`albumId`) REFERENCES `albums`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`wallpaperId`) REFERENCES `wallpapers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallpaper_current_wallpaperId` ON `wallpaper_current` (`wallpaperId`)")
            db.execSQL("INSERT INTO wallpaper_queue SELECT * FROM saved_queue")
            db.execSQL("DROP TABLE saved_queue")
            if (version >= 3) {
                db.execSQL("INSERT INTO wallpaper_current SELECT * FROM saved_current")
                db.execSQL("DROP TABLE saved_current")
            }
        }
    }
}.toTypedArray()
