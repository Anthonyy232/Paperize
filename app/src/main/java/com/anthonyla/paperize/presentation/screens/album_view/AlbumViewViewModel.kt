package com.anthonyla.paperize.presentation.screens.album_view

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.core.util.detectMediaType
import com.anthonyla.paperize.core.util.generateId
import com.anthonyla.paperize.core.util.getFileName
import com.anthonyla.paperize.core.util.scanFolderForImages
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

    private val albumRoute = savedStateHandle.toRoute<AlbumRoute>()
    private val albumId: String = albumRoute.albumId

    val album: StateFlow<Album?> = albumRepository.getAlbumById(albumId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val folders: StateFlow<List<Folder>> = album
        .map { it?.folders ?: emptyList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Only get direct wallpapers (not those in folders)
    val wallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getDirectWallpapersByAlbum(albumId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selection state
    private val _selectedWallpapers = MutableStateFlow<Set<String>>(emptySet())
    val selectedWallpapers: StateFlow<Set<String>> = _selectedWallpapers.asStateFlow()

    private val _selectedFolders = MutableStateFlow<Set<String>>(emptySet())
    val selectedFolders: StateFlow<Set<String>> = _selectedFolders.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    fun addWallpapers(uris: List<String>) {
        viewModelScope.launch {
            val wallpapers = uris.mapIndexed { index, uri ->
                val parsedUri = uri.toUri()
                val mediaType = parsedUri.detectMediaType(context) ?: WallpaperMediaType.IMAGE

                Wallpaper(
                    id = generateId(),
                    albumId = albumId,
                    folderId = null,
                    uri = uri,
                    fileName = parsedUri.getFileName(context) ?: uri.substringAfterLast('/'),
                    dateModified = System.currentTimeMillis(),
                    displayOrder = index,
                    mediaType = mediaType
                )
            }
            when (albumRepository.addWallpapersToAlbum(albumId, wallpapers)) {
                is com.anthonyla.paperize.core.Result.Success -> { /* Success */ }
                is com.anthonyla.paperize.core.Result.Error -> { /* Handle error */ }
                is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
            }
        }
    }

    fun addFolder(uri: String) {
        viewModelScope.launch {
            // Scan folder for images on IO dispatcher
            val imageUris = withContext(Dispatchers.IO) {
                uri.toUri().scanFolderForImages(context)
            }

            // Create folder with scanned wallpapers
            val folderId = generateId()
            val wallpapers = imageUris.mapIndexed { index, imageUri ->
                val mediaType = imageUri.detectMediaType(context) ?: WallpaperMediaType.IMAGE

                Wallpaper(
                    id = generateId(),
                    albumId = albumId,
                    folderId = folderId,
                    uri = imageUri.toString(),
                    fileName = imageUri.getFileName(context) ?: imageUri.lastPathSegment ?: imageUri.toString().substringAfterLast('/'),
                    dateModified = System.currentTimeMillis(),
                    displayOrder = index,
                    mediaType = mediaType
                )
            }

            val folder = Folder(
                id = folderId,
                albumId = albumId,
                uri = uri,
                name = uri.toUri().getFileName(context) ?: uri.substringAfterLast('/'),
                coverUri = wallpapers.firstOrNull()?.uri, // Use first image as cover
                dateModified = System.currentTimeMillis(),
                displayOrder = 0,
                wallpapers = wallpapers
            )

            when (albumRepository.addFolderToAlbum(albumId, folder)) {
                is com.anthonyla.paperize.core.Result.Success -> { /* Success */ }
                is com.anthonyla.paperize.core.Result.Error -> { /* Handle error */ }
                is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
            }
        }
    }

    fun deleteAlbum() {
        viewModelScope.launch {
            when (albumRepository.deleteAlbum(albumId)) {
                is com.anthonyla.paperize.core.Result.Success -> { /* Success */ }
                is com.anthonyla.paperize.core.Result.Error -> { /* Handle error */ }
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
            var hasError = false

            // Batch delete wallpapers from album (includes timestamp update and cover refresh)
            if (wallpaperIds.isNotEmpty()) {
                when (albumRepository.removeWallpapersFromAlbum(albumId, wallpaperIds)) {
                    is com.anthonyla.paperize.core.Result.Success -> { /* Success */ }
                    is com.anthonyla.paperize.core.Result.Error -> hasError = true
                    is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
                }
            }

            // Delete folders (each folder deletion also deletes its wallpapers and refreshes cover)
            if (folderIds.isNotEmpty()) {
                folderIds.forEach { folderId ->
                    when (albumRepository.removeFolderFromAlbum(albumId, folderId)) {
                        is com.anthonyla.paperize.core.Result.Success -> { /* Success */ }
                        is com.anthonyla.paperize.core.Result.Error -> hasError = true
                        is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
                    }
                }
            }

            // Only clear selection if all operations succeeded
            if (!hasError) {
                clearSelection()
            }
        }
    }

    private fun updateSelectionMode() {
        _isSelectionMode.value = _selectedWallpapers.value.isNotEmpty() || _selectedFolders.value.isNotEmpty()
    }
}
