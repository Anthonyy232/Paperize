package com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen


import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.compress
import com.anthonyla.paperize.core.getFolderMetadata
import com.anthonyla.paperize.core.getImageMetadata
import com.anthonyla.paperize.core.getWallpaperFromFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.SelectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.plus

@HiltViewModel
class AlbumScreenViewModel @Inject constructor(
    application: Application,
    private val repository: AlbumRepository,
) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication<Application>().applicationContext
    private val _state = MutableStateFlow<AlbumViewState>(AlbumViewState())
    val state = combine(
        loadAlbumsFlow(),
        _state
    ) { albums: List<AlbumWithWallpaperAndFolder>, currentState: AlbumViewState ->
        currentState.copy(albums = albums)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumViewState()
    )

    private fun loadAlbumsFlow() = repository.getAlbumsWithWallpaperAndFolder()

    init {
        viewModelScope.launch {
            state.collect { newState -> _state.value = newState }
        }
    }

    fun onEvent(event: AlbumViewEvent) {
        when (event) {
            is AlbumViewEvent.LoadAlbum -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(initialAlbumName = event.initialAlbumName)
                    }
                }
            }

            is AlbumViewEvent.AddWallpapers -> {
                viewModelScope.launch {
                    if (event.wallpaperUris.isEmpty()) { return@launch }
                    val album = _state.value.albums.find { it.album.initialAlbumName == _state.value.initialAlbumName }
                    if (album == null) { return@launch }
                    val existingUris = album.wallpapers.map { it.wallpaperUri }
                    val newUris = event.wallpaperUris.filter { it !in existingUris }
                    if (newUris.isEmpty()) { return@launch }
                    val wallpapers = newUris.mapIndexed { index, uri ->
                        val metadata = getImageMetadata(context, uri)
                        Wallpaper(
                            initialAlbumName = _state.value.initialAlbumName,
                            wallpaperUri = uri.compress("content://com.android.externalstorage.documents/"),
                            fileName = metadata.filename,
                            dateModified = metadata.lastModified,
                            order = if (album.wallpapers.isEmpty()) 0 else album.wallpapers.size + index,
                            key = _state.value.initialAlbumName.hashCode() + uri.hashCode()
                        )
                    }
                    var totalWallpapers = album.wallpapers.size + wallpapers.size
                    val folders = album.folders.map { folder ->
                        val updatedWallpapers = folder.wallpapers.mapIndexed { index, wallpaper ->
                            wallpaper.copy(
                                order = index + totalWallpapers
                            )
                        }
                        totalWallpapers += folder.wallpapers.size
                        folder.copy(
                            wallpapers = updatedWallpapers
                        )
                    }
                    val albumWithWallpaperAndFolder = album.copy(
                        wallpapers = album.wallpapers + wallpapers,
                        folders = folders
                    )
                    repository.upsertAlbumWithWallpaperAndFolder(albumWithWallpaperAndFolder)
                    _state.update {
                        it.copy(
                            isEmpty = false,
                            selectionState = SelectionState(),
                            isLoading = false
                        )
                    }
                }
            }

            is AlbumViewEvent.AddFolder -> {
                viewModelScope.launch {
                    val album = _state.value.albums.find { it.album.initialAlbumName == _state.value.initialAlbumName }
                    if (album == null || album.folders.any { it.folderUri == event.directoryUri }) { return@launch }
                    _state.update { it.copy(isLoading = true) }
                    val wallpapers = getWallpaperFromFolder(event.directoryUri, context).mapIndexed { index, wallpaper ->
                        wallpaper.copy(
                            initialAlbumName = _state.value.initialAlbumName,
                            key = _state.value.initialAlbumName.hashCode() + event.directoryUri.hashCode() + wallpaper.wallpaperUri.hashCode(),
                            order = index + album.wallpapers.size + album.folders.sumOf { it.wallpaperUris.size }
                        )
                    }
                    val metadata = getFolderMetadata(event.directoryUri, context)
                    val folder = Folder(
                        initialAlbumName = _state.value.initialAlbumName,
                        folderUri = event.directoryUri,
                        folderName = metadata.filename,
                        coverUri = wallpapers.map { it.wallpaperUri }.firstOrNull() ?: "",
                        dateModified = metadata.lastModified,
                        wallpaperUris = wallpapers.map { it.wallpaperUri },
                        order = album.folders.size,
                        key = _state.value.initialAlbumName.hashCode() + event.directoryUri.hashCode()
                    )
                    repository.upsertFolder(folder)
                    _state.update {
                        it.copy(
                            isEmpty = false,
                            selectionState = SelectionState(),
                            isLoading = false
                        )
                    }
                }
            }

            is AlbumViewEvent.ChangeAlbumName -> {
                viewModelScope.launch {
                    if (!_state.value.albums.any { it.album.displayedAlbumName == event.displayName }) {
                        val album = _state.value.albums.find { it.album.initialAlbumName == _state.value.initialAlbumName }
                        if (album == null) { return@launch }
                        repository.updateAlbum(
                            album.album.copy(displayedAlbumName = event.displayName)
                        )
                    }
                }
            }

            is AlbumViewEvent.SelectWallpaper -> {
                viewModelScope.launch {
                    if (!_state.value.selectionState.selectedWallpapers.contains(event.wallpaperUri)) {
                        val album = _state.value.albums.find { it.album.initialAlbumName == _state.value.initialAlbumName }
                        if (album == null) { return@launch }
                        _state.update {
                            val newSelectedWallpapers = it.selectionState.selectedWallpapers.plus(event.wallpaperUri)
                            it.copy(
                                selectionState = it.selectionState.copy(
                                    selectedWallpapers = newSelectedWallpapers,
                                    selectedCount = newSelectedWallpapers.size + it.selectionState.selectedFolders.size,
                                    allSelected = it.selectionState.selectedFolders.size + newSelectedWallpapers.size >=
                                            album.folders.size + album.wallpapers.size
                                )
                            )
                        }
                    }
                }
            }

            is AlbumViewEvent.SelectFolder -> {
                viewModelScope.launch {
                    if (!_state.value.selectionState.selectedFolders.contains(event.directoryUri)) {
                        val album = _state.value.albums.find { it.album.initialAlbumName == _state.value.initialAlbumName }
                        if (album == null) { return@launch }
                        _state.update {
                            val newSelectedFolders = it.selectionState.selectedFolders.plus(event.directoryUri)
                            it.copy(
                                selectionState = it.selectionState.copy(
                                    selectedFolders = newSelectedFolders,
                                    selectedCount = newSelectedFolders.size + it.selectionState.selectedWallpapers.size,
                                    allSelected = newSelectedFolders.size + it.selectionState.selectedWallpapers.size >=
                                            album.folders.size + album.wallpapers.size
                                )
                            )
                        }
                    }
                }
            }

            is AlbumViewEvent.DeselectFolder -> {
                viewModelScope.launch {
                    if (_state.value.selectionState.selectedFolders.contains(event.directoryUri)) {
                        _state.update {
                            it.copy(
                                selectionState = it.selectionState.copy(
                                    selectedFolders = it.selectionState.selectedFolders.minus(event.directoryUri),
                                    selectedCount = it.selectionState.selectedCount - 1,
                                    allSelected = false
                                )
                            )
                        }
                    }
                }
            }

            is AlbumViewEvent.DeselectWallpaper -> {
                viewModelScope.launch {
                    if (_state.value.selectionState.selectedWallpapers.contains(event.wallpaperUri)) {
                        _state.update {
                            it.copy(
                                selectionState = it.selectionState.copy(
                                    selectedWallpapers = it.selectionState.selectedWallpapers.minus(event.wallpaperUri),
                                    selectedCount = it.selectionState.selectedCount - 1,
                                    allSelected = false
                                )
                            )
                        }
                    }
                }
            }

            is AlbumViewEvent.DeleteSelected -> {
                viewModelScope.launch {
                    val album = _state.value.albums.find { it.album.initialAlbumName == _state.value.initialAlbumName }
                    if (album == null) { return@launch }
                    val selectedFolders = _state.value.selectionState.selectedFolders
                    val selectedWallpapers = _state.value.selectionState.selectedWallpapers
                    val foldersToDelete = album.folders.filter { selectedFolders.contains(it.folderUri) }
                    val wallpapersToDelete = album.wallpapers.filter { selectedWallpapers.contains(it.wallpaperUri) }
                    if (foldersToDelete.size + wallpapersToDelete.size == album.folders.size + album.wallpapers.size) {
                        repository.cascadeDeleteAlbum(album.album)
                    }
                    else {
                        repository.deleteFolderList(foldersToDelete)
                        repository.deleteWallpaperList(wallpapersToDelete)
                        if (album.album.coverUri in (foldersToDelete.map { it.folderUri } + wallpapersToDelete.map { it.wallpaperUri })) {
                            val newCover = album.folders.minus(foldersToDelete.toSet()).firstOrNull()?.coverUri ?: album.wallpapers.minus(wallpapersToDelete.toSet()
                            ).firstOrNull()?.wallpaperUri ?: ""
                            repository.updateAlbum(album.album.copy(coverUri = newCover))
                        }

                        val remainingWallpapers = album.wallpapers.minus(wallpapersToDelete.toSet())
                        val newWallpapers = remainingWallpapers.sortedBy { it.order }.mapIndexed { index, wallpaper ->
                            wallpaper.copy(order = index)
                        }

                        val remainingFolders = album.folders.minus(foldersToDelete.toSet())
                        var totalWallpaper = newWallpapers.size
                        val newFolders = remainingFolders.sortedBy { it.order }.mapIndexed { folderIndex, folder ->
                            val updatedWallpapers = folder.wallpapers.mapIndexed { index, wallpaper ->
                                wallpaper.copy(order = index + totalWallpaper)
                            }
                            totalWallpaper += folder.wallpapers.size
                            folder.copy(
                                wallpapers = updatedWallpapers,
                                order = folderIndex
                            )
                        }

                        repository.upsertAlbumWithWallpaperAndFolder(
                            album.copy(
                                folders = newFolders,
                                wallpapers = newWallpapers
                            )
                        )
                    }
                    _state.update {
                        it.copy(
                            selectionState = SelectionState(),
                            isLoading = false
                        )
                    }
                }
            }

            is AlbumViewEvent.Reset -> {
                viewModelScope.launch {
                    _state.update { AlbumViewState() }
                }
            }

            is AlbumViewEvent.SelectAll -> {
                viewModelScope.launch {
                    if (!_state.value.selectionState.allSelected) {
                        val album = _state.value.albums.find { it.album.initialAlbumName == _state.value.initialAlbumName }
                        if (album == null) { return@launch }
                        _state.update {
                            it.copy(
                                selectionState = SelectionState(
                                    selectedFolders = album.folders.map { it.folderUri },
                                    selectedWallpapers = album.wallpapers.map { it.wallpaperUri },
                                    allSelected = true,
                                    selectedCount = album.folders.size + album.wallpapers.size
                                )
                            )
                        }
                    }
                }
            }

            is AlbumViewEvent.DeselectAll -> {
                viewModelScope.launch {
                    _state.update { it.copy(selectionState = SelectionState()) }
                }
            }

            is AlbumViewEvent.SetLoading -> {
                viewModelScope.launch {
                    _state.update { it.copy(isLoading = event.isLoading) }
                }
            }

            is AlbumViewEvent.LoadFoldersAndWallpapers -> {
                viewModelScope.launch {
                    _state.update { state ->
                        val album = _state.value.albums.find { it.album.initialAlbumName == _state.value.initialAlbumName }
                        if (album == null) { return@launch }
                        repository.upsertAlbumWithWallpaperAndFolder(
                            AlbumWithWallpaperAndFolder(
                                album = album.album.copy(
                                    coverUri = event.folders.firstOrNull()?.coverUri ?: event.wallpapers.firstOrNull()?.wallpaperUri ?: ""
                                ),
                                folders = event.folders.map {
                                    it.copy(coverUri = it.wallpapers.firstOrNull()?.wallpaperUri ?: "")
                                },
                                wallpapers = event.wallpapers
                            )
                        )
                        state.copy(
                            selectionState = SelectionState(),
                            isLoading = false
                        )
                    }
                }
            }

            is AlbumViewEvent.DeleteAlbum -> {
                viewModelScope.launch {
                    val album = _state.value.albums.find { it.album.initialAlbumName == _state.value.initialAlbumName }
                    if (album == null) { return@launch }
                    repository.cascadeDeleteAlbum(album.album)
                    _state.update { AlbumViewState() }
                }
            }
        }
    }

}