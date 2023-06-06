package com.anthonyla.paperize.data.settings

interface SettingsDataStore {
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun putString(key: String, value: String)
    suspend fun getBoolean(key: String): Boolean?
    suspend fun getString(key: String): String?
    suspend fun deleteBoolean(key: String)
    suspend fun deleteString(key: String)
    suspend fun clearPreferences()
}