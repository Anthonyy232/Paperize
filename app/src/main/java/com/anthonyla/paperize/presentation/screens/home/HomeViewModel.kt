package com.anthonyla.paperize.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.runCatchingCancellable
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.domain.model.AppSettings
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.domain.usecase.CreateAlbumUseCase
import com.anthonyla.paperize.presentation.common.stateWhileSubscribed
import com.anthonyla.paperize.service.SettingsCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    albumRepository: AlbumRepository,
    settingsRepository: SettingsRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val createAlbumUseCase: CreateAlbumUseCase,
    private val coordinator: SettingsCoordinator
) : ViewModel() {

    val albums: StateFlow<List<AlbumSummary>> =
        albumRepository.getAlbumSummaries().stateWhileSubscribed(viewModelScope, emptyList())

    val scheduleSettings: StateFlow<ScheduleSettings> =
        settingsRepository.getScheduleSettingsFlow().stateWhileSubscribed(viewModelScope, ScheduleSettings())

    val appSettings: StateFlow<AppSettings> =
        settingsRepository.getAppSettingsFlow().stateWhileSubscribed(viewModelScope, AppSettings())

    val wallpaperMode: StateFlow<WallpaperMode?> =
        settingsRepository.getWallpaperModeFlow().stateWhileSubscribed(viewModelScope, null)

    /**
     * URI of the wallpaper Paperize last applied for the home / lock screen, for the in-app preview.
     * Reading the source URI (rather than WallpaperManager.getDrawable) avoids the storage permission
     * the preview would otherwise need. Falls back to the BOTH-mode record when home/lock are synced.
     */
    val currentHomeWallpaperUri: StateFlow<String?> = currentWallpaperUri(ScreenType.HOME) { it.homeAlbumId }

    val currentLockWallpaperUri: StateFlow<String?> = currentWallpaperUri(ScreenType.LOCK) { it.lockAlbumId }

    private val _showLiveWallpaperPrompt = MutableStateFlow(false)
    val showLiveWallpaperPrompt: StateFlow<Boolean> = _showLiveWallpaperPrompt.asStateFlow()

    private var pendingSettingsJob: Job? = null

    init {
        // Launched in order so later edits queue behind this check in the coordinator.
        viewModelScope.launch { coordinator.disableIfLiveWallpaperReplaced() }
    }

    fun dismissLiveWallpaperPrompt() {
        _showLiveWallpaperPrompt.value = false
    }

    suspend fun createAlbum(name: String) = runCatchingCancellable { createAlbumUseCase(name) }

    fun selectAlbum(screen: ScreenType, album: AlbumSummary?) {
        viewModelScope.launch { coordinator.selectAlbum(screen, album?.id) }
    }

    fun setChangerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (coordinator.setChangerEnabled(enabled)) _showLiveWallpaperPrompt.value = true
        }
    }

    fun updateScheduleSettings(settings: ScheduleSettings) {
        pendingSettingsJob?.cancel()
        pendingSettingsJob = null
        viewModelScope.launch { coordinator.updateScheduleSettings(settings) }
    }

    fun updateScheduleSettingsDebounced(settings: ScheduleSettings) {
        pendingSettingsJob?.cancel()
        pendingSettingsJob = viewModelScope.launch {
            delay(Constants.SETTINGS_DEBOUNCE_MS)
            pendingSettingsJob = null
            coordinator.updateScheduleSettings(settings)
        }
    }

    fun changeWallpaperNow() = coordinator.changeWallpaperNow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun currentWallpaperUri(screen: ScreenType, albumId: (ScheduleSettings) -> String?): StateFlow<String?> =
        scheduleSettings
            .map(albumId)
            .distinctUntilChanged()
            .flatMapLatest { id -> if (id == null) flowOf(null) else currentWallpaperUriFlow(id, screen) }
            .stateWhileSubscribed(viewModelScope, null)

    private fun currentWallpaperUriFlow(albumId: String, screen: ScreenType): Flow<String?> = combine(
        wallpaperRepository.getCurrentWallpaperFlow(albumId, screen),
        wallpaperRepository.getCurrentWallpaperFlow(albumId, ScreenType.BOTH)
    ) { specific, both -> (specific ?: both)?.uri }
}
