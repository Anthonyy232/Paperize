package com.anthonyla.paperize.feature.wallpaper.presentation.album

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.findFirstValidUri
import com.anthonyla.paperize.core.getFolderMetadata
import com.anthonyla.paperize.core.getWallpaperFromFolder
import com.anthonyla.paperize.core.isDirectory
import com.anthonyla.paperize.core.isValidUri
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
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
        loadSelectedAlbumFlow(),
        _state
    ) { albums: List<AlbumWithWallpaperAndFolder>, selectedAlbum: List<AlbumWithWallpaperAndFolder>, currentState: AlbumsState ->
        currentState.copy(
            albumsWithWallpapers = albums,
            selectedAlbum = selectedAlbum
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumsState()
    )

    private fun loadAlbumsFlow() = repository.getAlbumsWithWallpaperAndFolder()
    private fun loadSelectedAlbumFlow() = repository.getSelectedAlbums()

    init {
        viewModelScope.launch {
            state.collect { newState -> _state.value = newState }
        }
    }

    fun onEvent(event: AlbumsEvent) {
        when (event) {
            is AlbumsEvent.AddSelectedAlbum -> {
                viewModelScope.launch {
                    event.deselectAlbumName?.let { repository.updateAlbumSelection(it, false) }
                    val homeWallpapers = if (event.shuffle) event.album.totalWallpapers.shuffled().map { it.wallpaperUri }
                    else event.album.sortedTotalWallpapers.map { it.wallpaperUri }
                    val lockWallpapers = if (event.shuffle) event.album.totalWallpapers.shuffled().map { it.wallpaperUri }
                    else event.album.sortedTotalWallpapers.map { it.wallpaperUri }
                    repository.updateAlbum(
                        event.album.album.copy(
                            selected = true,
                            lockWallpapersInQueue = lockWallpapers,
                            homeWallpapersInQueue = homeWallpapers
                        )
                    )

                    _state.value.albumsWithWallpapers
                        .filter { it.album.initialAlbumName != event.album.album.initialAlbumName }
                        .forEach { album ->
                            val otherHomeWallpapers = if (event.shuffle) album.totalWallpapers.shuffled().map { it.wallpaperUri }
                            else album.sortedTotalWallpapers.map { it.wallpaperUri }
                            val otherLockWallpapers = if (event.shuffle) album.totalWallpapers.shuffled().map { it.wallpaperUri }
                            else album.sortedTotalWallpapers.map { it.wallpaperUri }
                            repository.updateAlbum(
                                album.album.copy(
                                    lockWallpapersInQueue = otherLockWallpapers,
                                    homeWallpapersInQueue = otherHomeWallpapers
                                )
                            )
                        }
                }
            }

            is AlbumsEvent.RemoveSelectedAlbum -> {
                viewModelScope.launch {
                    repository.updateAlbumSelection(event.deselectAlbumName, false)
                }
            }

            is AlbumsEvent.DeselectSelected -> {
                viewModelScope.launch {
                    repository.deselectAllAlbums()
                }
            }

            is AlbumsEvent.Refresh -> {
                viewModelScope.launch {
                    updateAlbums()
                }
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
            _state.value.albumsWithWallpapers.forEach { album ->
                // Remove invalid wallpapers
                val validWallpapers = async {
                    album.wallpapers
                        .asSequence()
                        .filter { isValidUri(context, it.wallpaperUri) }
                        .mapIndexed { index, wallpaper -> wallpaper.copy(order = index) }
                        .toList()
                }

                // Remove invalid folders and inner wallpapers
                val validFolders = async {
                    album.folders
                        .asSequence()
                        .filterNot { isDirectory(context, it.folderUri) }
                        .map { folder ->
                            async {
                                val metadata = getFolderMetadata(folder.folderUri, context)
                                if (metadata.lastModified != folder.dateModified) {
                                    val existingWallpaperUris = folder.wallpaperUris
                                        .asSequence()
                                        .filter { isValidUri(context, it) }
                                        .toList()

                                    val wallpapersOnDisk = getWallpaperFromFolder(folder.folderUri, context)
                                    val newWallpapers = wallpapersOnDisk
                                        .asSequence()
                                        .filterNot { new -> existingWallpaperUris.contains(new.wallpaperUri) }
                                        .mapIndexed { index, wallpaper ->
                                            wallpaper.copy(
                                                initialAlbumName = album.album.initialAlbumName,
                                                order = album.wallpapers.size + 1 + index,
                                                key = album.album.initialAlbumName.hashCode() +
                                                        folder.folderUri.hashCode() +
                                                        wallpaper.wallpaperUri.hashCode()
                                            )
                                        }.toList()

                                    val combinedWallpaperUris = (existingWallpaperUris + newWallpapers.map { it.wallpaperUri }).sorted()
                                    folder.copy(
                                        coverUri = newWallpapers.firstOrNull()?.wallpaperUri ?: folder.coverUri,
                                        wallpaperUris = combinedWallpaperUris,
                                        dateModified = metadata.lastModified,
                                        folderName = metadata.filename
                                    )
                                }
                                else { folder }
                            }
                        }
                        .toList()
                        .awaitAll()
                }

                val folders = validFolders.await()
                val wallpapers = validWallpapers.await()
                val coverUri = findFirstValidUri(context, folders, wallpapers)
                repository.upsertAlbumWithWallpaperAndFolder(
                    album.copy(
                        album = album.album.copy(coverUri = coverUri),
                        wallpapers = wallpapers,
                        folders = folders
                    )
                )
            }
        }
    }
}

