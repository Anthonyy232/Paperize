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
                        selectedAlbum = selectedAlbum.firstOrNull(),
                        isDataLoaded = true
                    )
                }
            }
        }
    }

    fun onEvent(event: WallpaperEvent) {
        when (event) {
            is WallpaperEvent.UpdateSelectedAlbum -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (!event.setHome && !event.setLock) return@launch
                    if (event.selectedAlbum != null) {
                        _state.update {
                            it.copy(
                                selectedAlbum = event.selectedAlbum
                            )
                        }
                        repository.upsertSelectedAlbum(event.selectedAlbum)
                    }
                    else if (event.album != null) {
                        val wallpapers: List<Wallpaper> = event.album.wallpapers + event.album.folders.asSequence().flatMap { folder ->
                            folder.wallpapers.asSequence().map { wallpaper ->
                                Wallpaper(
                                    initialAlbumName = event.album.album.initialAlbumName,
                                    wallpaperUri = wallpaper,
                                    key = wallpaper.hashCode() + event.album.album.initialAlbumName.hashCode(),
                                )
                            }
                        }.toList()
                        val shuffledWallpapers1 = wallpapers.map { it.wallpaperUri }.shuffled()
                        val shuffledWallpapers2 = wallpapers.map { it.wallpaperUri }.shuffled()
                        val newSelectedAlbum = SelectedAlbum(
                            album = event.album.album.copy(
                                homeWallpapersInQueue = shuffledWallpapers1,
                                lockWallpapersInQueue = shuffledWallpapers2,
                                currentHomeWallpaper = if (event.setHome) shuffledWallpapers1.firstOrNull() else null,
                                currentLockWallpaper = if (event.scheduleSeparately && event.setLock) shuffledWallpapers2.firstOrNull() else if (event.setLock) shuffledWallpapers1.firstOrNull() else null,
                            ),
                            wallpapers = wallpapers
                        )
                        _state.update {
                            it.copy(
                                selectedAlbum = newSelectedAlbum
                            )
                        }
                        repository.upsertSelectedAlbum(newSelectedAlbum)
                    }
                }
            }
            is WallpaperEvent.Refresh -> {
                refreshAlbums()
            }
            is WallpaperEvent.Reset -> {
                viewModelScope.launch(Dispatchers.IO) {
                    _state.update {
                        it.copy(
                            selectedAlbum = null
                        )
                    }
                    repository.deleteAll()
                }
            }
        }
    }

    // Retrieve album from database into viewModel
    private fun refreshAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getSelectedAlbum().collect { selectedAlbum ->
                _state.update { it.copy(
                    selectedAlbum = selectedAlbum.firstOrNull(),
                ) }
            }
        }
    }
}
