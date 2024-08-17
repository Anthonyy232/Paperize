package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
        viewModelScope.launch(Dispatchers.IO) {
            repository.getSelectedAlbum().collect { selectedAlbum ->
                _state.update {
                    it.copy(
                        selectedAlbum = selectedAlbum,
                        isDataLoaded = true
                    )
                }
            }
        }
    }

    fun onEvent(event: WallpaperEvent) {
        when (event) {
            is WallpaperEvent.AddSelectedAlbum -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val deleted = event.deleteAlbumName?.let {
                        repository.cascadeDeleteAlbum(it)
                        true
                    } ?: false
                    val wallpapers = event.album.wallpapers + event.album.folders.flatMap { folder ->
                        folder.wallpapers.map { wallpaper ->
                            Wallpaper(
                                initialAlbumName = event.album.album.initialAlbumName,
                                wallpaperUri = wallpaper,
                                key = wallpaper.hashCode() + event.album.album.initialAlbumName.hashCode() + System.currentTimeMillis().toInt(),
                            )
                        }
                    }
                    val newSelectedAlbum = SelectedAlbum(
                        album = event.album.album.copy(
                            lockWallpapersInQueue = wallpapers.map { it.wallpaperUri }.shuffled(),
                            homeWallpapersInQueue = wallpapers.map { it.wallpaperUri }.shuffled()
                        ),
                        wallpapers = wallpapers
                    )
                    repository.upsertSelectedAlbum(newSelectedAlbum)
                    _state.update {
                        it.copy(
                            selectedAlbum = it.selectedAlbum?.let { selectedAlbums ->
                                selectedAlbums.filterNot { selectedAlbum -> deleted && selectedAlbum.album.initialAlbumName == event.deleteAlbumName } + newSelectedAlbum
                            }
                        )
                    }
                }
            }
            is WallpaperEvent.UpdateSelectedAlbum -> {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.upsertSelectedAlbum(event.album)
                    _state.update {
                        it.copy(
                            selectedAlbum = it.selectedAlbum?.map { selectedAlbum ->
                                if (selectedAlbum.album.initialAlbumName == event.album.album.initialAlbumName) {
                                    event.album
                                } else {
                                    selectedAlbum
                                }
                            }
                        )
                    }
                }
            }
            is WallpaperEvent.Refresh -> {
                refreshAlbums()
            }
            is WallpaperEvent.Reset -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (event.album == null) {
                        repository.deleteAll()
                        _state.update {
                            it.copy(
                                selectedAlbum = null
                            )
                        }
                    }
                    else {
                        repository.cascadeDeleteAlbum(event.album.album.initialAlbumName)
                        _state.update {
                            it.copy(
                                selectedAlbum = it.selectedAlbum?.filter { selectedAlbum -> selectedAlbum.album.initialAlbumName != event.album.album.initialAlbumName }
                            )
                        }
                    }
                }
            }
        }
    }

    // Retrieve album from database into viewModel
    private fun refreshAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getSelectedAlbum().collect { selectedAlbum ->
                _state.update { it.copy(
                    selectedAlbum = selectedAlbum,
                ) }
            }
        }
    }
}
