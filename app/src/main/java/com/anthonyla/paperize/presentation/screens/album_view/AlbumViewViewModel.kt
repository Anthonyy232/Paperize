package com.anthonyla.paperize.presentation.screens.album_view

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val wallpaperRepository: WallpaperRepository
) : ViewModel() {

    private val albumId: String = savedStateHandle.get<String>("albumId") ?: ""

    val album: StateFlow<Album?> = albumRepository.getAlbumById(albumId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val folders: StateFlow<List<Folder>> = album
        .map { it?.folders ?: emptyList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val wallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapersByAlbum(albumId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addWallpapers(uris: List<String>) {
        viewModelScope.launch {
            val wallpapers = uris.mapIndexed { index, uri ->
                Wallpaper(
                    id = "",
                    albumId = albumId,
                    folderId = null,
                    uri = uri,
                    fileName = uri.substringAfterLast('/'),
                    dateModified = System.currentTimeMillis(),
                    displayOrder = index
                )
            }
            albumRepository.addWallpapersToAlbum(albumId, wallpapers)
        }
    }

    fun addFolder(uri: String) {
        viewModelScope.launch {
            val folder = Folder(
                id = "",
                albumId = albumId,
                uri = uri,
                name = uri.substringAfterLast('/'),
                coverUri = null,
                dateModified = System.currentTimeMillis(),
                displayOrder = 0,
                wallpapers = emptyList()
            )
            albumRepository.addFolderToAlbum(albumId, folder)
        }
    }

    fun deleteAlbum() {
        viewModelScope.launch {
            albumRepository.deleteAlbum(albumId)
        }
    }
}
