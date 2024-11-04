package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WallpaperScreenViewModel @Inject constructor(
    private val repository: SelectedAlbumRepository,
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

    private fun loadSelectedAlbumFlow(): Flow<List<SelectedAlbum>?> =
        repository.getSelectedAlbum()

    fun onEvent(event: WallpaperEvent) {
        when (event) {
            is WallpaperEvent.AddSelectedAlbum -> {
                viewModelScope.launch {
                    event.deleteAlbumName?.let {
                        repository.cascadeDeleteAlbum(it)
                    }
                    
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
                }
            }

            is WallpaperEvent.UpdateSelectedAlbum -> {
                viewModelScope.launch {
                    repository.upsertSelectedAlbum(event.album)
                }
            }

            is WallpaperEvent.Reset -> {
                viewModelScope.launch {
                    if (event.album == null) {
                        repository.deleteAll()
                    } else {
                        repository.cascadeDeleteAlbum(event.album.album.initialAlbumName)
                    }
                }
            }
        }
    }
}
