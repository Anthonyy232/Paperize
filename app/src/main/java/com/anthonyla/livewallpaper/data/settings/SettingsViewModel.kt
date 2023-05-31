package com.anthonyla.livewallpaper.data.settings

import androidx.lifecycle.ViewModel
import com.anthonyla.livewallpaper.data.settings.SettingsConstants.DARK_MODE_TYPE
import com.anthonyla.livewallpaper.data.settings.SettingsConstants.DYNAMIC_THEME_TYPE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor (private val settingsDataStoreImpl: SettingsDataStoreImpl): ViewModel() {
    fun setDarkMode(status: Boolean?) = runBlocking {
        when (status) {
            true -> { settingsDataStoreImpl.putBoolean(DARK_MODE_TYPE, true) }

            false -> { settingsDataStoreImpl.putBoolean(DARK_MODE_TYPE, false) }

            null -> { settingsDataStoreImpl.deleteBoolean(DARK_MODE_TYPE) }
        }
    }

    fun getDarkMode() = runBlocking {
        return@runBlocking settingsDataStoreImpl.getBoolean(DARK_MODE_TYPE)
    }

    fun setDynamic (status: Boolean?) = runBlocking {
        when (status) {
            true -> { settingsDataStoreImpl.putBoolean(DYNAMIC_THEME_TYPE, true) }

            false -> { settingsDataStoreImpl.putBoolean(DYNAMIC_THEME_TYPE, false) }

            null -> { settingsDataStoreImpl.deleteBoolean(DYNAMIC_THEME_TYPE) }
        }
    }

    fun getDynamicTheme() = runBlocking {
        return@runBlocking settingsDataStoreImpl.getBoolean(DYNAMIC_THEME_TYPE)
    }

    fun resetSettings() = runBlocking {
        settingsDataStoreImpl.clearPreferences()
    }
}