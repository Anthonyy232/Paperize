package com.anthonyla.paperize.presentation.screens.wallpaper_view

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.presentation.common.navigation.WallpaperViewRoute
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class WallpaperViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    settingsRepository: SettingsRepository
) : ViewModel() {
    private val route = savedStateHandle.toRoute<WallpaperViewRoute>()

    val wallpaperMode = settingsRepository.getWallpaperModeFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
        initialValue = WallpaperMode.STATIC
    )

    fun applyTo(screenType: ScreenType) {
        require(screenType != ScreenType.LIVE)
        context.startForegroundService(
            Intent(context, WallpaperChangeService::class.java).apply {
                action = WallpaperChangeService.ACTION_APPLY_SPECIFIC_WALLPAPER
                putExtra(WallpaperChangeService.EXTRA_WALLPAPER_ID, route.wallpaperId)
                putExtra(WallpaperChangeService.EXTRA_SCREEN_TYPE, screenType.name)
            }
        )
    }
}
