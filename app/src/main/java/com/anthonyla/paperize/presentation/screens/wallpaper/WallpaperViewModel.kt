package com.anthonyla.paperize.presentation.screens.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.util.getDeviceScreenSize
import com.anthonyla.paperize.core.util.retrieveBitmap
import com.anthonyla.paperize.core.util.toUriOrNull
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.presentation.common.navigation.WallpaperRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val wallpaperRepository: WallpaperRepository
) : ViewModel() {

    private val wallpaperRoute = savedStateHandle.toRoute<WallpaperRoute>()
    private val wallpaperId = wallpaperRoute.wallpaperId

    private val _wallpaper = MutableStateFlow<Wallpaper?>(null)
    val wallpaper: StateFlow<Wallpaper?> = _wallpaper

    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadWallpaper()
    }

    private fun loadWallpaper() {
        viewModelScope.launch {
            _isLoading.value = true
            val wallpaper = wallpaperRepository.getWallpaperById(wallpaperId)
            _wallpaper.value = wallpaper

            wallpaper?.let {
                val uri = it.uri.toUriOrNull()
                if (uri != null) {
                    val screenSize = getDeviceScreenSize(context)
                    val loadedBitmap = retrieveBitmap(
                        context,
                        uri,
                        screenSize.width,
                        screenSize.height
                    )
                    _bitmap.value = loadedBitmap
                }
            }
            _isLoading.value = false
        }
    }

    fun setAsWallpaper(flag: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val bmp = _bitmap.value ?: return@launch
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                wallpaperManager.setBitmap(bmp, null, true, flag)
                onSuccess()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
