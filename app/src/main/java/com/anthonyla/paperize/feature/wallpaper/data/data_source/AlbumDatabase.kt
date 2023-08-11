package com.anthonyla.paperize.feature.wallpaper.data.data_source

import androidx.room.Database
import androidx.room.RoomDatabase
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.Image

@Database(
    entities = [Album::class, Image::class],
    version = 1
)
abstract class AlbumDatabase: RoomDatabase() {
    abstract val albumDao: AlbumDao
    companion object {
        const val DATABASE_NAME = "album_db"
    }
}