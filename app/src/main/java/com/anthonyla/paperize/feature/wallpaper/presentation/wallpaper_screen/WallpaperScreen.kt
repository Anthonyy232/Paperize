package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
    onStop: () -> Unit
) {
    val albumState = albumsViewModel.state.collectAsStateWithLifecycle()
    val selectedState = wallpaperScreenViewModel.state.collectAsStateWithLifecycle()
    val settingsState = settingsViewModel.state.collectAsStateWithLifecycle()
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
                        modifier = Modifier.padding(PaddingValues(horizontal = 16.dp, vertical = 24.dp))
                    )
                }
            ) },
        modifier = Modifier.fillMaxSize(),
        content = { it
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    CurrentSelectedAlbum(
                        selectedAlbum = selectedState.value.selectedAlbum,
                        onOpenBottomSheet = {
                            if (albumState.value.albumsWithWallpapers.firstOrNull() != null) openBottomSheet = true
                        },
                        onStop = {
                            if (selectedState.value.selectedAlbum != null) {
                                wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset)
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        message = "${selectedState.value.selectedAlbum?.album?.displayedAlbumName} has been unselected.",
                                        actionLabel = "Dismiss",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                onStop()
                            }
                        }
                    )
                }
                item {
                    AnimatedVisibility(
                        visible = selectedState.value.selectedAlbum != null,
                        enter = slideInVertically(initialOffsetY = { -it }),
                        exit = fadeOut()
                    ) {
                        TimeSliders(
                            timeInMinutes = settingsState.value.interval,
                            onTimeChange = { days, hours, minutes ->
                                val totalMinutes = 24 * days * 60 + hours * 60 + minutes
                                settingsViewModel.onEvent(SettingsEvent.SetWallpaperInterval(totalMinutes))
                                onScheduleWallpaperChanger(totalMinutes)
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = selectedState.value.selectedAlbum != null,
                        enter = slideInVertically(initialOffsetY = { -it }),
                        exit = fadeOut()
                    ) {
                        if (selectedState.value.selectedAlbum != null) {
                            SetLockScreenSwitch(
                                albumUri = selectedState.value.selectedAlbum!!.album.coverUri,
                                checked = settingsState.value.setLockWithHome,
                                onCheckedChange = { isChecked ->
                                    settingsViewModel.onEvent(SettingsEvent.SetLockWithHome(isChecked))
                                }
                            )
                        }
                    }
                }
            }
            if (openBottomSheet) {
                AlbumBottomSheet(
                    albums = albumState.value.albumsWithWallpapers,
                    currentSelectedAlbum = selectedState.value.selectedAlbum,
                    onDismiss = { openBottomSheet = false },
                    onSelect = { album ->
                        val wallpapers: List<Wallpaper> = album.wallpapers + album.folders.flatMap { folder ->
                            folder.wallpapers.map { wallpaper ->
                                Wallpaper(
                                    initialAlbumName = album.album.initialAlbumName,
                                    wallpaperUri = wallpaper,
                                    key = wallpaper.hashCode() + album.album.initialAlbumName.hashCode(),
                                )
                            }
                        }
                        val newSelectedAlbum = SelectedAlbum(
                            album = album.album.copy(
                                wallpapersInQueue = wallpapers.map { it.wallpaperUri }.shuffled()
                            ),
                            wallpapers = wallpapers
                        )
                        wallpaperScreenViewModel.onEvent(WallpaperEvent.UpdateSelectedAlbum(newSelectedAlbum))
                        onScheduleWallpaperChanger(settingsState.value.interval)
                        openBottomSheet = false
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                message = "${album.album.displayedAlbumName} has been selected.",
                                actionLabel = "Dismiss",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }
        },
    )
}