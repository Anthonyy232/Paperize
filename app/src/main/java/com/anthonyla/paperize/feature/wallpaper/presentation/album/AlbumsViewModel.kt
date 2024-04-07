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
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
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
            is AlbumsEvent.RefreshAlbums -> {
                viewModelScope.launch {
                    updateAlbums()
                    refreshAlbums()
                }
            }
        }
    }

    /**
     * Verify and remove stale data in Room database
     */
    private fun updateAlbums() {
        viewModelScope.launch {
            var albumWithWallpapers = repository.getAlbumsWithWallpaperAndFolder().first()
            albumWithWallpapers.forEach { albumWithWallpaper ->
                // Delete wallpaper if the URI is invalid
                val invalidWallpapers = albumWithWallpaper.wallpapers.filterNot { wallpaper ->
                    val file = DocumentFile.fromSingleUri(context, wallpaper.wallpaperUri.toUri())
                    file?.exists() == true
                }
                if (invalidWallpapers.isNotEmpty()) {
                    repository.deleteWallpaperList(invalidWallpapers)
                }

                // Update folder cover uri and children uri
                albumWithWallpaper.folders.forEach { folder ->
                    DocumentFile.fromTreeUri(context, folder.folderUri.toUri())?.let { folderDirectory ->
                        if (!folderDirectory.isDirectory) {
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
                // Update album cover uri if null or invalid
                val albumCoverFile = albumWithWallpaper.album.coverUri?.toUri()?.let { DocumentFile.fromSingleUri(context, it) }
                if (albumCoverFile == null || !albumCoverFile.exists()) {
                    val newCoverUri = findFirstValidUri(albumWithWallpaper.wallpapers, albumWithWallpaper.folders)
                    repository.updateAlbum(albumWithWallpaper.album.copy(coverUri = newCoverUri))
                }
            }
        }
    }

    /**
     * Retrieve albums from database into viewModel
     */
    private fun refreshAlbums() {
        viewModelScope.launch {
            repository.getAlbumsWithWallpaperAndFolder().collect { albumWithWallpapers ->
                _state.update { it.copy(
                    albumsWithWallpapers = albumWithWallpapers
                ) }
            }
        }
    }

    /**
     * Retrieve wallpaper URIs from a folder directory URI
     */
    private fun getWallpaperFromFolder(folderUri: String, context: Context): List<String> {
        val folderDocumentFile = DocumentFileCompat.fromTreeUri(context, folderUri.toUri())
        return listFilesRecursive(folderDocumentFile, context)
    }

    /**
     * Helper function to recursively list files in a directory
     */
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

    /**
     * Helper function to find the first valid URI from a list of wallpapers
     * It will search through all wallpapers of an album first, and then all wallpapers of every folder of an album
     */
    private fun findFirstValidUri(wallpapers: List<Wallpaper>, folders: List<Folder>): String? {
        wallpapers.forEach { wallpaper ->
            val file = DocumentFile.fromSingleUri(context, wallpaper.wallpaperUri.toUri())
            if (file?.exists() == true) {
                return wallpaper.wallpaperUri
            }
        }
        folders.forEach { folder ->
            folder.wallpapers.forEach { wallpaper ->
                val file = DocumentFile.fromSingleUri(context, wallpaper.toUri())
                if (file?.exists() == true) {
                    return wallpaper
                }
            }
        }
        return null
    }
}

