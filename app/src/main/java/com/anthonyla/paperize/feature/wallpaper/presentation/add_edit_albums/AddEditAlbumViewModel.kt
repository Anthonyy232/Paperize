package com.anthonyla.paperize.feature.wallpaper.presentation.add_edit_albums

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.anthonyla.paperize.feature.wallpaper.use_case.AlbumUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class AddEditAlbumViewModel @Inject constructor(
    private val albumUseCases: AlbumUseCases
) : ViewModel() {

    private val _albumName = mutableStateOf("")
    val albumName: State<String> = _albumName

    private val _eventFlow = MutableSharedFlow<UiEvent>()

    sealed class UiEvent {
        object SaveAlbum
    }

    fun onEvent(event: AddEditAlbumEvent) {
        when(event) {
            is AddEditAlbumEvent.AddTitle -> {
                _albumName.value = event.value
            }
            is AddEditAlbumEvent.AddImages -> {

            }
            is AddEditAlbumEvent.SaveAlbum -> {

            }
        }
    }

}