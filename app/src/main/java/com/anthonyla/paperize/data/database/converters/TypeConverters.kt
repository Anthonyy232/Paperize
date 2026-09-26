package com.anthonyla.paperize.data.database.converters

import androidx.room.TypeConverter
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMediaType
import com.anthonyla.paperize.core.WallpaperSourceType

class TypeConverters {
    @TypeConverter
    fun fromScreenType(value: ScreenType): String = value.name

    @TypeConverter
    fun toScreenType(value: String): ScreenType = ScreenType.fromString(value)

    @TypeConverter
    fun fromWallpaperSourceType(value: WallpaperSourceType): String = value.name

    @TypeConverter
    fun toWallpaperSourceType(value: String): WallpaperSourceType =
        WallpaperSourceType.entries.find { it.name == value } ?: WallpaperSourceType.DIRECT

    @TypeConverter
    fun fromWallpaperMediaType(value: WallpaperMediaType): String = value.name

    @TypeConverter
    fun toWallpaperMediaType(value: String): WallpaperMediaType =
        WallpaperMediaType.fromString(value) ?: WallpaperMediaType.IMAGE
}
