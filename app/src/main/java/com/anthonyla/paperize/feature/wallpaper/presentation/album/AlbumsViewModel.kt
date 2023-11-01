package com.anthonyla.paperize.feature.wallpaper.presentation.album

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import com.lazygeniouz.dfc.file.DocumentFileCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    application: Application,
    private val repository: AlbumRepository
) : AndroidViewModel(application) {
    var shouldNotBypassSplashScreen by mutableStateOf(true)
    private val context: Context
        get() = getApplication<Application>().applicationContext
    private val _state = MutableStateFlow(AlbumsState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000), AlbumsState()
    )

    init {
        refreshAlbums()
        shouldNotBypassSplashScreen = false
    }

    fun onEvent(event: AlbumsEvent) {
    }

    private fun refreshAlbums() {
        viewModelScope.launch {
            repository.getAlbumsWithWallpapers().collect { albumWithWallpapers ->
                _state.update { it.copy(
                    albumWithWallpapers = albumWithWallpapers
                ) }
            }
            //verifyAllAlbumAndWallpaper()
        }
    }
    private fun verifyAllAlbumAndWallpaper() {
        viewModelScope.launch {
        }
    }
}

