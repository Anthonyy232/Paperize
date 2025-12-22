package com.anthonyla.paperize.presentation.screens.sort
import com.anthonyla.paperize.core.constants.Constants

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
        started = SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
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

            // Clear queues for this album to force rebuild with new sort order
            // This ensures the wallpaper changer respects the new order immediately
            if (!hasError) {
                wallpaperRepository.clearQueuesForAlbum(albumId)
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
                val updatedFolders = com.anthonyla.paperize.core.util.WallpaperSorter.shiftFolder(
                    folders = state.value.folders,
                    fromUri = event.from.key as String,
                    toUri = event.to.key as String
                )
                _state.value = state.value.copy(folders = updatedFolders)
            }

            is SortEvent.ShiftFolderWallpaper -> {
                val updatedFolders = com.anthonyla.paperize.core.util.WallpaperSorter.shiftWallpaperInFolder(
                    folders = state.value.folders,
                    folderId = event.folderId,
                    fromUri = event.from.key as String,
                    toUri = event.to.key as String
                )
                _state.value = state.value.copy(folders = updatedFolders)
            }

            is SortEvent.ShiftWallpaper -> {
                val updatedWallpapers = com.anthonyla.paperize.core.util.WallpaperSorter.shiftWallpaper(
                    wallpapers = state.value.wallpapers,
                    fromUri = event.from.key as String,
                    toUri = event.to.key as String
                )
                _state.value = state.value.copy(wallpapers = updatedWallpapers)
            }

            is SortEvent.Reset -> {
                _state.value = SortState()
            }

            is SortEvent.SortAlphabetically -> {
                val (sortedFolders, sortedWallpapers) = com.anthonyla.paperize.core.util.WallpaperSorter.sortAllAlphabetically(
                    folders = state.value.folders,
                    wallpapers = state.value.wallpapers,
                    ascending = true
                )
                _state.value = state.value.copy(
                    folders = sortedFolders,
                    wallpapers = sortedWallpapers
                )
            }

            is SortEvent.SortAlphabeticallyReverse -> {
                val (sortedFolders, sortedWallpapers) = com.anthonyla.paperize.core.util.WallpaperSorter.sortAllAlphabetically(
                    folders = state.value.folders,
                    wallpapers = state.value.wallpapers,
                    ascending = false
                )
                _state.value = state.value.copy(
                    folders = sortedFolders,
                    wallpapers = sortedWallpapers
                )
            }

            is SortEvent.SortByLastModified -> {
                val (sortedFolders, sortedWallpapers) = com.anthonyla.paperize.core.util.WallpaperSorter.sortAllByDateModified(
                    folders = state.value.folders,
                    wallpapers = state.value.wallpapers,
                    ascending = true
                )
                _state.value = state.value.copy(
                    folders = sortedFolders,
                    wallpapers = sortedWallpapers
                )
            }

            is SortEvent.SortByLastModifiedReverse -> {
                val (sortedFolders, sortedWallpapers) = com.anthonyla.paperize.core.util.WallpaperSorter.sortAllByDateModified(
                    folders = state.value.folders,
                    wallpapers = state.value.wallpapers,
                    ascending = false
                )
                _state.value = state.value.copy(
                    folders = sortedFolders,
                    wallpapers = sortedWallpapers
                )
            }
        }
    }
}
