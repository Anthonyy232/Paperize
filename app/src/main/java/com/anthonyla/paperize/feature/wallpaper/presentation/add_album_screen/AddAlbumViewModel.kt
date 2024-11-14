package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen


import android.R.attr.order
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.getFolderLastModified
import com.anthonyla.paperize.core.getFolderNameFromUri
import com.anthonyla.paperize.core.getImageMetadata
import com.anthonyla.paperize.core.getWallpaperFromFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddAlbumViewModel @Inject constructor(
    application: Application,
    private val repository: AlbumRepository,
) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication<Application>().applicationContext
    private val _state = MutableStateFlow(AddAlbumState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000), AddAlbumState()
    )

    fun onEvent(event: AddAlbumEvent) {
        when (event) {
            is AddAlbumEvent.SaveAlbum -> {
                viewModelScope.launch {
                    if (_state.value.wallpapers.isEmpty() && _state.value.folders.isEmpty()) { return@launch }
                    val wallpapers = _state.value.wallpapers.map {
                        it.copy(
                            initialAlbumName = event.initialAlbumName,
                            key = event.initialAlbumName.hashCode() + it.wallpaperUri.hashCode()
                        )
                    }
                    val folders = _state.value.folders.map { folder ->
                        folder.copy(
                            initialAlbumName = event.initialAlbumName,
                            key = event.initialAlbumName.hashCode() + folder.hashCode(),
                            wallpapers = folder.wallpapers.map {
                                it.copy(
                                    initialAlbumName = event.initialAlbumName,
                                    key = event.initialAlbumName.hashCode() + folder.folderUri.hashCode() + it.wallpaperUri.hashCode()
                                )
                            }
                        )
                    }
                    val totalWallpapers: List<Wallpaper> = folders.flatMap { it.wallpapers } + wallpapers
                    val albumWithWallpaperAndFolder = AlbumWithWallpaperAndFolder(
                        album = Album(
                            initialAlbumName = event.initialAlbumName,
                            displayedAlbumName = event.initialAlbumName,
                            coverUri = wallpapers.firstOrNull()?.wallpaperUri ?: folders.firstOrNull()?.coverUri,
                            homeWallpapersInQueue = emptyList(),
                            lockWallpapersInQueue = emptyList(),
                            initialized = false,
                            selected = false
                        ),
                        wallpapers = wallpapers,
                        folders = folders,
                        totalWallpapers = totalWallpapers
                    )
                    repository.upsertAlbumWithWallpaperAndFolder(albumWithWallpaperAndFolder)
                    _state.update { AddAlbumState() }
                }
            }

            is AddAlbumEvent.DeleteSelected -> {
                viewModelScope.launch {
                    val selectedWallpaperUris = _state.value.selectionState.selectedWallpapers.toSet()
                    val selectedFolderUris = _state.value.selectionState.selectedFolders.toSet()
                    val wallpapersRemoved = _state.value.wallpapers.filter { wallpaper ->
                        wallpaper.wallpaperUri !in selectedWallpaperUris
                    }
                    val foldersRemoved = _state.value.folders.filter { folder ->
                        folder.folderUri !in selectedFolderUris
                    }
                    _state.update {
                        it.copy(
                            wallpapers = wallpapersRemoved,
                            folders = foldersRemoved,
                            isEmpty = wallpapersRemoved.isEmpty() && foldersRemoved.isEmpty(),
                            selectionState = SelectionState()
                        )
                    }
                }
            }

            is AddAlbumEvent.AddWallpapers -> {
                viewModelScope.launch {
                    val existingWallpapers = _state.value.wallpapers.map { it.wallpaperUri }.toSet()
                    val newWallpapers = event.wallpaperUris.filter { wallpaperUri ->
                        wallpaperUri !in existingWallpapers
                    }
                    _state.update {
                        it.copy(
                            wallpapers = it.wallpapers.plus(
                                newWallpapers.mapIndexed { index, wallpaperUri ->
                                    val metadata = getImageMetadata(context, wallpaperUri)
                                    Wallpaper(
                                        initialAlbumName = "",
                                        wallpaperUri = wallpaperUri,
                                        fileName = metadata.filename,
                                        dateModified = metadata.lastModified,
                                        order = index + it.wallpapers.size,
                                        key = 0
                                    )
                                }
                            ),
                            isEmpty = event.wallpaperUris.isEmpty(),
                            selectionState = SelectionState(),
                            isLoading = false
                        )
                    }
                }
            }

            is AddAlbumEvent.AddFolder -> {
                viewModelScope.launch {
                    if (event.directoryUri in _state.value.folders.map { it.folderUri }) { return@launch }
                    _state.update { it.copy(isLoading = true) }
                    val wallpapers = getWallpaperFromFolder(event.directoryUri, context)
                    val folderName = getFolderNameFromUri(event.directoryUri, context)
                    val lastModified = getFolderLastModified(event.directoryUri, context)
                    val folder = Folder(
                        initialAlbumName = "",
                        folderName = folderName,
                        folderUri = event.directoryUri,
                        wallpapers = wallpapers,
                        coverUri = wallpapers.firstOrNull()?.wallpaperUri ?: "",
                        dateModified = lastModified,
                        order = _state.value.folders.size + 1,
                        key = 0
                    )
                    _state.update {
                        it.copy(
                            folders = it.folders.plus(folder),
                            isEmpty = false,
                            selectionState = SelectionState(),
                            isLoading = false
                        )
                    }
                }
            }

            is AddAlbumEvent.SelectAll -> {
                viewModelScope.launch {
                    if (!_state.value.selectionState.allSelected) {
                        _state.update {
                            it.copy(
                                selectionState = SelectionState(
                                    selectedFolders = it.folders.map { it.folderUri },
                                    selectedWallpapers = it.wallpapers.map { it.wallpaperUri },
                                    allSelected = true,
                                    selectedCount = it.folders.size + it.wallpapers.size
                                )
                            )
                        }
                    }
                }
            }
            is AddAlbumEvent.DeselectAll-> {
                viewModelScope.launch {
                    _state.update { it.copy(selectionState = SelectionState()) }
                }
            }

            is AddAlbumEvent.SelectFolder -> {
                viewModelScope.launch {
                    val folderUri = (_state.value.folders.find { it.folderUri == event.directoryUri } ?: return@launch).folderUri
                    _state.update {
                        val newSelectedFolders = it.selectionState.selectedFolders.plus(folderUri)
                        it.copy(
                            selectionState = it.selectionState.copy(
                                selectedFolders = newSelectedFolders,
                                selectedCount = newSelectedFolders.size + it.selectionState.selectedWallpapers.size,
                                allSelected = newSelectedFolders.size + it.selectionState.selectedWallpapers.size >=
                                        it.wallpapers.size + it.folders.size
                            )
                        )
                    }
                }
            }

            is AddAlbumEvent.SelectWallpaper -> {
                viewModelScope.launch {
                    if (!_state.value.selectionState.selectedWallpapers.contains(event.wallpaperUri)) {
                        _state.update {
                            val newSelectedWallpapers = it.selectionState.selectedWallpapers.plus(event.wallpaperUri)
                            it.copy(
                                selectionState = it.selectionState.copy(
                                    selectedWallpapers = newSelectedWallpapers,
                                    selectedCount = newSelectedWallpapers.size + it.selectionState.selectedFolders.size,
                                    allSelected = it.selectionState.selectedFolders.size + newSelectedWallpapers.size >=
                                        it.wallpapers.size + it.folders.size
                                )
                            )
                        }
                    }
                }
            }

            is AddAlbumEvent.DeselectFolder -> {
                viewModelScope.launch {
                    val folderUri = (_state.value.folders.find { it.folderUri == event.directoryUri } ?: return@launch).folderUri
                    _state.update {
                        val newSelectedFolders = it.selectionState.selectedFolders.minus(folderUri)
                        it.copy(
                            selectionState = it.selectionState.copy(
                                selectedFolders = newSelectedFolders,
                                selectedCount = newSelectedFolders.size + it.selectionState.selectedWallpapers.size,
                                allSelected = false
                            )
                        )
                    }
                }
            }

            is AddAlbumEvent.DeselectWallpaper -> {
                viewModelScope.launch {
                    if (_state.value.selectionState.selectedWallpapers.contains(event.wallpaperUri)) {
                        _state.update {
                            val newSelectedWallpapers = it.selectionState.selectedWallpapers.minus(event.wallpaperUri)
                            it.copy(
                                selectionState = it.selectionState.copy(
                                    selectedWallpapers = newSelectedWallpapers,
                                    selectedCount = newSelectedWallpapers.size + it.selectionState.selectedFolders.size,
                                    allSelected = false
                                )
                            )
                        }
                    }
                }
            }

            is AddAlbumEvent.LoadFoldersAndWallpapers -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            folders = event.folders,
                            wallpapers = event.wallpapers,
                            isEmpty = event.folders.isEmpty() && event.wallpapers.isEmpty(),
                            selectionState = SelectionState(),
                            isLoading = false
                        )
                    }
                }
            }

            is AddAlbumEvent.SetLoading -> {
                viewModelScope.launch {
                    _state.update { it.copy(isLoading = event.isLoading) }
                }
            }

            is AddAlbumEvent.Reset -> {
                viewModelScope.launch {
                    _state.update { AddAlbumState() }
                }
            }
        }
    }
}