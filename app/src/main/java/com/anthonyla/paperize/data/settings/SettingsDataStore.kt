package com.anthonyla.paperize.data.settings

/**
 * Interface for the data store that handles the settings for application
 */
interface SettingsDataStore {
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun putString(key: String, value: String)
    suspend fun putInt(key: String, value: Int)
    suspend fun getBoolean(key: String): Boolean?
    suspend fun getString(key: String): String?
    suspend fun getInt(key: String): Int?
    suspend fun deleteBoolean(key: String)
    suspend fun deleteString(key: String)
    suspend fun deleteInt(key: String)
    suspend fun clearPreferences()
}