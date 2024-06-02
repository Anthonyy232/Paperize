package com.anthonyla.paperize.feature.wallpaper.presentation.album

import android.app.Application
import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.findFirstValidUri
import com.anthonyla.paperize.core.getWallpaperFromFolder
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import com.lazygeniouz.dfc.file.DocumentFileCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor (
    application: Application,
    private val repository: AlbumRepository
) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication<Application>().applicationContext
    private val _state = MutableStateFlow(AlbumsState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000), AlbumsState()
    )

    init {
        updateAlbums()
        refreshAlbums()
    }


    fun onEvent(event: AlbumsEvent) {
        when (event) {
            is AlbumsEvent.DeleteAlbumWithWallpapers -> {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.cascadeDeleteAlbum(event.albumWithWallpaperAndFolder.album)
                }
            }
            is AlbumsEvent.ChangeAlbumName -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (!(_state.value.albumsWithWallpapers.any { it.album.displayedAlbumName == event.title })) {
                        repository.updateAlbum(
                            event.albumWithWallpaperAndFolder.album.copy(displayedAlbumName = event.title)
                        )
                    }
                }
            }
            is AlbumsEvent.RefreshAlbums -> {
                updateAlbums()
                refreshAlbums()
            }
            is AlbumsEvent.InitializeAlbum -> {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updateAlbum(
                        event.albumWithWallpaperAndFolder.album.copy(
                            initialized = true
                        )
                    )
                }
            }
            is AlbumsEvent.Reset -> {
                viewModelScope.launch(Dispatchers.IO) {
                    _state.update { AlbumsState() }
                    repository.deleteAllData()
                }
            }
        }
    }

    /**
     * Verify and remove stale data in Room database
     */
    private fun updateAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            var albumWithWallpapers = repository.getAlbumsWithWallpaperAndFolder().first()
            albumWithWallpapers.forEach { albumWithWallpaper ->
                // Delete wallpaper if the URI is invalid
                val invalidWallpapers = albumWithWallpaper.wallpapers.filterNot { wallpaper ->
                    val file = DocumentFileCompat.fromSingleUri(context, wallpaper.wallpaperUri.toUri())
                    file?.exists() == true
                }
                if (invalidWallpapers.isNotEmpty()) {
                    repository.deleteWallpaperList(invalidWallpapers)
                }

                // Update folder cover uri and wallpapers uri
                albumWithWallpaper.folders.forEach { folder ->
                    DocumentFileCompat.fromTreeUri(context, folder.folderUri.toUri())?.let { folderDirectory ->
                        if (!folderDirectory.isDirectory()) {
                            repository.deleteFolder(folder)
                        } else {
                            val wallpapers = getWallpaperFromFolder(folder.folderUri, context)
                            val folderCoverFile = folder.coverUri?.let { DocumentFile.fromSingleUri(context, it.toUri()) }
                            val folderCover = folderCoverFile?.takeIf { it.exists() }?.uri?.toString() ?: wallpapers.randomOrNull()
                            repository.updateFolder(folder.copy(coverUri = folderCover, wallpapers = wallpapers))
                        }
                    }
                }
            }
            albumWithWallpapers = repository.getAlbumsWithWallpaperAndFolder().first()
            albumWithWallpapers.forEach { albumWithWallpaper ->
                // Delete empty albums
                if (albumWithWallpaper.wallpapers.isEmpty() && albumWithWallpaper.folders.all { it.wallpapers.isEmpty() }) {
                    repository.deleteAlbum(albumWithWallpaper.album)
                }
                else {
                    // Update album cover uri if null or invalid
                    val albumCoverFile = albumWithWallpaper.album.coverUri?.toUri()?.let { DocumentFile.fromSingleUri(context, it) }
                    if (albumCoverFile == null || !albumCoverFile.exists()) {
                        val newCoverUri = findFirstValidUri(context, albumWithWallpaper.wallpapers, albumWithWallpaper.folders)
                        repository.updateAlbum(albumWithWallpaper.album.copy(coverUri = newCoverUri))
                    }
                }
            }
        }
    }

    /**
     * Retrieve albums from database into viewModel
     */
    private fun refreshAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAlbumsWithWallpaperAndFolder().collect { albumWithWallpapers ->
                _state.update { it.copy(
                    albumsWithWallpapers = albumWithWallpapers
                ) }
            }
        }
    }
}

