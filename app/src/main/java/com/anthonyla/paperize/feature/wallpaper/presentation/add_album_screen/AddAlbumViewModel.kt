package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen


import android.app.Application
import android.content.Context
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.use_case.AlbumsUseCases
import com.lazygeniouz.dfc.file.DocumentFileCompat
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
                    val albumWithWallpaper = AlbumWithWallpaper(
                        album = Album(
                            state.value.initialAlbumName,
                            state.value.displayedAlbumName,
                            state.value.coverUri,
                            state.value.folders
                        ),
                        wallpapers = state.value.wallpapers
                    )
                    albumsUseCases.addAlbumWithWallpaper(albumWithWallpaper)

                    //Clear viewModel state after adding album
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
            is AddAlbumEvent.SetAlbumName -> {
                viewModelScope.launch {
                    _state.update { it.copy(
                        initialAlbumName = event.initialAlbumName,
                        displayedAlbumName = event.initialAlbumName,
                    ) }
                }
            }

            is AddAlbumEvent.ReflectAlbumName -> {
                if (event.newAlbumName != state.value.initialAlbumName) {
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
                        val wallpapers: List<String> = getWallpaperFromFolder(event.directoryUri, context)
                        val folderName = getFolderNameFromUri(event.directoryUri, context)
                        if (wallpapers.isNotEmpty()) {
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

    private fun getWallpaperFromFolder(folderUri: String, context: Context): List<String> {
        val folderDocumentFile = DocumentFileCompat.fromTreeUri(context, folderUri.toUri())
        return listFilesRecursive(folderDocumentFile, context)
    }

    private fun listFilesRecursive(parent: DocumentFileCompat?, context: Context): List<String> {
        val files = mutableListOf<String>()
        parent?.listFiles()?.forEach { file ->
            if (file.isDirectory()) {
                files.addAll(listFilesRecursive(file, context))
            } else {
                val extension = MimeTypeMap.getFileExtensionFromUrl(file.uri.toString())
                val allowedExtensions = listOf("jpg", "png", "heif", "webp")
                if (extension in allowedExtensions) {
                    files.add(file.uri.toString())
                }
            }
        }
        return files
    }

    private fun getFolderNameFromUri(folderUri: String, context: Context): String? {
        return DocumentFileCompat.fromTreeUri(context, folderUri.toUri())?.name
    }
}