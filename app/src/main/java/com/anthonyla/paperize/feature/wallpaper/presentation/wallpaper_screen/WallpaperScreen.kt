package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.AlbumBottomSheet
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.CurrentAndNextChange
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.CurrentSelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.SetLockScreenSwitch
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.TimeSliders
import kotlinx.coroutines.launch

@Composable
fun WallpaperScreen(
    albumsViewModel: AlbumsViewModel = hiltViewModel(),
    wallpaperScreenViewModel: WallpaperScreenViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onScheduleWallpaperChanger: (Int) -> Unit,
    onSetLockWithHome: (Boolean) -> Unit,
    onStop: () -> Unit,
    animate: Boolean,
    interval: Int,
    setLockWithHome: Boolean,
    lastSetTime: String?,
    nextSetTime: String?,
    selectedAlbum: SelectedAlbum?
) {
    val albumState = albumsViewModel.state.collectAsStateWithLifecycle()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        modifier = Modifier.padding(PaddingValues(horizontal = 16.dp, vertical = 24.dp)),
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            ) },
        modifier = Modifier.fillMaxSize(),
        content = { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    CurrentSelectedAlbum(
                        selectedAlbum = selectedAlbum,
                        onOpenBottomSheet = {
                            if (albumState.value.albumsWithWallpapers.firstOrNull() != null) openBottomSheet = true
                        },
                        onStop = {
                            if (selectedAlbum != null) {
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        message = "${selectedAlbum.album.displayedAlbumName} has been unselected.",
                                        actionLabel = "Dismiss",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                onStop()
                            }
                        },
                        animate = animate
                    )
                }
                item {
                    if (animate) {
                        AnimatedVisibility(
                            visible = selectedAlbum != null,
                            enter = slideInVertically(initialOffsetY = { -it }),
                            exit = fadeOut()
                        ) {
                            if (selectedAlbum != null) {
                                CurrentAndNextChange(lastSetTime, nextSetTime)
                            }
                        }
                    }
                    else {
                        if (selectedAlbum != null) {
                            CurrentAndNextChange(lastSetTime, nextSetTime)
                        }
                    }
                }
                item {
                    if (animate) {
                        AnimatedVisibility(
                            visible = selectedAlbum != null,
                            enter = slideInVertically(initialOffsetY = { -it }),
                            exit = fadeOut()
                        ) {
                            if (selectedAlbum != null) {
                                SetLockScreenSwitch(
                                    albumUri = selectedAlbum.album.coverUri,
                                    checked = setLockWithHome,
                                    onCheckedChange = { onSetLockWithHome(it) },
                                    animate = true
                                )
                            }
                        }
                    }
                    else {
                        if (selectedAlbum != null) {
                            SetLockScreenSwitch(
                                albumUri = selectedAlbum.album.coverUri,
                                checked = setLockWithHome,
                                onCheckedChange = { onSetLockWithHome(it) },
                                animate = false
                            )
                        }
                    }
                }
                item {
                    if (animate) {
                        AnimatedVisibility(
                            visible = selectedAlbum != null,
                            enter = slideInVertically(initialOffsetY = { -it }),
                            exit = fadeOut()
                        ) {
                            TimeSliders(
                                timeInMinutes = interval,
                                onTimeChange = { days, hours, minutes ->
                                    val totalMinutes = 24 * days * 60 + hours * 60 + minutes
                                    settingsViewModel.onEvent(
                                        SettingsEvent.SetWallpaperInterval(totalMinutes)
                                    )
                                    onScheduleWallpaperChanger(totalMinutes)
                                }
                            )
                        }
                    }
                    else {
                        if (selectedAlbum != null) {
                            TimeSliders(
                                timeInMinutes = interval,
                                onTimeChange = { days, hours, minutes ->
                                    val totalMinutes = 24 * days * 60 + hours * 60 + minutes
                                    settingsViewModel.onEvent(
                                        SettingsEvent.SetWallpaperInterval(totalMinutes)
                                    )
                                    onScheduleWallpaperChanger(totalMinutes)
                                }
                            )
                        }
                    }
                }
            }
            if (openBottomSheet) {
                AlbumBottomSheet(
                    albums = albumState.value.albumsWithWallpapers,
                    currentSelectedAlbum = selectedAlbum,
                    onDismiss = { openBottomSheet = false },
                    onSelect = { album ->
                        val wallpapers: List<Wallpaper> = album.wallpapers + album.folders.asSequence().flatMap { folder ->
                            folder.wallpapers.asSequence().map { wallpaper ->
                                Wallpaper(
                                    initialAlbumName = album.album.initialAlbumName,
                                    wallpaperUri = wallpaper,
                                    key = wallpaper.hashCode() + album.album.initialAlbumName.hashCode(),
                                )
                            }
                        }.toList()
                        val newSelectedAlbum = SelectedAlbum(
                            album = album.album.copy(
                                wallpapersInQueue = wallpapers.map { it.wallpaperUri }.shuffled()
                            ),
                            wallpapers = wallpapers
                        )
                        wallpaperScreenViewModel.onEvent(WallpaperEvent.UpdateSelectedAlbum(newSelectedAlbum))
                        openBottomSheet = false
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                message = "${album.album.displayedAlbumName} has been selected.",
                                actionLabel = "Dismiss",
                                duration = SnackbarDuration.Short
                            )
                        }
                        onScheduleWallpaperChanger(interval)
                    },
                    animate = animate
                )
            }
        },
    )
}