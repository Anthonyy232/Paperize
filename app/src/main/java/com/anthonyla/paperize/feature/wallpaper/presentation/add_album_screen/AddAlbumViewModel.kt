package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen


import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.use_case.AlbumsUseCases
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.List.copyOf
import javax.inject.Inject

@HiltViewModel
class AddAlbumViewModel @Inject constructor(
    application: Application,
    private val albumsUseCases: AlbumsUseCases,
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
                }
            }
            is AddAlbumEvent.SetAlbumName -> {
                viewModelScope.launch {
                    _state.update { it.copy(
                        initialAlbumName = event.initialAlbumName,
                        displayedAlbumName = event.initialAlbumName,
                    ) }
                }
            }

            is AddAlbumEvent.ReflectAlbumName -> {
                viewModelScope.launch {
                    _state.update { it.copy(
                        initialAlbumName = event.newAlbumName,
                        displayedAlbumName = event.newAlbumName,
                        coverUri = it.coverUri,
                        wallpapers = it.wallpapers.map { wallpaper ->
                            wallpaper.copy(initialAlbumName = event.newAlbumName)
                        },
                        folders = it.folders.map { folder ->
                            folder.copy(initialAlbumName = event.newAlbumName)
                        },
                    ) }
                    updateIsEmpty()
                }
            }
            is AddAlbumEvent.AddWallpaper -> {
                viewModelScope.launch {
                    if (!state.value.wallpapers.any { it.wallpaperUri == event.wallpaperUri }) {
                        _state.update { it.copy(
                            wallpapers = it.wallpapers.plus(
                                Wallpaper(it.initialAlbumName, event.wallpaperUri)
                            ),
                        ) }
                        updateIsEmpty()
                    }
                }
            }
            is AddAlbumEvent.DeleteWallpaper -> {
                viewModelScope.launch {
                    val wallpaperToRemove = state.value.wallpapers.find { it.wallpaperUri == event.wallpaperUri }
                    if (wallpaperToRemove != null) {
                        _state.update { it.copy(
                            wallpapers = it.wallpapers.minus(wallpaperToRemove),
                        ) }
                        updateIsEmpty()
                    }
                }
            }
            is AddAlbumEvent.AddFolder -> {
                viewModelScope.launch {
                    if (!state.value.folders.any { it.folderUri == event.directoryUri }) {
                        val wallpapers = getWallpaperFromFolder(event.directoryUri, context)
                        val folderName = getFolderNameFromUri(event.directoryUri, context)
                        _state.update { it.copy(
                            folders = it.folders.plus(
                                Folder(
                                    initialAlbumName = it.initialAlbumName,
                                    folderName = folderName,
                                    folderUri = event.directoryUri,
                                    coverUri = wallpapers.random(),
                                    wallpapers = wallpapers
                                )
                            ),
                        ) }
                        updateIsEmpty()
                    }
                }
            }
            is AddAlbumEvent.DeleteFolder -> {
                viewModelScope.launch {
                    val folderToRemove = state.value.folders.find { it.folderUri == event.directoryUri }
                    if (folderToRemove != null) {
                        _state.update { it.copy(
                            folders = it.folders.minus(folderToRemove)
                        ) }
                        updateIsEmpty()
                    }
                }
            }
            is AddAlbumEvent.SelectAll -> {
                viewModelScope.launch {
                    if (!state.value.allSelected) {
                        _state.update { it.copy(
                            selectedFolders = state.value.folders.map { folder -> folder.folderUri },
                            selectedWallpapers = state.value.wallpapers.map { wallpaper -> wallpaper.wallpaperUri },
                            selectedCount = it.folders.size + it.wallpapers.size
                        ) }
                        updateAllSelected()
                    }
                }
            }
            is AddAlbumEvent.DeselectAll-> {
                viewModelScope.launch {
                    _state.update { it.copy(
                        selectedFolders = emptyList(),
                        selectedWallpapers = emptyList(),
                        selectedCount = 0,
                        allSelected = false
                    ) }
                }
            }
            is AddAlbumEvent.SelectFolder -> {
                viewModelScope.launch {
                    if (!state.value.selectedFolders.any { it == event.directoryUri }) {
                        _state.update { it.copy(
                            selectedFolders = it.selectedFolders.plus(event.directoryUri),
                            selectedCount = it.selectedCount + 1
                        ) }
                        updateAllSelected()
                    }
                }
            }
            is AddAlbumEvent.SelectWallpaper -> {
                viewModelScope.launch {
                    if (!state.value.selectedWallpapers.any { it == event.wallpaperUri }) {
                        _state.update { it.copy(
                            selectedWallpapers = it.selectedWallpapers.plus(event.wallpaperUri),
                            selectedCount = it.selectedCount + 1
                        ) }
                        updateAllSelected()
                    }
                }
            }
            is AddAlbumEvent.RemoveFolderFromSelection -> {
                viewModelScope.launch {
                    if (state.value.selectedFolders.find { it == event.directoryUri } != null ) {
                        _state.update { it.copy(
                            selectedFolders = it.selectedFolders.minus(event.directoryUri),
                            selectedCount = it.selectedCount - 1
                        ) }
                        updateAllSelected()
                    }
                }
            }
            is AddAlbumEvent.RemoveWallpaperFromSelection -> {
                viewModelScope.launch {
                    if (state.value.selectedWallpapers.find { it == event.wallpaperUri } != null ) {
                        _state.update { it.copy(
                            selectedWallpapers = it.selectedWallpapers.minus(event.wallpaperUri),
                            selectedCount = it.selectedCount - 1
                        ) }
                        updateAllSelected()
                    }
                }
            }
            is AddAlbumEvent.ClearState -> {
                viewModelScope.launch {
                    _state.update { it.copy(
                        initialAlbumName = "",
                        displayedAlbumName = "",
                        coverUri = "",
                        wallpapers = emptyList(),
                        folders = emptyList(),
                        selectedFolders = emptyList(),
                        selectedWallpapers = emptyList(),
                        isEmpty = false,
                        allSelected = false,
                        selectedCount = 0,
                    ) }
                }
            }
        }
    }
    private fun updateIsEmpty() {
        viewModelScope.launch {
            _state.update { it.copy(
                isEmpty = it.wallpapers.isEmpty() && it.folders.isEmpty()
            ) }
        }
    }

    private fun updateAllSelected() {
        viewModelScope.launch {
            _state.update { it.copy(
                allSelected = it.selectedFolders.size + it.selectedWallpapers.size >= it.wallpapers.size + it.folders.size
            ) }
        }
    }
}

