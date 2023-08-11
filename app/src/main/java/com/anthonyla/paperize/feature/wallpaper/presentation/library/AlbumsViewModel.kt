package com.anthonyla.paperize.feature.wallpaper.presentation.library


import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.feature.wallpaper.use_case.AlbumUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val albumUseCases: AlbumUseCases
) : ViewModel() {

    private var lastAlbumId: Long = 0
    private val _state = mutableStateOf(AlbumsState())
    val state: State<AlbumsState> = _state

    fun onEvent(event: AlbumsEvent) {
        when (event) {
            is AlbumsEvent.AddAlbum -> {
                viewModelScope.launch {
                    lastAlbumId = albumUseCases.addAlbum(event.album)
                }
            }
            is AlbumsEvent.AddImage -> {
                viewModelScope.launch {
                    albumUseCases.addImage(event.image.copy(albumId = lastAlbumId))
                }
            }
            else -> {}
        }

    }
}






