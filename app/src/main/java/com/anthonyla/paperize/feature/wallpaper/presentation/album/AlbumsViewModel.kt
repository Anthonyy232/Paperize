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
                    _state.update {
                        it.copy(
                            albumsWithWallpapers = it.albumsWithWallpapers.filterNot { albumWithWallpaper ->
                                albumWithWallpaper.album == event.albumWithWallpaperAndFolder.album
                            }
                        )
                    }
                }
            }
            is AlbumsEvent.ChangeAlbumName -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (!(_state.value.albumsWithWallpapers.any { it.album.displayedAlbumName == event.title })) {
                        repository.updateAlbum(
                            event.albumWithWallpaperAndFolder.album.copy(displayedAlbumName = event.title)
                        )
                        _state.update {
                            it.copy(
                                albumsWithWallpapers = it.albumsWithWallpapers.map { albumWithWallpaper ->
                                    if (albumWithWallpaper.album == event.albumWithWallpaperAndFolder.album) {
                                        albumWithWallpaper.copy(
                                            album = albumWithWallpaper.album.copy(displayedAlbumName = event.title)
                                        )
                                    } else {
                                        albumWithWallpaper
                                    }
                                }
                            )
                        }
                    }
                }
            }
            is AlbumsEvent.InitializeAlbum -> {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updateAlbum(
                        event.albumWithWallpaperAndFolder.album.copy(
                            initialized = true
                        )
                    )
                    _state.update {
                        it.copy(
                            albumsWithWallpapers = it.albumsWithWallpapers.map { albumWithWallpaper ->
                                if (albumWithWallpaper.album == event.albumWithWallpaperAndFolder.album) {
                                    albumWithWallpaper.copy(
                                        album = albumWithWallpaper.album.copy(
                                            initialized = true
                                        )
                                    )
                                } else {
                                    albumWithWallpaper
                                }
                            }
                        )
                    }
                }
            }
            is AlbumsEvent.RefreshAlbums -> {
                updateAlbums()
                refreshAlbums()
            }
            is AlbumsEvent.Reset -> {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.deleteAllData()
                    _state.update { AlbumsState() }
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
                    try {
                        val file = DocumentFileCompat.fromSingleUri(
                            context,
                            wallpaper.wallpaperUri.toUri()
                        )
                        file?.exists() == true
                    } catch (e: Exception) {
                        val file =
                            DocumentFile.fromSingleUri(context, wallpaper.wallpaperUri.toUri())
                        file?.exists() == true
                    }
                }
                if (invalidWallpapers.isNotEmpty()) {
                    repository.deleteWallpaperList(invalidWallpapers)
                }

                // Update folder cover uri and wallpapers uri
                albumWithWallpaper.folders.forEach { folder ->
                    try {
                        DocumentFileCompat.fromTreeUri(context, folder.folderUri.toUri())
                            ?.let { folderDirectory ->
                                if (!folderDirectory.isDirectory()) {
                                    repository.deleteFolder(folder)
                                } else {
                                    val wallpapers =
                                        getWallpaperFromFolder(folder.folderUri, context)
                                    val folderCoverFile = folder.coverUri?.let {
                                        DocumentFileCompat.fromSingleUri(
                                            context,
                                            it.toUri()
                                        )
                                    }
                                    val folderCover =
                                        folderCoverFile?.takeIf { it.exists() }?.uri?.toString()
                                            ?: wallpapers.randomOrNull()
                                    repository.updateFolder(
                                        folder.copy(
                                            coverUri = folderCover,
                                            wallpapers = wallpapers
                                        )
                                    )
                                }
                            }
                    } catch (e: Exception) {
                        DocumentFile.fromTreeUri(context, folder.folderUri.toUri())
                            ?.let { folderDirectory ->
                                if (!folderDirectory.isDirectory) {
                                    repository.deleteFolder(folder)
                                } else {
                                    val wallpapers =
                                        getWallpaperFromFolder(folder.folderUri, context)
                                    try {
                                        val folderCoverFile = folder.coverUri?.let {
                                            DocumentFileCompat.fromSingleUri(
                                                context,
                                                it.toUri()
                                            )
                                        }
                                        val folderCover =
                                            folderCoverFile?.takeIf { it.exists() }?.uri?.toString()
                                                ?: wallpapers.randomOrNull()
                                        repository.updateFolder(
                                            folder.copy(
                                                coverUri = folderCover,
                                                wallpapers = wallpapers
                                            )
                                        )
                                    } catch (e: Exception) {
                                        val folderCoverFile = folder.coverUri?.let {
                                            DocumentFile.fromSingleUri(
                                                context,
                                                it.toUri()
                                            )
                                        }
                                        val folderCover =
                                            folderCoverFile?.takeIf { it.exists() }?.uri?.toString()
                                                ?: wallpapers.randomOrNull()
                                        repository.updateFolder(
                                            folder.copy(
                                                coverUri = folderCover,
                                                wallpapers = wallpapers
                                            )
                                        )
                                    }
                                }
                            }
                    }
                }
                albumWithWallpapers = repository.getAlbumsWithWallpaperAndFolder().first()
                albumWithWallpapers.forEach { album ->
                    // Delete empty albums
                    if (album.wallpapers.isEmpty() && album.folders.all { it.wallpapers.isEmpty() }) {
                        repository.deleteAlbum(album.album)
                    } else {
                        // Update album cover uri if null or invalid
                        try {
                            val albumCoverFile = album.album.coverUri?.toUri()
                                ?.let { DocumentFileCompat.fromSingleUri(context, it) }
                            if (albumCoverFile == null || !albumCoverFile.exists()) {
                                val newCoverUri =
                                    findFirstValidUri(context, album.wallpapers, album.folders)
                                repository.updateAlbum(album.album.copy(coverUri = newCoverUri))
                            }
                        } catch (e: Exception) {
                            val albumCoverFile = album.album.coverUri?.toUri()
                                ?.let { DocumentFile.fromSingleUri(context, it) }
                            if (albumCoverFile == null || !albumCoverFile.exists()) {
                                val newCoverUri =
                                    findFirstValidUri(context, album.wallpapers, album.folders)
                                repository.updateAlbum(album.album.copy(coverUri = newCoverUri))
                            }
                        }
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

