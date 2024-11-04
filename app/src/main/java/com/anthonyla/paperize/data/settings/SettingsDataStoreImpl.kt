package com.anthonyla.paperize.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anthonyla.paperize.core.SettingsConstants.SETTINGS_DATASTORE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS_DATASTORE)

class SettingsDataStoreImpl(private val context: Context) : SettingsDataStore {
    override suspend fun putBoolean(key: String, value: Boolean) {
        val preferencesKey = booleanPreferencesKey(key)
        context.dataStore.edit {
            it[preferencesKey] = value
        }
    }

    override suspend fun putString(key: String, value: String) {
        val preferencesKey = stringPreferencesKey(key)
        context.dataStore.edit {
            it[preferencesKey] = value
        }
    }

    override suspend fun putInt(key: String, value: Int) {
        val preferencesKey = intPreferencesKey(key)
        context.dataStore.edit {
            it[preferencesKey] = value
        }
    }

    override suspend fun getBoolean(key: String): Boolean? {
        val head = context.dataStore.data.first()
        val preferencesKey = booleanPreferencesKey(key)
        return head[preferencesKey]
    }

    override suspend fun getString(key: String): String? {
        return try {
            val head = context.dataStore.data.first()
            val preferencesKey = stringPreferencesKey(key)
            head[preferencesKey]
        } catch (exception: Exception) {
            exception.printStackTrace()
            null
        }
    }

    override suspend fun getInt(key: String): Int? {
        return try {
            val head = context.dataStore.data.first()
            val preferencesKey = intPreferencesKey(key)
            head[preferencesKey]?.toInt()
        } catch (exception: Exception) {
            exception.printStackTrace()
            null
        }
    }

    override fun getBooleanFlow(key: String): Flow<Boolean?> = context.dataStore.data
        .catch { exception ->
            exception.printStackTrace()
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences[booleanPreferencesKey(key)]
        }

    override fun getStringFlow(key: String): Flow<String?> = context.dataStore.data
        .catch { exception ->
            exception.printStackTrace()
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences[stringPreferencesKey(key)]
        }

    override fun getIntFlow(key: String): Flow<Int?> = context.dataStore.data
        .catch { exception ->
            exception.printStackTrace()
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences[intPreferencesKey(key)]
        }

    override suspend fun deleteBoolean(key: String) {
        val preferencesKey = booleanPreferencesKey(key)
        context.dataStore.edit {
            if (it.contains(preferencesKey)) {
                it.remove(preferencesKey)
            }
        }
    }

    override suspend fun deleteString(key: String) {
        val preferencesKey = stringPreferencesKey(key)
        context.dataStore.edit {
            if (it.contains(preferencesKey)) {
                it.remove(preferencesKey)
            }
        }
    }

    override suspend fun deleteInt(key: String) {
        val preferencesKey = intPreferencesKey(key)
        context.dataStore.edit {
            if (it.contains(preferencesKey)) {
                it.remove(preferencesKey)
            }
        }
    }

    override suspend fun clearPreferences() {
        context.dataStore.edit {
            it.clear()
        }
    }

    override suspend fun clear(keys: List<String>) {
        context.dataStore.edit { preferences ->
            keys.forEach { key ->
                if (preferences.contains(booleanPreferencesKey(key))) {
                    preferences.remove(booleanPreferencesKey(key))
                }
                else if (preferences.contains(stringPreferencesKey(key))) {
                    preferences.remove(stringPreferencesKey(key))
                }
                else if (preferences.contains(intPreferencesKey(key))) {
                    preferences.remove(intPreferencesKey(key))
                }
            }
        }
    }
}