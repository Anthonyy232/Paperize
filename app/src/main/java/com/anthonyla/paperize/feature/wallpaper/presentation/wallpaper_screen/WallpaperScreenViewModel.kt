package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WallpaperScreenViewModel @Inject constructor (
    application: Application,
    private val repository: SelectedAlbumRepository,
) : AndroidViewModel(application) {
    private var _state = MutableStateFlow(WallpaperState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000), WallpaperState()
    )

    init {
        viewModelScope.launch {
            refreshAlbums()
        }
    }

    fun onEvent(event: WallpaperEvent) {
        when (event) {
            is WallpaperEvent.UpdateSelectedAlbum -> {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.deleteAll()
                        repository.upsertSelectedAlbum(event.selectedAlbum)
                    }
                }
            }
            is WallpaperEvent.Refresh -> {
                refreshAlbums()
            }
            is WallpaperEvent.Reset -> {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.deleteAll()
                        _state.update {
                            WallpaperState()
                        }
                    }
                }
            }
        }
    }

    // Retrieve album from database into viewModel
    private fun refreshAlbums() {
        viewModelScope.launch {
            repository.getSelectedAlbum().collect { selectedAlbum ->
                _state.update { it.copy(
                    selectedAlbum = selectedAlbum.firstOrNull()
                ) }
            }
        }
    }
}
