package com.anthonyla.paperize.presentation.screens.sort

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.presentation.common.navigation.SortRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the sort view screen to hold the folders and wallpapers
 */
@HiltViewModel
class SortViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val wallpaperRepository: WallpaperRepository
) : ViewModel() {
    private val sortRoute = savedStateHandle.toRoute<SortRoute>()
    private val albumId: String = sortRoute.albumId

    private val _state = MutableStateFlow(SortState())
    val state = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SortState()
    )

    init {
        loadAlbumData()
    }

    private fun loadAlbumData() {
        viewModelScope.launch {
            val album = albumRepository.getAlbumById(albumId).first()
            val wallpapers = wallpaperRepository.getWallpapersByAlbum(albumId).first()

            _state.value = state.value.copy(
                folders = album?.folders ?: emptyList(),
                wallpapers = wallpapers
            )
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            var hasError = false

            // Update folders with new display orders
            state.value.folders.forEach { folder ->
                when (albumRepository.updateFolder(folder)) {
                    is com.anthonyla.paperize.core.Result.Success -> { /* Success */ }
                    is com.anthonyla.paperize.core.Result.Error -> hasError = true
                    is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
                }
            }

            // Update wallpapers with new display orders
            state.value.wallpapers.forEach { wallpaper ->
                when (wallpaperRepository.updateWallpaper(wallpaper)) {
                    is com.anthonyla.paperize.core.Result.Success -> { /* Success */ }
                    is com.anthonyla.paperize.core.Result.Error -> hasError = true
                    is com.anthonyla.paperize.core.Result.Loading -> { /* Loading state not used */ }
                }
            }
        }
    }

    fun onEvent(event: SortEvent) {
        when (event) {
            is SortEvent.LoadSortView -> {
                _state.value = state.value.copy(
                    folders = event.folders,
                    wallpapers = event.wallpapers
                )
            }

            is SortEvent.ShiftFolder -> {
                val currentFolders = state.value.folders.toMutableList()
                val fromIndex = currentFolders.indexOfFirst { it.uri == event.from.key }
                val toIndex = currentFolders.indexOfFirst { it.uri == event.to.key }
                val movedFolder = currentFolders.removeAt(fromIndex)
                currentFolders.add(toIndex, movedFolder)
                currentFolders.forEachIndexed { index, folder ->
                    currentFolders[index] = folder.copy(displayOrder = index)
                }
                _state.value = state.value.copy(folders = currentFolders)
            }

            is SortEvent.ShiftFolderWallpaper -> {
                val folder = state.value.folders.find { it.id == event.folderId }
                val currentWallpapers = folder?.wallpapers?.toMutableList() ?: return
                val fromIndex = currentWallpapers.indexOfFirst { it.uri == event.from.key }
                val toIndex = currentWallpapers.indexOfFirst { it.uri == event.to.key }
                val movedWallpaper = currentWallpapers.removeAt(fromIndex)
                currentWallpapers.add(toIndex, movedWallpaper)
                currentWallpapers.forEachIndexed { index, wallpaper ->
                    currentWallpapers[index] = wallpaper.copy(displayOrder = index)
                }
                val updatedFolders = state.value.folders.map { folder ->
                    if (folder.id == event.folderId) {
                        folder.copy(
                            wallpapers = currentWallpapers,
                            coverUri = currentWallpapers.firstOrNull()?.uri ?: folder.coverUri
                        )
                    } else {
                        folder
                    }
                }
                _state.value = state.value.copy(folders = updatedFolders)
            }

            is SortEvent.ShiftWallpaper -> {
                val currentWallpapers = state.value.wallpapers.toMutableList()
                val fromIndex = currentWallpapers.indexOfFirst { it.uri == event.from.key }
                val toIndex = currentWallpapers.indexOfFirst { it.uri == event.to.key }
                val movedWallpaper = currentWallpapers.removeAt(fromIndex)
                currentWallpapers.add(toIndex, movedWallpaper)
                currentWallpapers.forEachIndexed { index, wallpaper ->
                    currentWallpapers[index] = wallpaper.copy(displayOrder = index)
                }
                _state.value = state.value.copy(wallpapers = currentWallpapers)
            }

            is SortEvent.Reset -> {
                _state.value = SortState()
            }

            is SortEvent.SortAlphabetically -> {
                val sortedFolders = state.value.folders
                    .map { folder ->
                        val sortedFolderWallpapers = folder.wallpapers
                            .sortedBy { it.fileName }
                            .mapIndexed { index, wallpaper ->
                                wallpaper.copy(displayOrder = index)
                            }
                        folder.copy(wallpapers = sortedFolderWallpapers)
                    }
                    .sortedBy { it.name }
                    .mapIndexed { index, folder ->
                        folder.copy(displayOrder = index)
                    }

                val sortedWallpapers = state.value.wallpapers
                    .sortedBy { it.fileName }
                    .mapIndexed { index, wallpaper ->
                        wallpaper.copy(displayOrder = index)
                    }

                _state.value = state.value.copy(
                    folders = sortedFolders,
                    wallpapers = sortedWallpapers
                )
            }

            is SortEvent.SortAlphabeticallyReverse -> {
                val sortedFolders = state.value.folders
                    .map { folder ->
                        val sortedFolderWallpapers = folder.wallpapers
                            .sortedByDescending { it.fileName }
                            .mapIndexed { index, wallpaper ->
                                wallpaper.copy(displayOrder = index)
                            }
                        folder.copy(wallpapers = sortedFolderWallpapers)
                    }
                    .sortedByDescending { it.name }
                    .mapIndexed { index, folder ->
                        folder.copy(displayOrder = index)
                    }

                val sortedWallpapers = state.value.wallpapers
                    .sortedByDescending { it.fileName }
                    .mapIndexed { index, wallpaper ->
                        wallpaper.copy(displayOrder = index)
                    }

                _state.value = state.value.copy(
                    folders = sortedFolders,
                    wallpapers = sortedWallpapers
                )
            }

            is SortEvent.SortByLastModified -> {
                val sortedFolders = state.value.folders
                    .map { folder ->
                        val sortedFolderWallpapers = folder.wallpapers
                            .sortedBy { it.dateModified }
                            .mapIndexed { index, wallpaper ->
                                wallpaper.copy(displayOrder = index)
                            }
                        folder.copy(wallpapers = sortedFolderWallpapers)
                    }
                    .sortedBy { it.dateModified }
                    .mapIndexed { index, folder ->
                        folder.copy(displayOrder = index)
                    }

                val sortedWallpapers = state.value.wallpapers
                    .sortedBy { it.dateModified }
                    .mapIndexed { index, wallpaper ->
                        wallpaper.copy(displayOrder = index)
                    }

                _state.value = state.value.copy(
                    folders = sortedFolders,
                    wallpapers = sortedWallpapers
                )
            }

            is SortEvent.SortByLastModifiedReverse -> {
                val sortedFolders = state.value.folders
                    .map { folder ->
                        val sortedFolderWallpapers = folder.wallpapers
                            .sortedByDescending { it.dateModified }
                            .mapIndexed { index, wallpaper ->
                                wallpaper.copy(displayOrder = index)
                            }
                        folder.copy(wallpapers = sortedFolderWallpapers)
                    }
                    .sortedByDescending { it.dateModified }
                    .mapIndexed { index, folder ->
                        folder.copy(displayOrder = index)
                    }

                val sortedWallpapers = state.value.wallpapers
                    .sortedByDescending { it.dateModified }
                    .mapIndexed { index, wallpaper ->
                        wallpaper.copy(displayOrder = index)
                    }

                _state.value = state.value.copy(
                    folders = sortedFolders,
                    wallpapers = sortedWallpapers
                )
            }
        }
    }
}
