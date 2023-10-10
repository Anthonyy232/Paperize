package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper


import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.feature.wallpaper.use_case.WallpaperUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    private val wallpaperUseCases: WallpaperUseCases
) : ViewModel() {
    private val _state = mutableStateOf(WallpaperState())
    val state: State<WallpaperState> = _state
    private var currentGetJob: Job?= null

    init { refreshWallpapers() }

    fun onEvent(event: WallpaperEvent) {
        when (event) {
            is WallpaperEvent.AddWallpaper -> {
                viewModelScope.launch {
                    wallpaperUseCases.addWallpaper(event.uriString)
                }
            }
            is WallpaperEvent.DeleteWallpaper -> {
                viewModelScope.launch {
                    wallpaperUseCases.deleteWallpaper(event.uriString)
                }
            }
        }
    }

    fun refreshWallpapers() {
        currentGetJob?.cancel()
        currentGetJob = wallpaperUseCases.getWallpapers().onEach { wallpapers ->
            _state.value = state.value.copy(
                wallpapers = wallpapers
            )
        }.launchIn(viewModelScope)
    }
}

