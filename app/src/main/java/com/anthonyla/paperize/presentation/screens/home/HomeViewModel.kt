package com.anthonyla.paperize.presentation.screens.home

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.util.isPaperizeLiveWallpaperActive
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.usecase.CreateAlbumUseCase
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService
import com.anthonyla.paperize.service.worker.WallpaperScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    albumRepository: AlbumRepository,
    private val createAlbumUseCase: CreateAlbumUseCase,
    private val settingsRepository: SettingsRepository,
    private val wallpaperScheduler: WallpaperScheduler,
    private val wallpaperRepository: com.anthonyla.paperize.domain.repository.WallpaperRepository
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val settingsMutex = Mutex()

    // Keep settings writes and their scheduling side effects ordered during rapid UI changes.
    private fun launchSettingsUpdate(action: suspend () -> Unit) = viewModelScope.launch {
        settingsMutex.withLock { action() }
    }

    init {
        checkLiveWallpaperStatus()
    }

    val albums: StateFlow<List<AlbumSummary>> = albumRepository.getAlbumSummaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList()
        )

    val scheduleSettings: StateFlow<ScheduleSettings> = settingsRepository.getScheduleSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ScheduleSettings()
        )

    val appSettings = settingsRepository.getAppSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
            initialValue = com.anthonyla.paperize.domain.model.AppSettings()
        )

    val wallpaperMode = settingsRepository.getWallpaperModeFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS),
            initialValue = null
        )

    /**
     * URI of the wallpaper Paperize last applied for the home / lock screen, for the in-app preview.
     * Reading the source URI (rather than WallpaperManager.getDrawable) avoids the storage permission
     * the preview would otherwise need. Falls back to the BOTH-mode record when home/lock are synced.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentHomeWallpaperUri: StateFlow<String?> = scheduleSettings
        .map { it.homeAlbumId }
        .distinctUntilChanged()
        .flatMapLatest { albumId -> currentWallpaperUriFlow(albumId, ScreenType.HOME) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentLockWallpaperUri: StateFlow<String?> = scheduleSettings
        .map { it.lockAlbumId }
        .distinctUntilChanged()
        .flatMapLatest { albumId -> currentWallpaperUriFlow(albumId, ScreenType.LOCK) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(Constants.FLOW_SUBSCRIPTION_TIMEOUT_MS), null)

    private fun currentWallpaperUriFlow(albumId: String?, screenType: ScreenType): Flow<String?> =
        if (albumId == null) {
            flowOf(null)
        } else {
            combine(
                wallpaperRepository.getCurrentWallpaperFlow(albumId, screenType),
                wallpaperRepository.getCurrentWallpaperFlow(albumId, ScreenType.BOTH)
            ) { specific, both -> (specific ?: both)?.uri }
        }

    private val _showLiveWallpaperPrompt = MutableStateFlow(false)
    val showLiveWallpaperPrompt: StateFlow<Boolean> = _showLiveWallpaperPrompt

    fun dismissLiveWallpaperPrompt() {
        _showLiveWallpaperPrompt.value = false
    }

    private fun checkLiveWallpaperStatus() {
        launchSettingsUpdate {
            val mode = settingsRepository.getWallpaperMode()
            val settings = settingsRepository.getScheduleSettings()

            if (mode == WallpaperMode.LIVE &&
                settings.liveAlbumId != null &&
                settings.enableChanger) {

                val isActive = isPaperizeLiveWallpaperActive(context)
                Log.d(TAG, "Startup check: LIVE mode with album selected, isPaperizeLiveWallpaperActive=$isActive")

                if (!isActive) {
                    Log.w(TAG, "Paperize live wallpaper was replaced/disabled - clearing live album selection")
                    settingsRepository.updateLiveAlbumId(null)
                    settingsRepository.updateEnableChanger(false)
                    wallpaperScheduler.cancelAllWallpaperChanges()
                }
            }
        }
    }

    suspend fun createAlbum(name: String) = createAlbumUseCase(name)

    fun selectHomeAlbum(album: AlbumSummary?) = selectAlbum(album, ScreenType.HOME, settingsRepository::updateHomeAlbumId)

    fun selectLockAlbum(album: AlbumSummary?) = selectAlbum(album, ScreenType.LOCK, settingsRepository::updateLockAlbumId)

    fun selectLiveAlbum(album: AlbumSummary?) = selectAlbum(album, ScreenType.LIVE, settingsRepository::updateLiveAlbumId)

    private fun selectAlbum(
        album: AlbumSummary?,
        screen: ScreenType,
        updateSelection: suspend (String?) -> Unit
    ) {
        launchSettingsUpdate {
            updateSelection(album?.id)
            val mode = settingsRepository.getWallpaperMode()
            var updated = settingsRepository.getScheduleSettings()
            if (album == null && updated.activeScreens(mode).isEmpty()) {
                settingsRepository.updateEnableChanger(false)
                updated = updated.copy(enableChanger = false)
            }
            wallpaperScheduler.updateSchedules(updated, mode)
            if (album != null && updated.enableChanger && updated.hasRequiredAlbums(mode) && mode == WallpaperMode.STATIC) {
                val target = if (ScreenType.BOTH in updated.activeScreens(mode)) ScreenType.BOTH else screen
                changeWallpaperNow(target)
            }
        }
    }

    fun toggleWallpaperChanger(enabled: Boolean, onlyIfNotScheduled: Boolean = false) {
        launchSettingsUpdate {
            settingsRepository.updateEnableChanger(enabled)
            val updated = settingsRepository.getScheduleSettings()
            val mode = settingsRepository.getWallpaperMode()
            wallpaperScheduler.updateSchedules(updated, mode, onlyIfNotScheduled)
            if (enabled && updated.hasRequiredAlbums(mode)) {
                if (!onlyIfNotScheduled && mode == WallpaperMode.STATIC) {
                    updated.activeScreens(mode).forEach(::changeWallpaperNow)
                }
                promptForLiveWallpaper(mode)
            }
        }
    }

    private fun promptForLiveWallpaper(mode: WallpaperMode) {
        if (mode == WallpaperMode.LIVE && !isPaperizeLiveWallpaperActive(context)) {
            _showLiveWallpaperPrompt.value = true
        }
    }

    fun updateScheduleSettings(settings: ScheduleSettings) {
        pendingSettingsJob?.cancel()
        pendingSettingsJob = null
        applyScheduleSettings(settings)
    }

    private fun applyScheduleSettings(settings: ScheduleSettings) {
        launchSettingsUpdate {
            lateinit var currentSettings: ScheduleSettings
            val validated = settingsRepository.updateScheduleSettings { current ->
                currentSettings = current
                // Album selection and pause/resume have their own actions. A delayed effect edit
                // must not overwrite changes those actions made after this draft was captured.
                settings.copy(
                    enableChanger = current.enableChanger,
                    homeAlbumId = if (settings.homeEnabled) current.homeAlbumId else null,
                    lockAlbumId = if (settings.lockEnabled) current.lockAlbumId else null,
                    liveAlbumId = current.liveAlbumId
                ).validate()
            }
            val shuffleChanged = currentSettings.shuffleEnabled != validated.shuffleEnabled
            val schedulingChanged = validated.hasSchedulingChanges(currentSettings)
            val displayChanged = validated.hasDisplayChanges(currentSettings)

            if (shuffleChanged) {
                wallpaperRepository.clearAllQueues()
            }

            val mode = settingsRepository.getWallpaperMode()
            if (schedulingChanged) {
                wallpaperScheduler.updateSchedules(validated, mode)
            }
            if (validated.enableChanger && validated.hasRequiredAlbums(mode) && displayChanged && mode == WallpaperMode.STATIC) {
                validated.activeScreens(mode).forEach(::reapplyEffectsNow)
            }
        }
    }

    private var pendingSettingsJob: Job? = null

    fun updateScheduleSettingsDebounced(settings: ScheduleSettings) {
        pendingSettingsJob?.cancel()
        pendingSettingsJob = viewModelScope.launch {
            delay(Constants.SETTINGS_DEBOUNCE_MS)
            pendingSettingsJob = null
            applyScheduleSettings(settings)
        }
    }

    fun changeWallpaperNow(screenType: ScreenType) {
        val intent = Intent(context, WallpaperChangeService::class.java).apply {
            action = WallpaperChangeService.ACTION_CHANGE_WALLPAPER
            putExtra(WallpaperChangeService.EXTRA_SCREEN_TYPE, screenType.name)
        }
        context.startForegroundService(intent)
    }

    /**
     * Change whichever wallpaper destinations are currently configured.
     *
     * Synchronized home/lock settings use one BOTH request; independent schedules are
     * changed separately. LIVE is routed through the service to reload the renderer.
     */
    fun changeWallpaperNowForActiveScreens() {
        val mode = wallpaperMode.value ?: return
        scheduleSettings.value.activeScreens(mode).forEach(::changeWallpaperNow)
    }

    fun reapplyEffectsNow(screenType: ScreenType) {
        val intent = Intent(context, WallpaperChangeService::class.java).apply {
            action = WallpaperChangeService.ACTION_REAPPLY_EFFECTS
            putExtra(WallpaperChangeService.EXTRA_SCREEN_TYPE, screenType.name)
        }
        context.startForegroundService(intent)
    }

}
