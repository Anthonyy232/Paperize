package com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.presentation.sort_view_screen.SortEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.sort_view_screen.SortState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the folder view screen to hold the folder name and the list of wallpapers
 */
class FolderViewModel @Inject constructor (): ViewModel() {
    private val _state = MutableStateFlow<FolderState>(FolderState())
    val state = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FolderState()
    )

    fun onEvent(event: FolderEvent) {
        when (event) {
            is FolderEvent.LoadFolderView -> {
                _state.value = state.value.copy(
                    folder = event.folder
                )
            }
            is FolderEvent.Reset -> {
                _state.value = FolderState()
            }
        }
    }
}