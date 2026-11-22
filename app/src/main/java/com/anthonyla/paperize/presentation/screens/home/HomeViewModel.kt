package com.anthonyla.paperize.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.runCatchingCancellable
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.Wallpaper
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
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    settingsRepository: SettingsRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val createAlbumUseCase: CreateAlbumUseCase,
    private val coordinator: SettingsCoordinator
) : ViewModel() {

    val albums: StateFlow<List<AlbumSummary>> =
        albumRepository.getAlbumSummaries().stateWhileSubscribed(viewModelScope, emptyList())

    val scheduleSettings: StateFlow<ScheduleSettings> =
        settingsRepository.getScheduleSettingsFlow().stateWhileSubscribed(viewModelScope, ScheduleSettings())

    val animate: StateFlow<Boolean> =
        settingsRepository.getAppSettingsFlow().map { it.animate }.stateWhileSubscribed(viewModelScope, true)

    val wallpaperMode: StateFlow<WallpaperMode?> =
        settingsRepository.getWallpaperModeFlow().stateWhileSubscribed(viewModelScope, null)

    /**
     * Wallpaper Paperize last applied for the home / lock screen, for the in-app preview.
     * Reading the source URI (rather than WallpaperManager.getDrawable) avoids the storage permission
     * the preview would otherwise need. Falls back to the BOTH-mode record when home/lock are synced.
     */
    val currentHomeWallpaper: StateFlow<Wallpaper?> = currentWallpaper(ScreenType.HOME) { it.homeAlbumId }

    val currentLockWallpaper: StateFlow<Wallpaper?> = currentWallpaper(ScreenType.LOCK) { it.lockAlbumId }

    private val _removedWallpaper = MutableStateFlow<Wallpaper?>(null)
    /** The last wallpaper removed from the preview, while its undo is offered. */
    val removedWallpaper: StateFlow<Wallpaper?> = _removedWallpaper.asStateFlow()

    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message.asStateFlow()

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

    /** Creates the album and returns null, or returns the string resource of the error to show. */
    suspend fun createAlbum(name: String): Int? = runCatchingCancellable { createAlbumUseCase(name) }.fold(
        onSuccess = { created -> if (created) null else R.string.album_name_exists_error },
        onFailure = { R.string.album_create_failed }
    )

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

    /** Removes [wallpaper] from its album and replaces it on every screen that shows it. */
    fun removeCurrentWallpaper(wallpaper: Wallpaper) {
        val home = currentHomeWallpaper.value?.id == wallpaper.id
        val lock = currentLockWallpaper.value?.id == wallpaper.id
        viewModelScope.launch {
            runCatchingCancellable { albumRepository.removeWallpaper(wallpaper.id) }
                .onSuccess { removed ->
                    if (removed == null) return@onSuccess
                    _removedWallpaper.value = removed
                    coordinator.changeWallpaperNow(
                        if (home && lock) ScreenType.BOTH else if (lock) ScreenType.LOCK else ScreenType.HOME
                    )
                }
                .onFailure { _message.value = R.string.remove_wallpaper_failed }
        }
    }

    fun restoreWallpaper(wallpaper: Wallpaper) {
        _removedWallpaper.value = null
        viewModelScope.launch {
            runCatchingCancellable { albumRepository.restoreWallpaper(wallpaper) }
                .onFailure { _message.value = R.string.restore_wallpaper_failed }
        }
    }

    fun dismissRemovedWallpaper() { _removedWallpaper.value = null }

    fun dismissMessage() { _message.value = null }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun currentWallpaper(screen: ScreenType, albumId: (ScheduleSettings) -> String?): StateFlow<Wallpaper?> =
        scheduleSettings
            .map(albumId)
            .distinctUntilChanged()
            .flatMapLatest { id -> if (id == null) flowOf(null) else currentWallpaperFlow(id, screen) }
            .stateWhileSubscribed(viewModelScope, null)

    private fun currentWallpaperFlow(albumId: String, screen: ScreenType): Flow<Wallpaper?> = combine(
        wallpaperRepository.getCurrentWallpaperFlow(albumId, screen),
        wallpaperRepository.getCurrentWallpaperFlow(albumId, ScreenType.BOTH)
    ) { specific, both -> specific ?: both }
        // Removing the shown image drops its record until the replacement is applied.
        .scan(null as Wallpaper?) { last, current -> current ?: last }
}
