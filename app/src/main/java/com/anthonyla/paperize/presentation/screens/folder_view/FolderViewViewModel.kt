package com.anthonyla.paperize.presentation.screens.folder_view
import com.anthonyla.paperize.core.constants.Constants

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.presentation.common.navigation.FolderRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FolderViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    albumRepository: AlbumRepository,
    wallpaperRepository: WallpaperRepository
) : ViewModel() {

    private val folderRoute = savedStateHandle.toRoute<FolderRoute>()
    private val folderId: String = folderRoute.folderId

    val folder: StateFlow<Folder?> = albumRepository.getFolderById(folderId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
            initialValue = null
        )

    val wallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapersByFolder(folderId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList()
        )
}
