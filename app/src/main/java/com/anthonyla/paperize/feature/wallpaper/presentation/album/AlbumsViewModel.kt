package com.anthonyla.paperize.feature.wallpaper.presentation.album

import android.app.Application
import android.content.Context
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import com.lazygeniouz.dfc.file.DocumentFileCompat
import dagger.hilt.android.lifecycle.HiltViewModel
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
    var shouldNotBypassSplashScreen by mutableStateOf(true)
    private val context: Context
        get() = getApplication<Application>().applicationContext
    private val _state = MutableStateFlow(AlbumsState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000), AlbumsState()
    )

    init {
        viewModelScope.launch {
            updateAlbums()
            refreshAlbums()
            shouldNotBypassSplashScreen = false
        }
    }

    fun onEvent(event: AlbumsEvent) {
        when (event) {
            is AlbumsEvent.DeleteAlbumWithWallpapers -> {
                viewModelScope.launch {
                    repository.cascadeDeleteAlbum(event.albumWithWallpaper.album)
                    refreshAlbums()
                }
            }
            is AlbumsEvent.ChangeAlbumName -> {
                viewModelScope.launch {
                    if (!(_state.value.albumWithWallpapers.any { it.album.displayedAlbumName == event.title })) {
                        repository.updateAlbum(
                            event.albumWithWallpaper.album.copy(displayedAlbumName = event.title)
                        )
                    }
                }
            }
            is AlbumsEvent.RefreshAlbums -> {
                viewModelScope.launch {
                    updateAlbums()
                    refreshAlbums()
                }
            }
            is AlbumsEvent.RefreshAlbumCoverUri -> {
                viewModelScope.launch {
                    event.albumWithWallpaper.let { album ->
                        if (!album.wallpapers.any { it.wallpaperUri == album.album.coverUri }) {
                            repository.updateAlbum(album.album.copy(
                                coverUri = null
                            ))
                        }
                    }
                    updateAlbums()
                    refreshAlbums()
                }
            }
        }
    }

    // Verify and remove stale data in Room database
    private fun updateAlbums() {
        viewModelScope.launch {
            val albumWithWallpapers = repository.getAlbumsWithWallpapers().first()
            albumWithWallpapers.forEach { albumWithWallpaper ->
                if (albumWithWallpaper.album.coverUri != null) {
                    val albumCoverFile = DocumentFile.fromSingleUri(context, albumWithWallpaper.album.coverUri.toUri())
                    if (albumCoverFile == null || !albumCoverFile.exists()) {
                        repository.updateAlbum(albumWithWallpaper.album.copy(coverUri = null))
                    }
                }
                // Delete wallpaper if the URI is invalid
                albumWithWallpaper.wallpapers.forEach { wallpaper ->
                    val file = DocumentFile.fromSingleUri(context, wallpaper.wallpaperUri.toUri())
                    if (file == null || !file.exists()) { repository.deleteWallpaper(wallpaper) }
                }
                // Update folder cover uri and children uri
                albumWithWallpaper.folders.forEach { folder ->
                    val folderDirectory = DocumentFile.fromTreeUri(context, folder.folderUri.toUri())
                    if (folderDirectory == null || !folderDirectory.isDirectory) {
                        repository.deleteFolder(folder)
                    } else {
                        val wallpapers = getWallpaperFromFolder(folder.folderUri, context)
                        val folderCover: String?
                        if (folder.coverUri != null) {
                            val folderCoverFile = DocumentFile.fromSingleUri(context, folder.coverUri.toUri())
                            if (folderCoverFile == null || !folderCoverFile.exists()) {
                                folderCover = folder.wallpapers.randomOrNull()
                            } else { folderCover = folder.coverUri }
                        } else {
                            folderCover = wallpapers.randomOrNull()
                        }
                        repository.updateFolder(folder.copy(coverUri = folderCover, wallpapers = wallpapers))
                    }
                }
            }
        }
    }

    // Retrieve albums from database into viewModel
    private fun refreshAlbums() {
        viewModelScope.launch {
            repository.getAlbumsWithWallpapers().collect { albumWithWallpapers ->
                _state.update { it.copy(
                    albumWithWallpapers = albumWithWallpapers
                ) }
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
                val extension = MimeTypeMap.getFileExtensionFromUrl(file.uri.toString())
                val allowedExtensions = listOf("jpg", "png", "heif", "webp")
                if (extension in allowedExtensions) {
                    files.add(file.uri.toString())
                }
            }
        }
        return files
    }
}

