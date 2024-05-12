package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen


import android.app.Application
import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
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
                    val albumCoverUri = when (_state.value.isEmpty) {
                        true -> ""
                        false -> {
                            if (_state.value.wallpapers.isNotEmpty())
                                _state.value.wallpapers.random().wallpaperUri
                            else {
                                _state.value.folders.random().coverUri
                            }
                        }
                    }
                    val albumWithWallpaperAndFolder = AlbumWithWallpaperAndFolder(
                        album = Album(
                            initialAlbumName = _state.value.initialAlbumName,
                            displayedAlbumName = _state.value.displayedAlbumName,
                            coverUri = albumCoverUri,
                        ),
                        wallpapers = _state.value.wallpapers,
                        folders = _state.value.folders,
                    )
                    repository.upsertAlbumWithWallpaperAndFolder(albumWithWallpaperAndFolder)
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
            is AddAlbumEvent.DeleteSelected -> {
                viewModelScope.launch {
                    _state.update {
                        val wallpapersRemoved = it.wallpapers.filterNot { it.wallpaperUri in _state.value.selectedWallpapers }
                        val foldersRemoved = it.folders.filterNot { it.folderUri in _state.value.selectedFolders }
                        it.copy(
                            wallpapers = wallpapersRemoved,
                            folders = foldersRemoved,
                            isEmpty = wallpapersRemoved.isEmpty() && foldersRemoved.isEmpty()
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
                if (event.newAlbumName != _state.value.initialAlbumName) {
                    viewModelScope.launch {
                        _state.update {
                            val wallpapers = it.wallpapers.map { wallpaper ->
                                wallpaper.copy(initialAlbumName = event.newAlbumName)
                            }
                            val folders = it.folders.map { folder ->
                                folder.copy(initialAlbumName = event.newAlbumName)
                            }
                            it.copy(
                                initialAlbumName = event.newAlbumName,
                                displayedAlbumName = event.newAlbumName,
                                coverUri = it.coverUri,
                                wallpapers = wallpapers,
                                folders = folders,
                                isEmpty = wallpapers.isEmpty() && folders.isEmpty()
                            ) }
                    }
                }
            }
            is AddAlbumEvent.AddWallpapers -> {
                viewModelScope.launch {
                    val newWallpaperUris = event.wallpaperUris.filterNot { it in _state.value.wallpapers.map { wallpaper -> wallpaper.wallpaperUri } }
                    val newWallpapers = newWallpaperUris.map { uri ->
                        Wallpaper(
                            initialAlbumName = _state.value.initialAlbumName,
                            wallpaperUri = uri,
                            key = uri.hashCode() + _state.value.initialAlbumName.hashCode(),
                        )
                    }
                    _state.update {
                        it.copy(
                            selectedFolders = emptyList(),
                            selectedWallpapers = emptyList(),
                            allSelected = false,
                            selectedCount = 0,
                            wallpapers = it.wallpapers.plus(newWallpapers),
                            isEmpty = false
                        )
                    }
                }
            }
            is AddAlbumEvent.AddFolder -> {
                viewModelScope.launch {
                    if (event.directoryUri !in _state.value.folders.map { it.folderUri }) {
                        val wallpapers: List<String> = getWallpaperFromFolder(event.directoryUri, context)
                        val folderName = getFolderNameFromUri(event.directoryUri, context)
                        _state.update {
                            it.copy(
                                selectedFolders = emptyList(),
                                selectedWallpapers = emptyList(),
                                allSelected = false,
                                selectedCount = 0,
                                isEmpty = false,
                                folders = it.folders.plus(
                                    Folder(
                                        initialAlbumName = it.initialAlbumName,
                                        folderName = folderName,
                                        folderUri = event.directoryUri,
                                        coverUri = wallpapers.randomOrNull(),
                                        wallpapers = wallpapers,
                                        key = event.directoryUri.hashCode() + it.initialAlbumName.hashCode()
                                    )
                                )
                            )
                        }
                    }
                }
            }
            is AddAlbumEvent.SelectAll -> {
                viewModelScope.launch {
                    if (!_state.value.allSelected) {
                        _state.update { it.copy(
                            selectedFolders = _state.value.folders.map { folder -> folder.folderUri },
                            selectedWallpapers = _state.value.wallpapers.map { wallpaper -> wallpaper.wallpaperUri },
                            selectedCount = it.folders.size + it.wallpapers.size,
                            allSelected = true
                        ) }
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
                    if (!_state.value.selectedFolders.any { it == event.directoryUri }) {
                        _state.update {
                            val folders = it.selectedFolders.plus(event.directoryUri)
                            it.copy(
                                selectedFolders = folders,
                                selectedCount = it.selectedCount + 1,
                                allSelected = folders.size + it.selectedWallpapers.size >= it.wallpapers.size + it.folders.size
                            ) }
                    }
                }
            }
            is AddAlbumEvent.SelectWallpaper -> {
                viewModelScope.launch {
                    if (!_state.value.selectedWallpapers.any { it == event.wallpaperUri }) {
                        _state.update {
                            val wallpapers = it.selectedWallpapers.plus(event.wallpaperUri)
                            it.copy(
                                selectedWallpapers = wallpapers,
                                selectedCount = it.selectedCount + 1,
                                allSelected = it.selectedFolders.size + wallpapers.size >= it.wallpapers.size + it.folders.size
                            ) }
                    }
                }
            }
            is AddAlbumEvent.RemoveFolderFromSelection -> {
                viewModelScope.launch {
                    if (_state.value.selectedFolders.find { it == event.directoryUri } != null ) {
                        _state.update {
                            val folders = it.selectedFolders.minus(event.directoryUri)
                            it.copy(
                                selectedFolders = folders,
                                selectedCount = it.selectedCount - 1,
                                allSelected = folders.size + it.selectedWallpapers.size >= it.wallpapers.size + it.folders.size

                            ) }
                    }
                }
            }
            is AddAlbumEvent.RemoveWallpaperFromSelection -> {
                viewModelScope.launch {
                    if (_state.value.selectedWallpapers.find { it == event.wallpaperUri } != null ) {
                        _state.update {
                            val wallpapers = it.selectedWallpapers.minus(event.wallpaperUri)
                            it.copy(
                                selectedWallpapers = wallpapers,
                                selectedCount = it.selectedCount - 1,
                                allSelected = it.selectedFolders.size + wallpapers.size >= it.wallpapers.size + it.folders.size
                            ) }
                    }
                }
            }
            is AddAlbumEvent.Reset -> {
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
                val allowedExtensions = listOf("jpg", "jpeg", "png", "heif", "webp", "JPG", "JPEG", "PNG", "HEIF", "WEBP")
                if (file.extension in allowedExtensions) {
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