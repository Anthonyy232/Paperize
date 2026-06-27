package com.anthonyla.paperize.presentation.screens.album_view
import com.anthonyla.paperize.core.constants.Constants

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.core.util.detectMediaType
import com.anthonyla.paperize.core.util.generateId
import com.anthonyla.paperize.core.util.getFileName
import com.anthonyla.paperize.core.util.scanFolderImages
import com.anthonyla.paperize.core.WallpaperMediaType
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.presentation.common.navigation.AlbumRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AlbumViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val wallpaperRepository: WallpaperRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "AlbumViewViewModel"
    }

    private val albumRoute = savedStateHandle.toRoute<AlbumRoute>()
    private val albumId: String = albumRoute.albumId

    val album: StateFlow<Album?> = albumRepository.getAlbumById(albumId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
            initialValue = null
        )

    val folders: StateFlow<List<Folder>> = album
        .map { it?.folders ?: emptyList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList()
        )

    // Only get direct wallpapers (not those in folders)
    val wallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getDirectWallpapersByAlbum(albumId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList()
        )

    // Selection state
    private val _selectedWallpapers = MutableStateFlow<Set<String>>(emptySet())
    val selectedWallpapers: StateFlow<Set<String>> = _selectedWallpapers.asStateFlow()

    private val _selectedFolders = MutableStateFlow<Set<String>>(emptySet())
    val selectedFolders: StateFlow<Set<String>> = _selectedFolders.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _importProgress = MutableStateFlow<ImportProgress>(ImportProgress.Idle)
    val importProgress: StateFlow<ImportProgress> = _importProgress.asStateFlow()

    fun addWallpapers(uris: List<String>) {
        viewModelScope.launch {
            _importProgress.value = ImportProgress.Saving(saved = 0, total = uris.size)
            val existingCount = wallpapers.value.size
            val newWallpapers = uris.mapIndexed { index, uri ->
                val parsedUri = uri.toUri()
                val mediaType = parsedUri.detectMediaType(context) ?: WallpaperMediaType.IMAGE

                Wallpaper(
                    id = generateId(),
                    albumId = albumId,
                    folderId = null,
                    uri = uri,
                    fileName = parsedUri.getFileName(context) ?: uri.substringAfterLast('/'),
                    dateModified = System.currentTimeMillis(),
                    displayOrder = existingCount + index,
                    mediaType = mediaType
                )
            }
            val result = albumRepository.addWallpapersToAlbum(albumId, newWallpapers) { saved, total ->
                _importProgress.value = ImportProgress.Saving(saved, total)
            }
            when (result) {
                is com.anthonyla.paperize.core.Result.Success -> {
                    // Clear queues so the new wallpapers are included on the next cycle
                    wallpaperRepository.clearQueuesForAlbum(albumId)
                }
                is com.anthonyla.paperize.core.Result.Error -> {
                    Log.e(TAG, "Error adding wallpapers to album", result.exception)
                }
                is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
            }
            _importProgress.value = ImportProgress.Idle
        }
    }

    fun addFolder(uri: String) {
        viewModelScope.launch {
            _importProgress.value = ImportProgress.Scanning(found = 0)
            // Scan folder for images on IO dispatcher. The scan already returns each file's
            // name and modified time, so no per-file follow-up query is needed below.
            val images = withContext(Dispatchers.IO) {
                uri.toUri().scanFolderImages(context) { found ->
                    _importProgress.value = ImportProgress.Scanning(found)
                }.sortedBy { it.uri.toString() }
            }

            // Create folder with scanned wallpapers
            val folderId = generateId()
            val wallpapers = images.mapIndexed { index, image ->
                Wallpaper(
                    id = generateId(),
                    albumId = albumId,
                    folderId = folderId,
                    uri = image.uri.toString(),
                    fileName = image.name,
                    dateModified = image.lastModified,
                    displayOrder = index,
                    mediaType = WallpaperMediaType.fromExtension(image.name.substringAfterLast('.', "")) ?: WallpaperMediaType.IMAGE
                )
            }
            _importProgress.value = ImportProgress.Saving(saved = 0, total = wallpapers.size)

            val folder = Folder(
                id = folderId,
                albumId = albumId,
                uri = uri,
                name = uri.toUri().getFileName(context) ?: uri.substringAfterLast('/'),
                coverUri = wallpapers.firstOrNull()?.uri, // Use first image as cover
                dateModified = System.currentTimeMillis(),
                displayOrder = folders.value.size,
                wallpapers = wallpapers
            )

            val result = albumRepository.addFolderToAlbum(albumId, folder) { saved, total ->
                _importProgress.value = ImportProgress.Saving(saved, total)
            }
            when (result) {
                is com.anthonyla.paperize.core.Result.Success -> {
                    // Clear queues so the new folder's wallpapers are included on the next cycle
                    wallpaperRepository.clearQueuesForAlbum(albumId)
                }
                is com.anthonyla.paperize.core.Result.Error -> {
                    Log.e(TAG, "Error adding folder to album", result.exception)
                }
                is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
            }
            _importProgress.value = ImportProgress.Idle
        }
    }

    fun deleteAlbum() {
        viewModelScope.launch {
            when (val result = albumRepository.deleteAlbum(albumId)) {
                is com.anthonyla.paperize.core.Result.Success -> { /* Success */ }
                is com.anthonyla.paperize.core.Result.Error -> { 
                    Log.e(TAG, "Error deleting album", result.exception)
                }
                is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
            }
        }
    }

    // Selection functions
    fun toggleWallpaperSelection(wallpaperId: String) {
        _selectedWallpapers.value = if (wallpaperId in _selectedWallpapers.value) {
            _selectedWallpapers.value - wallpaperId
        } else {
            _selectedWallpapers.value + wallpaperId
        }
        updateSelectionMode()
    }

    fun toggleFolderSelection(folderId: String) {
        _selectedFolders.value = if (folderId in _selectedFolders.value) {
            _selectedFolders.value - folderId
        } else {
            _selectedFolders.value + folderId
        }
        updateSelectionMode()
    }

    fun selectAll() {
        _selectedWallpapers.value = wallpapers.value.map { it.id }.toSet()
        _selectedFolders.value = folders.value.map { it.id }.toSet()
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedWallpapers.value = emptySet()
        _selectedFolders.value = emptySet()
        _isSelectionMode.value = false
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val wallpaperIds = _selectedWallpapers.value.toList()
            val folderIds = _selectedFolders.value.toList()

            // Batch delete wallpapers from album (includes timestamp update and cover refresh)
            if (wallpaperIds.isNotEmpty()) {
                when (albumRepository.removeWallpapersFromAlbum(albumId, wallpaperIds)) {
                    is com.anthonyla.paperize.core.Result.Success -> {
                        _selectedWallpapers.value = _selectedWallpapers.value - wallpaperIds.toSet()
                    }
                    is com.anthonyla.paperize.core.Result.Error -> { /* Leave failed items selected */ }
                    is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
                }
            }

            // Delete folders (each folder deletion also deletes its wallpapers and refreshes cover)
            if (folderIds.isNotEmpty()) {
                folderIds.forEach { folderId ->
                    when (albumRepository.removeFolderFromAlbum(albumId, folderId)) {
                        is com.anthonyla.paperize.core.Result.Success -> {
                            _selectedFolders.value = _selectedFolders.value - folderId
                        }
                        is com.anthonyla.paperize.core.Result.Error -> { /* Leave failed item selected */ }
                        is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
                    }
                }
            }

            updateSelectionMode()
        }
    }

    private fun updateSelectionMode() {
        _isSelectionMode.value = _selectedWallpapers.value.isNotEmpty() || _selectedFolders.value.isNotEmpty()
    }
}
