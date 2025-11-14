package com.anthonyla.paperize.presentation.screens.album

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.domain.usecase.AddWallpapersToAlbumUseCase
import com.anthonyla.paperize.domain.usecase.DeleteAlbumUseCase
import com.anthonyla.paperize.domain.usecase.RefreshAlbumUseCase
import com.anthonyla.paperize.domain.usecase.ScanFolderUseCase
import com.anthonyla.paperize.presentation.common.navigation.AlbumRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val addWallpapersToAlbumUseCase: AddWallpapersToAlbumUseCase,
    private val scanFolderUseCase: ScanFolderUseCase,
    private val refreshAlbumUseCase: RefreshAlbumUseCase,
    private val deleteAlbumUseCase: DeleteAlbumUseCase
) : ViewModel() {

    private val albumRoute = savedStateHandle.toRoute<AlbumRoute>()
    private val albumId = albumRoute.albumId

    val album: StateFlow<Album?> = albumRepository.getAlbumById(albumId)
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun refreshAlbum() {
        viewModelScope.launch {
            _isLoading.value = true
            refreshAlbumUseCase(albumId)
            _isLoading.value = false
        }
    }

    fun deleteAlbum(onDeleted: () -> Unit) {
        viewModelScope.launch {
            when (deleteAlbumUseCase(albumId)) {
                is Result.Success -> onDeleted()
                is Result.Error -> { /* Handle error */ }
                else -> {}
            }
        }
    }

    fun toggleAlbumSelection() {
        viewModelScope.launch {
            val current = album.value ?: return@launch
            albumRepository.setAlbumSelected(albumId, !current.isSelected)
        }
    }

    fun updateAlbumName(newName: String) {
        viewModelScope.launch {
            val current = album.value ?: return@launch
            albumRepository.updateAlbum(current.copy(name = newName))
        }
    }

    fun addWallpapers(uris: List<Uri>) {
        viewModelScope.launch {
            _isLoading.value = true
            addWallpapersToAlbumUseCase(albumId, uris)
            _isLoading.value = false
        }
    }

    fun addFolder(folderUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            scanFolderUseCase(albumId, folderUri)
            _isLoading.value = false
        }
    }

    fun deleteWallpapers(wallpaperIds: List<String>) {
        viewModelScope.launch {
            wallpaperIds.forEach { wallpaperId ->
                wallpaperRepository.deleteWallpaper(wallpaperId)
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            wallpaperRepository.deleteWallpapersByFolderId(folderId)
        }
    }
}
