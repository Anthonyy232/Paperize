package com.anthonyla.paperize.data.database.converters

import androidx.room.TypeConverter
import com.anthonyla.paperize.core.ScreenType

class TypeConverters {
    @TypeConverter
    fun fromScreenType(value: ScreenType): String = value.name

    @TypeConverter
    fun toScreenType(value: String): ScreenType = ScreenType.fromString(value)
}
