package com.anthonyla.paperize.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.anthonyla.paperize.data.database.dao.AlbumDao
import com.anthonyla.paperize.data.database.dao.ExcludedWallpaperDao
import com.anthonyla.paperize.data.database.dao.FolderDao
import com.anthonyla.paperize.data.database.dao.WallpaperCurrentDao
import com.anthonyla.paperize.data.database.dao.WallpaperDao
import com.anthonyla.paperize.data.database.dao.WallpaperQueueDao
import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.database.entities.ExcludedWallpaperEntity
import com.anthonyla.paperize.data.database.entities.FolderEntity
import com.anthonyla.paperize.data.database.entities.WallpaperCurrentEntity
import com.anthonyla.paperize.data.database.entities.WallpaperEntity
import com.anthonyla.paperize.data.database.entities.WallpaperQueueEntity
import com.anthonyla.paperize.core.constants.Constants

@Database(
    entities = [
        AlbumEntity::class,
        WallpaperEntity::class,
        FolderEntity::class,
        WallpaperQueueEntity::class,
        WallpaperCurrentEntity::class,
        ExcludedWallpaperEntity::class
    ],
    version = Constants.DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 4, to = 5)]
)
abstract class PaperizeDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun wallpaperDao(): WallpaperDao
    abstract fun folderDao(): FolderDao
    abstract fun wallpaperQueueDao(): WallpaperQueueDao
    abstract fun wallpaperCurrentDao(): WallpaperCurrentDao
    abstract fun excludedWallpaperDao(): ExcludedWallpaperDao
}
