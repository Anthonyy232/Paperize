package com.anthonyla.paperize.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anthonyla.paperize.data.settings.SettingsConstants.SETTINGS_DATASTORE
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private val Context.dataStore : DataStore<Preferences> by preferencesDataStore(name = SETTINGS_DATASTORE)
class SettingsDataStoreImpl @Inject constructor (private val context: Context): SettingsDataStore {
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

    override suspend fun clearPreferences() {
        context.dataStore.edit {
            it.clear()
        }
    }
}