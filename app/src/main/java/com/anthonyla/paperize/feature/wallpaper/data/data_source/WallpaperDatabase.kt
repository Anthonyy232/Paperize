package com.anthonyla.paperize.feature.wallpaper.data.data_source

import androidx.room.Database
import androidx.room.RoomDatabase
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper

@Database(
    entities = [Wallpaper::class],
    version = 1
)
abstract class WallpaperDatabase: RoomDatabase() {
    abstract val wallpaperDao: WallpaperDao
    companion object {
        const val DATABASE_NAME = "paperize_db"
    }
}