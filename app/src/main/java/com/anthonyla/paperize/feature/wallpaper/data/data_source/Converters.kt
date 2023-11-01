package com.anthonyla.paperize.feature.wallpaper.data.data_source

import androidx.room.TypeConverter
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun listToJsonString(value: List<String>?): String = Gson().toJson(value)

    @TypeConverter
    fun jsonStringToList(value: String) = Gson().fromJson(value, Array<String>::class.java).toList()
}
