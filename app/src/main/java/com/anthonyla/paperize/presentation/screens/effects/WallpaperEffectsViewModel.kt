package com.anthonyla.paperize.presentation.screens.effects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.domain.model.WallpaperEffects
import com.anthonyla.paperize.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WallpaperEffectsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val effects: StateFlow<WallpaperEffects> = settingsRepository.getWallpaperEffectsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WallpaperEffects.default()
        )

    fun updateBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWallpaperEffects(
                effects.value.copy(enableBlur = enabled)
            )
        }
    }

    fun updateBlurPercentage(percentage: Int) {
        viewModelScope.launch {
            settingsRepository.updateWallpaperEffects(
                effects.value.copy(blurPercentage = percentage)
            )
        }
    }

    fun updateDarkenEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWallpaperEffects(
                effects.value.copy(enableDarken = enabled)
            )
        }
    }

    fun updateDarkenPercentage(percentage: Int) {
        viewModelScope.launch {
            settingsRepository.updateWallpaperEffects(
                effects.value.copy(darkenPercentage = percentage)
            )
        }
    }

    fun updateVignetteEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWallpaperEffects(
                effects.value.copy(enableVignette = enabled)
            )
        }
    }

    fun updateVignettePercentage(percentage: Int) {
        viewModelScope.launch {
            settingsRepository.updateWallpaperEffects(
                effects.value.copy(vignettePercentage = percentage)
            )
        }
    }

    fun updateGrayscaleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWallpaperEffects(
                effects.value.copy(enableGrayscale = enabled)
            )
        }
    }

    fun updateShuffle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWallpaperEffects(
                effects.value.copy(shuffle = enabled)
            )
        }
    }

    fun updateSkipLandscape(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWallpaperEffects(
                effects.value.copy(skipLandscape = enabled)
            )
        }
    }

    fun updateSkipNonInteractive(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWallpaperEffects(
                effects.value.copy(skipNonInteractive = enabled)
            )
        }
    }
}
