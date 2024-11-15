package com.anthonyla.paperize.feature.wallpaper.presentation.sort_view_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the sort view screen to hold the folders and wallpapers
 */
class SortViewModel @Inject constructor (): ViewModel() {
    private val _state = MutableStateFlow<SortState>(SortState())
    val state = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SortState()
    )

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
                val fromIndex = currentFolders.indexOfFirst { it.folderUri == event.from.key }
                val toIndex = currentFolders.indexOfFirst { it.folderUri == event.to.key }
                val movedFolder = currentFolders.removeAt(fromIndex)
                currentFolders.add(toIndex, movedFolder)
                currentFolders.forEachIndexed { index, folder ->
                    currentFolders[index] = folder.copy(order = index)
                }
                _state.value = state.value.copy(folders = currentFolders)
            }

            is SortEvent.ShiftFolderWallpaper -> {
                val folder = state.value.folders.find { it.folderUri == event.folderId }
                val currentWallpapers = folder?.wallpapers?.toMutableList() ?: return
                val fromIndex = currentWallpapers.indexOfFirst { it.wallpaperUri == event.from.key }
                val toIndex = currentWallpapers.indexOfFirst { it.wallpaperUri == event.to.key }
                val movedWallpaper = currentWallpapers.removeAt(fromIndex)
                currentWallpapers.add(toIndex, movedWallpaper)
                currentWallpapers.forEachIndexed { index, wallpaper ->
                    currentWallpapers[index] = wallpaper.copy(order = index)
                }
                val updatedFolders = state.value.folders.map { folder ->
                    if (folder.folderUri == event.folderId) {
                        folder.copy(
                            wallpapers = currentWallpapers,
                            coverUri = currentWallpapers.firstOrNull()?.wallpaperUri ?: folder.coverUri
                        )
                    } else {
                        folder
                    }
                }
                _state.value = state.value.copy(folders = updatedFolders)
            }

            is SortEvent.ShiftWallpaper -> {
                val currentWallpapers = state.value.wallpapers.toMutableList()
                val fromIndex = currentWallpapers.indexOfFirst { it.wallpaperUri == event.from.key }
                val toIndex = currentWallpapers.indexOfFirst { it.wallpaperUri == event.to.key }
                val movedWallpaper = currentWallpapers.removeAt(fromIndex)
                currentWallpapers.add(toIndex, movedWallpaper)
                currentWallpapers.forEachIndexed { index, wallpaper ->
                    currentWallpapers[index] = wallpaper.copy(order = index)
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
                                wallpaper.copy(order = index)
                            }
                        folder.copy(wallpapers = sortedFolderWallpapers)
                    }
                    .sortedBy { it.folderName }
                    .mapIndexed { index, folder -> 
                        folder.copy(order = index)
                    }
                
                val sortedWallpapers = state.value.wallpapers
                    .sortedBy { it.fileName }
                    .mapIndexed { index, wallpaper -> 
                        wallpaper.copy(order = index)
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
                                wallpaper.copy(order = index)
                            }
                        folder.copy(wallpapers = sortedFolderWallpapers)
                    }
                    .sortedByDescending { it.folderName }
                    .mapIndexed { index, folder -> 
                        folder.copy(order = index)
                    }
                
                val sortedWallpapers = state.value.wallpapers
                    .sortedByDescending { it.fileName }
                    .mapIndexed { index, wallpaper -> 
                        wallpaper.copy(order = index)
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
                                wallpaper.copy(order = index)
                            }
                        folder.copy(wallpapers = sortedFolderWallpapers)
                    }
                    .sortedBy { it.dateModified }
                    .mapIndexed { index, folder -> 
                        folder.copy(order = index)
                    }
                
                val sortedWallpapers = state.value.wallpapers
                    .sortedBy { it.dateModified }
                    .mapIndexed { index, wallpaper -> 
                        wallpaper.copy(order = index)
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
                                wallpaper.copy(order = index)
                            }
                        folder.copy(wallpapers = sortedFolderWallpapers)
                    }
                    .sortedByDescending { it.dateModified }
                    .mapIndexed { index, folder -> 
                        folder.copy(order = index)
                    }
                
                val sortedWallpapers = state.value.wallpapers
                    .sortedByDescending { it.dateModified }
                    .mapIndexed { index, wallpaper -> 
                        wallpaper.copy(order = index)
                    }
                
                _state.value = state.value.copy(
                    folders = sortedFolders,
                    wallpapers = sortedWallpapers
                )
            }
        }
    }
}