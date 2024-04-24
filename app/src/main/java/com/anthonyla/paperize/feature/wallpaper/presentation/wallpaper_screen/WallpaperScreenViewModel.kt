package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WallpaperScreenViewModel @Inject constructor (
    application: Application,
    private val repository: SelectedAlbumRepository,
) : AndroidViewModel(application) {
    var shouldNotBypassSplashScreen by mutableStateOf(true)
    private val context: Context get() = getApplication<Application>().applicationContext
    private var _state = MutableStateFlow(WallpaperState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000), WallpaperState()
    )

    init {
        viewModelScope.launch {
            refreshAlbums()
            shouldNotBypassSplashScreen = false
        }
    }

    fun onEvent(event: WallpaperEvent) {
        when (event) {
            is WallpaperEvent.UpdateSelectedAlbum -> {
                viewModelScope.launch {
                    repository.deleteAll()
                    repository.upsertSelectedAlbum(event.selectedAlbum)
                }
            }
            is WallpaperEvent.Refresh -> {
                refreshAlbums()
            }
            is WallpaperEvent.Reset -> {
                viewModelScope.launch {
                    repository.deleteAll()
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
