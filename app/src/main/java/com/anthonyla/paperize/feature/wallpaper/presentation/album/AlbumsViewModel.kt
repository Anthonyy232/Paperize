package com.anthonyla.paperize.feature.wallpaper.presentation.album

import android.app.Application
import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.findFirstValidUri
import com.anthonyla.paperize.core.getWallpaperFromFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import com.lazygeniouz.dfc.file.DocumentFileCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor (
    application: Application,
    private val repository: AlbumRepository
) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication<Application>().applicationContext
    
    private val _state = MutableStateFlow<AlbumsState>(AlbumsState())
    val state = combine(
        loadAlbumsFlow(),
        _state
    ) { albums: List<AlbumWithWallpaperAndFolder>, currentState: AlbumsState ->
        currentState.copy(
            albumsWithWallpapers = albums
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumsState()
    )

    private fun loadAlbumsFlow() = repository.getAlbumsWithWallpaperAndFolder()

    init {
        updateAlbums()
    }

    fun onEvent(event: AlbumsEvent) {
        when (event) {
            is AlbumsEvent.DeleteAlbumWithWallpapers -> {
                viewModelScope.launch {
                    repository.cascadeDeleteAlbum(event.albumWithWallpaperAndFolder.album)
                }
            }
            
            is AlbumsEvent.ChangeAlbumName -> {
                viewModelScope.launch {
                    if (!(_state.value.albumsWithWallpapers.any { it.album.displayedAlbumName == event.title })) {
                        repository.updateAlbum(
                            event.albumWithWallpaperAndFolder.album.copy(displayedAlbumName = event.title)
                        )
                    }
                }
            }

            is AlbumsEvent.InitializeAlbum -> {
                viewModelScope.launch {
                    repository.updateAlbum(
                        event.albumWithWallpaperAndFolder.album.copy(initialized = true)
                    )
                }
            }

            is AlbumsEvent.RefreshAlbums -> {
                updateAlbums()
            }

            is AlbumsEvent.Reset -> {
                viewModelScope.launch {
                    repository.deleteAllData()
                }
            }
        }
    }

    private fun updateAlbums() {
        viewModelScope.launch {
            var albumWithWallpapers = repository.getAlbumsWithWallpaperAndFolder().first()
            albumWithWallpapers.forEach { albumWithWallpaper ->
                // Delete invalid wallpapers
                val invalidWallpapers = albumWithWallpaper.wallpapers.filterNot { wallpaper ->
                    try {
                        val file = DocumentFileCompat.fromSingleUri(context, wallpaper.wallpaperUri.toUri())
                        file?.exists() == true
                    } catch (e: Exception) {
                        val file = DocumentFile.fromSingleUri(context, wallpaper.wallpaperUri.toUri())
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
                                    val wallpapers = getWallpaperFromFolder(folder.folderUri, context)
                                    val folderCoverFile = folder.coverUri?.let {
                                        DocumentFileCompat.fromSingleUri(context, it.toUri())
                                    }
                                    val folderCover =
                                        folderCoverFile?.takeIf { it.exists() }?.uri?.toString()
                                            ?: (wallpapers.randomOrNull()?.wallpaperUri ?: "")
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
                                    val wallpapers = getWallpaperFromFolder(folder.folderUri, context)
                                    try {
                                        val folderCoverFile = folder.coverUri?.let {
                                            DocumentFileCompat.fromSingleUri(context, it.toUri())
                                        }
                                        val folderCover =
                                            folderCoverFile?.takeIf { it.exists() }?.uri?.toString()
                                                ?: (wallpapers.randomOrNull()?.wallpaperUri ?: "")
                                        repository.updateFolder(
                                            folder.copy(
                                                coverUri = folderCover,
                                                wallpapers = wallpapers
                                            )
                                        )
                                    } catch (_: Exception) {
                                        val folderCoverFile = folder.coverUri?.let {
                                            DocumentFile.fromSingleUri(context, it.toUri())
                                        }
                                        val folderCover =
                                            folderCoverFile?.takeIf { it.exists() }?.uri?.toString()
                                                ?: (wallpapers.randomOrNull()?.wallpaperUri ?: "")
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
                // Delete empty albums
                albumWithWallpapers = repository.getAlbumsWithWallpaperAndFolder().first()
                albumWithWallpapers.forEach { album ->
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
}

