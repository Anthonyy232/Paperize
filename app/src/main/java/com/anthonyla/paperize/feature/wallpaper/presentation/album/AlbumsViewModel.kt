package com.anthonyla.paperize.feature.wallpaper.presentation.album

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumState
import com.anthonyla.paperize.feature.wallpaper.use_case.AlbumsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    application: Application,
    private val albumsUseCases: AlbumsUseCases,
    private val repository: AlbumRepository
) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication<Application>().applicationContext

    private val _albumWithWallpaper = MutableStateFlow(emptyList<AlbumWithWallpaper>())
    val albumWithWallpaper = _albumWithWallpaper.asStateFlow()

    init {
        refreshAlbums()
        print("hi")
    }


    fun onEvent(event: AlbumsEvent) {
    }

    private fun refreshAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAlbumsWithWallpapers().flowOn(Dispatchers.IO).collect { albumsWithWallpapers ->
                _albumWithWallpaper.update { albumsWithWallpapers }
            }
        }
    }
}