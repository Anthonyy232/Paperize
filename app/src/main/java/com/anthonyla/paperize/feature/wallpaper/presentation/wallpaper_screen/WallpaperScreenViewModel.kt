package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WallpaperScreenViewModel @Inject constructor(
    private val repository: AlbumRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WallpaperState())
    val state = combine(
        loadSelectedAlbumFlow(),
        _state
    ) { selectedAlbum, currentState ->
        currentState.copy(
            selectedAlbum = selectedAlbum,
            isDataLoaded = true
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WallpaperState()
    )

    private fun loadSelectedAlbumFlow(): Flow<List<AlbumWithWallpaperAndFolder>?> =
        repository.getSelectedAlbums()

    fun onEvent(event: WallpaperEvent) {
        when (event) {
            is WallpaperEvent.AddSelectedAlbum -> {
                viewModelScope.launch {
                    event.deselectAlbumName?.let {
                        repository.updateAlbumSelection(it, false)
                    }
                    repository.updateAlbumSelection(event.album.album.initialAlbumName, true)
                }
            }

            is WallpaperEvent.RemoveSelectedAlbum -> {
                viewModelScope.launch {
                    repository.updateAlbumSelection(event.deselectAlbumName, false)
                }
            }

            is WallpaperEvent.Reset -> {
                viewModelScope.launch {
                    repository.deselectAllAlbums()
                }
            }
        }
    }
}
