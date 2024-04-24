package com.anthonyla.paperize.feature.wallpaper.data.data_source

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Converters for Room database to convert list of strings to json string and vice versa
 */
class Converters {
    private val gson = Gson()

    /**
     * Convert list of strings to json string
     */
    @TypeConverter
    fun listToJsonString(value: List<String>?): String = gson.toJson(value)

    /**
     * Convert json string to list of strings
     */
    @TypeConverter
    fun jsonStringToList(value: String) = gson.fromJson(value, Array<String>::class.java).toList()
}
