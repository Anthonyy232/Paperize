package com.anthonyla.paperize.presentation.screens.folder_view

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.runCatchingCancellable
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.usecase.RefreshFolderUseCase
import com.anthonyla.paperize.presentation.common.navigation.FolderRoute
import com.anthonyla.paperize.presentation.common.stateWhileSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FolderViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    albumRepository: AlbumRepository,
    private val refreshFolderUseCase: RefreshFolderUseCase
) : ViewModel() {

    private val folderId = savedStateHandle.toRoute<FolderRoute>().folderId

    val folder: StateFlow<Folder?> = albumRepository.getFolderById(folderId)
        .stateWhileSubscribed(viewModelScope, null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()
    private val _message = MutableStateFlow<Int?>(null)
    val message = _message.asStateFlow()

    fun dismissMessage() { _message.value = null }

    fun refresh() {
        if (_isRefreshing.value) return
        _message.value = null
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                _message.value = if (runCatchingCancellable { refreshFolderUseCase(folderId) }.isSuccess) {
                    R.string.folder_refreshed
                } else {
                    R.string.folder_refresh_failed
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
