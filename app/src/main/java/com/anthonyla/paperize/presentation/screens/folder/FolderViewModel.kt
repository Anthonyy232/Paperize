package com.anthonyla.paperize.presentation.screens.folder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.presentation.common.navigation.FolderRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val folderRoute = savedStateHandle.toRoute<FolderRoute>()
    private val folderId = folderRoute.folderId

    private val _folder = MutableStateFlow<Folder?>(null)
    val folder: StateFlow<Folder?> = _folder

    init {
        loadFolder()
    }

    private fun loadFolder() {
        viewModelScope.launch {
            // TODO: Implement getFolderById in repository
            // For now, we can load from album and find the folder
        }
    }
}
