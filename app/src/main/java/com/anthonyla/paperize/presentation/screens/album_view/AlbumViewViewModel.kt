package com.anthonyla.paperize.presentation.screens.album_view
import com.anthonyla.paperize.core.constants.Constants

import android.util.Log
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.usecase.ImportWallpapersUseCase
import com.anthonyla.paperize.domain.usecase.DeleteAlbumUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.presentation.common.navigation.AlbumRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
            initialValue = null
        )

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
            try {
                if (!block()) _message.value = R.string.import_already_added
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                Log.e(TAG, "Import permission failed", e)
                _message.value = R.string.import_permission_error
            } catch (e: Exception) {
                Log.e(TAG, "Import failed", e)
                _message.value = R.string.import_failed
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
                deleteAlbumUseCase(albumId)
                    .getOrThrow()
                _albumDeleted.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting album", e)
                _message.value = R.string.delete_album_failed
            } finally {
                _isDeleting.value = false
            }
        }
    }

    fun toggleWallpaperSelection(wallpaperId: String) {
        if (_isDeleting.value) return
        _selectedWallpapers.value = if (wallpaperId in _selectedWallpapers.value) {
            _selectedWallpapers.value - wallpaperId
        } else {
            _selectedWallpapers.value + wallpaperId
        }
    }

    fun toggleFolderSelection(folderId: String) {
        if (_isDeleting.value) return
        _selectedFolders.value = if (folderId in _selectedFolders.value) {
            _selectedFolders.value - folderId
        } else {
            _selectedFolders.value + folderId
        }
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
                    when (albumRepository.removeWallpapersFromAlbum(albumId, wallpaperIds)) {
                        is Result.Success -> _selectedWallpapers.value -= wallpaperIds.toSet()
                        else -> _message.value = R.string.delete_items_failed
                    }
                }
                folderIds.forEach { folderId ->
                    when (albumRepository.removeFolderFromAlbum(albumId, folderId)) {
                        is Result.Success -> _selectedFolders.value -= folderId
                        else -> _message.value = R.string.delete_items_failed
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error removing selected items", e)
                _message.value = R.string.delete_items_failed
            } finally {
                _isDeleting.value = false
            }
        }
    }
}
