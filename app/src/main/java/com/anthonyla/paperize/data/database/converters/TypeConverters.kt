package com.anthonyla.paperize.data.database.converters

import androidx.room.TypeConverter
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.WallpaperSourceType

/**
 * Room type converters for enum types
 */
class TypeConverters {
    @TypeConverter
    fun fromScreenType(value: ScreenType): String = value.name

    @TypeConverter
    fun toScreenType(value: String): ScreenType = ScreenType.fromString(value)

    @TypeConverter
    fun fromScalingType(value: ScalingType): String = value.name

    @TypeConverter
    fun toScalingType(value: String): ScalingType = ScalingType.fromString(value)

    @TypeConverter
    fun fromWallpaperSourceType(value: WallpaperSourceType): String = value.name

    @TypeConverter
    fun toWallpaperSourceType(value: String): WallpaperSourceType =
        WallpaperSourceType.entries.find { it.name == value } ?: WallpaperSourceType.DIRECT
}
