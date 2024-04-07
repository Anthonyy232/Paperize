package com.anthonyla.paperize.feature.wallpaper.data.data_source

import androidx.room.TypeConverter
import com.google.gson.Gson

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun listToJsonString(value: List<String>?): String = gson.toJson(value)

    @TypeConverter
    fun jsonStringToList(value: String) = gson.fromJson(value, Array<String>::class.java).toList()
}
