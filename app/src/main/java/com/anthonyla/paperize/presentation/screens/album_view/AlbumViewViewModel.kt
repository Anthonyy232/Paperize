package com.anthonyla.paperize.presentation.screens.album_view

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.runCatchingCancellable
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.usecase.DeleteAlbumUseCase
import com.anthonyla.paperize.domain.usecase.ImportWallpapersUseCase
import com.anthonyla.paperize.presentation.common.navigation.AlbumRoute
import com.anthonyla.paperize.presentation.common.stateWhileSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AlbumViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val importWallpapersUseCase: ImportWallpapersUseCase,
    private val deleteAlbumUseCase: DeleteAlbumUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "AlbumViewViewModel"
    }

    private val albumId = savedStateHandle.toRoute<AlbumRoute>().albumId

    val album: StateFlow<Album?> = albumRepository.getAlbumById(albumId)
        .stateWhileSubscribed(viewModelScope, null)

    private val _selectedWallpapers = MutableStateFlow<Set<String>>(emptySet())
    val selectedWallpapers: StateFlow<Set<String>> = _selectedWallpapers.asStateFlow()

    private val _selectedFolders = MutableStateFlow<Set<String>>(emptySet())
    val selectedFolders: StateFlow<Set<String>> = _selectedFolders.asStateFlow()

    private val _importProgress = MutableStateFlow<ImportProgress>(ImportProgress.Idle)
    val importProgress: StateFlow<ImportProgress> = _importProgress.asStateFlow()

    private val _message = MutableStateFlow<Int?>(null)
    val message = _message.asStateFlow()
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting = _isDeleting.asStateFlow()
    private val _albumDeleted = MutableStateFlow(false)
    val albumDeleted = _albumDeleted.asStateFlow()
    private var importJob: Job? = null

    fun dismissMessage() { _message.value = null }

    fun addWallpapers(uris: List<String>) {
        if (uris.isEmpty()) return
        import(ImportProgress.Saving(0, uris.size)) {
            importWallpapersUseCase.addImages(albumId, uris) { saved, total ->
                _importProgress.value = ImportProgress.Saving(saved, total)
            }
        }
    }

    fun addFolder(uri: String) {
        import(ImportProgress.Scanning(0)) {
            importWallpapersUseCase.addFolder(
                albumId, uri,
                onScanning = { _importProgress.value = ImportProgress.Scanning(it) },
                onSaving = { saved, total -> _importProgress.value = ImportProgress.Saving(saved, total) }
            )
        }
    }

    private fun import(initialProgress: ImportProgress, block: suspend () -> Boolean) {
        if (importJob?.isCompleted == false || _isDeleting.value) return
        _message.value = null
        _importProgress.value = initialProgress
        importJob = viewModelScope.launch {
            runCatchingCancellable { block() }
                .onSuccess { added -> if (!added) _message.value = R.string.import_already_added }
                .onFailure { e ->
                    Log.e(TAG, "Import failed", e)
                    _message.value = if (e is SecurityException) {
                        R.string.import_permission_error
                    } else {
                        R.string.import_failed
                    }
                }
        }.also { job ->
            job.invokeOnCompletion { _importProgress.value = ImportProgress.Idle }
        }
    }

    fun cancelImport() { importJob?.cancel() }

    fun deleteAlbum() {
        if (_isDeleting.value || importJob?.isCompleted == false) return
        _message.value = null
        _isDeleting.value = true
        viewModelScope.launch {
            try {
                runCatchingCancellable { deleteAlbumUseCase(albumId) }
                    .onSuccess { _albumDeleted.value = true }
                    .onFailure { e ->
                        Log.e(TAG, "Error deleting album", e)
                        _message.value = R.string.delete_album_failed
                    }
            } finally {
                _isDeleting.value = false
            }
        }
    }

    fun toggleWallpaperSelection(wallpaperId: String) = toggle(_selectedWallpapers, wallpaperId)

    fun toggleFolderSelection(folderId: String) = toggle(_selectedFolders, folderId)

    private fun toggle(selection: MutableStateFlow<Set<String>>, id: String) {
        if (_isDeleting.value) return
        selection.value = if (id in selection.value) selection.value - id else selection.value + id
    }

    fun selectAll() {
        if (_isDeleting.value) return
        _selectedWallpapers.value = album.value?.wallpapers.orEmpty().map { it.id }.toSet()
        _selectedFolders.value = album.value?.folders.orEmpty().map { it.id }.toSet()
    }

    fun clearSelection() {
        if (_isDeleting.value) return
        _selectedWallpapers.value = emptySet()
        _selectedFolders.value = emptySet()
    }

    fun deleteSelected() {
        if (_isDeleting.value || importJob?.isCompleted == false) return
        _message.value = null
        val wallpaperIds = _selectedWallpapers.value.toList()
        val folderIds = _selectedFolders.value.toList()
        if (wallpaperIds.isEmpty() && folderIds.isEmpty()) return
        _isDeleting.value = true
        viewModelScope.launch {
            try {
                if (wallpaperIds.isNotEmpty()) {
                    runCatchingCancellable { albumRepository.removeWallpapersFromAlbum(albumId, wallpaperIds) }
                        .onSuccess { _selectedWallpapers.value -= wallpaperIds.toSet() }
                        .onFailure { _message.value = R.string.delete_items_failed }
                }
                folderIds.forEach { folderId ->
                    runCatchingCancellable { albumRepository.removeFolderFromAlbum(albumId, folderId) }
                        .onSuccess { _selectedFolders.value -= folderId }
                        .onFailure { _message.value = R.string.delete_items_failed }
                }
            } finally {
                _isDeleting.value = false
            }
        }
    }
}
