package com.anthonyla.paperize.presentation.screens.album_view

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.FolderRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val folderRepository: FolderRepository,
    private val wallpaperRepository: WallpaperRepository
) : ViewModel() {

    private val albumId: String = savedStateHandle.get<String>("albumId") ?: ""

    val album: StateFlow<Album?> = albumRepository.getAlbumById(albumId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val folders: StateFlow<List<Folder>> = folderRepository.getFoldersByAlbumId(albumId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val wallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapersByAlbumId(albumId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addWallpapers(uris: List<String>) {
        viewModelScope.launch {
            uris.forEach { uri ->
                wallpaperRepository.createWallpaper(
                    Wallpaper(
                        id = "",
                        albumId = albumId,
                        folderId = null,
                        uri = uri,
                        name = uri.substringAfterLast('/'),
                        dateAdded = System.currentTimeMillis(),
                        order = 0
                    )
                )
            }
        }
    }

    fun addFolder(uri: String) {
        viewModelScope.launch {
            folderRepository.createFolder(
                Folder(
                    id = "",
                    albumId = albumId,
                    uri = uri,
                    name = uri.substringAfterLast('/'),
                    dateAdded = System.currentTimeMillis(),
                    order = 0,
                    wallpapers = emptyList()
                )
            )
        }
    }

    fun deleteAlbum() {
        viewModelScope.launch {
            albumRepository.deleteAlbum(albumId)
        }
    }
}
