package com.anthonyla.paperize.presentation.screens.wallpaper_view

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.presentation.common.navigation.WallpaperViewRoute
import com.anthonyla.paperize.presentation.common.stateWhileSubscribed
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class WallpaperViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    settingsRepository: SettingsRepository
) : ViewModel() {
    private val route = savedStateHandle.toRoute<WallpaperViewRoute>()

    val wallpaperMode = settingsRepository.getWallpaperModeFlow().stateWhileSubscribed(viewModelScope, WallpaperMode.STATIC)

    fun applyTo(screenType: ScreenType) {
        require(screenType != ScreenType.LIVE)
        WallpaperChangeService.applyWallpaper(context, route.wallpaperId, screenType)
    }
}
