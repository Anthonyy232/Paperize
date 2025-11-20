package com.anthonyla.paperize.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.anthonyla.paperize.data.database.converters.TypeConverters as PaperizeTypeConverters
import com.anthonyla.paperize.data.database.dao.AlbumDao
import com.anthonyla.paperize.data.database.dao.FolderDao
import com.anthonyla.paperize.data.database.dao.WallpaperDao
import com.anthonyla.paperize.data.database.dao.WallpaperQueueDao
import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.database.entities.FolderEntity
import com.anthonyla.paperize.data.database.entities.WallpaperEntity
import com.anthonyla.paperize.data.database.entities.WallpaperQueueEntity
import com.anthonyla.paperize.core.constants.Constants

/**
 * Paperize Room Database
 *
 * **MAJOR ARCHITECTURAL IMPROVEMENT:**
 * - Properly normalized schema (no nested collections in entities)
 * - NO CursorWindow reflection hack needed!
 * - Proper foreign keys with cascade delete
 * - Indexed columns for performance
 * - Separate queue management table
 * - Room migrations support (no destructive migrations!)
 *
 * Version: 1
 */
@Database(
    entities = [
        AlbumEntity::class,
        WallpaperEntity::class,
        FolderEntity::class,
        WallpaperQueueEntity::class
    ],
    version = Constants.DATABASE_VERSION,
    exportSchema = true
)
@TypeConverters(PaperizeTypeConverters::class)
abstract class PaperizeDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun wallpaperDao(): WallpaperDao
    abstract fun folderDao(): FolderDao
    abstract fun wallpaperQueueDao(): WallpaperQueueDao
}
