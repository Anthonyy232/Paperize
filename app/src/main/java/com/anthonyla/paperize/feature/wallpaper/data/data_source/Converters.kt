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

    @TypeConverter
    fun fromFolderList(folders: List<Folder>): String {
        return Gson().toJson(folders)
    }

    @TypeConverter
    fun toFolderList(foldersString: String): List<Folder> {
        val listType = object : TypeToken<List<Folder>>() {}.type
        return Gson().fromJson(foldersString, listType)
    }

    @TypeConverter
    fun fromFolder(folder: Folder): String {
        return Gson().toJson(folder)
    }

    @TypeConverter
    fun toFolder(folderString: String): Folder {
        val type = object: TypeToken<Folder>() {}.type
        return Gson().fromJson(folderString, type)
    }
}
