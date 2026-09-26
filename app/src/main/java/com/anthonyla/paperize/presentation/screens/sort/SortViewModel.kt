package com.anthonyla.paperize.presentation.screens.sort

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.util.WallpaperSorter
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.presentation.common.navigation.SortRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class SortViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SortViewModel"
    }

    private val albumId = savedStateHandle.toRoute<SortRoute>().albumId

    private val _state = MutableStateFlow(SortState())
    val state = _state.asStateFlow()

    init {
        loadAlbumData()
    }

    private fun loadAlbumData() {
        viewModelScope.launch {
            try {
                val album = checkNotNull(albumRepository.getAlbumById(albumId).first())
                _state.value = _state.value.copy(
                    folders = album.folders.sortedBy { it.displayOrder }.map { folder ->
                        folder.copy(wallpapers = folder.wallpapers.sortedBy { it.displayOrder })
                    },
                    wallpapers = album.wallpapers.sortedBy { it.displayOrder },
                    isLoading = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sort data", e)
                _state.value = _state.value.copy(error = R.string.sort_load_failed)
            }
        }
    }

    fun dismissError() { _state.value = _state.value.copy(error = null) }

    fun saveChanges() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isSaving || snapshot.saved) return
        _state.value = snapshot.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                albumRepository.reorderAlbum(albumId, snapshot.folders, snapshot.wallpapers)
                    .getOrThrow()
                _state.value = _state.value.copy(saved = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error saving order", e)
                _state.value = _state.value.copy(error = R.string.sort_save_failed)
            } finally {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }

    fun onEvent(event: SortEvent) {
        if (_state.value.isLoading || _state.value.isSaving || _state.value.saved) return
        when (event) {
            is SortEvent.ShiftFolder -> {
                val fromUri = event.from.key as? String ?: return
                val toUri = event.to.key as? String ?: return
                val updatedFolders = WallpaperSorter.shiftFolder(
                    folders = state.value.folders,
                    fromUri = fromUri,
                    toUri = toUri
                )
                _state.value = _state.value.copy(folders = updatedFolders)
            }

            is SortEvent.ShiftFolderWallpaper -> {
                val fromUri = event.from.key as? String ?: return
                val toUri = event.to.key as? String ?: return
                val updatedFolders = WallpaperSorter.shiftWallpaperInFolder(
                    folders = state.value.folders,
                    folderId = event.folderId,
                    fromUri = fromUri,
                    toUri = toUri
                )
                _state.value = _state.value.copy(folders = updatedFolders)
            }

            is SortEvent.ShiftWallpaper -> {
                val fromUri = event.from.key as? String ?: return
                val toUri = event.to.key as? String ?: return
                val updatedWallpapers = WallpaperSorter.shiftWallpaper(
                    wallpapers = state.value.wallpapers,
                    fromUri = fromUri,
                    toUri = toUri
                )
                _state.value = _state.value.copy(wallpapers = updatedWallpapers)
            }

            is SortEvent.SortAlphabetically -> {
                val (sortedFolders, sortedWallpapers) = WallpaperSorter.sortAllAlphabetically(
                    folders = state.value.folders,
                    wallpapers = state.value.wallpapers,
                    ascending = true
                )
                _state.value = _state.value.copy(
                    folders = sortedFolders,
                    wallpapers = sortedWallpapers
                )
            }

            is SortEvent.SortAlphabeticallyReverse -> {
                val (sortedFolders, sortedWallpapers) = WallpaperSorter.sortAllAlphabetically(
                    folders = state.value.folders,
                    wallpapers = state.value.wallpapers,
                    ascending = false
                )
                _state.value = _state.value.copy(
                    folders = sortedFolders,
                    wallpapers = sortedWallpapers
                )
            }

            is SortEvent.SortByLastModified -> {
                val (sortedFolders, sortedWallpapers) = WallpaperSorter.sortAllByDateModified(
                    folders = state.value.folders,
                    wallpapers = state.value.wallpapers,
                    ascending = true
                )
                _state.value = _state.value.copy(
                    folders = sortedFolders,
                    wallpapers = sortedWallpapers
                )
            }

            is SortEvent.SortByLastModifiedReverse -> {
                val (sortedFolders, sortedWallpapers) = WallpaperSorter.sortAllByDateModified(
                    folders = state.value.folders,
                    wallpapers = state.value.wallpapers,
                    ascending = false
                )
                _state.value = _state.value.copy(
                    folders = sortedFolders,
                    wallpapers = sortedWallpapers
                )
            }
        }
    }
}
