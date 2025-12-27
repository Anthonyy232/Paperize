package com.anthonyla.paperize.presentation.screens.wallpaper_mode_selection
import com.anthonyla.paperize.core.constants.Constants

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WallpaperModeSelectionViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val albumRepository: com.anthonyla.paperize.domain.repository.AlbumRepository,
    private val wallpaperScheduler: com.anthonyla.paperize.service.worker.WallpaperScheduler
) : ViewModel() {

    companion object {
        private const val TAG = "WModeSelectionViewModel"
    }

    /**
     * Set wallpaper mode (STATIC or LIVE)
     */
    fun setWallpaperMode(mode: WallpaperMode) {
        viewModelScope.launch {
            // Cancel all scheduled wallpaper changes first
            wallpaperScheduler.cancelAllWallpaperChanges()

            // Delete all albums (cascades to delete all wallpapers, folders, and queues)
            when (val result = albumRepository.deleteAllAlbums()) {
                is com.anthonyla.paperize.core.Result.Success -> { /* Success */ }
                is com.anthonyla.paperize.core.Result.Error -> { 
                    Log.e(TAG, "Error deleting albums during mode selection", result.exception)
                }
                is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
            }

            // Reset schedule settings to default (clears effects, intervals, etc.)
            settingsRepository.clearScheduleSettings()

            settingsRepository.setWallpaperMode(mode)
        }
    }
}
