package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import android.app.WallpaperManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.AlbumBottomSheet
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.CurrentAndNextChange
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.CurrentSelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.DarkenSwitchAndSlider
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.SetLockScreenSwitch
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.ShowLiveWallpaperEnabledDialog
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.TimeSliders
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.WallpaperScalingRow
import kotlinx.coroutines.launch

@Composable
fun WallpaperScreen(
    albumsViewModel: AlbumsViewModel = hiltViewModel(),
    onScheduleWallpaperChanger: (Int) -> Unit,
    onSetLockWithHome: (Boolean) -> Unit,
    onToggleChanger: (Boolean) -> Unit,
    onSelectAlbum: (AlbumWithWallpaperAndFolder) -> Unit,
    onStop: () -> Unit,
    animate: Boolean,
    interval: Int,
    setLockWithHome: Boolean,
    lastSetTime: String?,
    nextSetTime: String?,
    selectedAlbum: SelectedAlbum?,
    enableChanger: Boolean,
    darkenPercentage: Int,
    onDarkenPercentage: (Int) -> Unit,
    darken: Boolean,
    onDarkCheck: (Boolean) -> Unit,
    scaling: ScalingConstants,
    onScalingChange: (ScalingConstants) -> Unit
) {
    val context = LocalContext.current
    val albumState = albumsViewModel.state.collectAsStateWithLifecycle()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val openDialog = rememberSaveable { mutableStateOf(false) }
    val showInterval = rememberSaveable { mutableStateOf(false) }

    if (openDialog.value) {
        ShowLiveWallpaperEnabledDialog(
            onDismissRequest = { openDialog.value = false }
        )
    }

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
                            if (albumState.value.albumsWithWallpapers.firstOrNull() != null) {
                                if (isLiveWallpaperSet(context)) {
                                    openDialog.value = true
                                }
                                else { openBottomSheet = true }
                            }
                            else {
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.no_albums_found),
                                        actionLabel = context.getString(R.string.dismiss),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        },
                        onStop = {
                            if (selectedAlbum != null) {
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.has_been_unselected, selectedAlbum.album.displayedAlbumName),
                                        actionLabel = context.getString(R.string.dismiss),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                onStop()
                            }
                        },
                        animate = animate,
                        enableChanger = enableChanger,
                        onToggleChanger = {
                            if (!it) {
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.wallpaper_changer_has_been_disabled),
                                        actionLabel = context.getString(R.string.dismiss),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                            onToggleChanger(it)
                        }
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
                                    albumUri = selectedAlbum.album.currentWallpaper,
                                    checked = setLockWithHome,
                                    onCheckedChange = { onSetLockWithHome(it) },
                                    animate = true,
                                    darken = darken,
                                    darkenPercentage = darkenPercentage,
                                    scaling = scaling
                                )
                            }
                        }
                    }
                    else {
                        if (selectedAlbum != null) {
                            SetLockScreenSwitch(
                                albumUri = selectedAlbum.album.currentWallpaper,
                                checked = setLockWithHome,
                                onCheckedChange = { onSetLockWithHome(it) },
                                animate = false,
                                darken = darken,
                                darkenPercentage = darkenPercentage,
                                scaling = scaling
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
                            WallpaperScalingRow(
                                scaling = scaling,
                                onScalingChange = onScalingChange
                            )
                        }
                    }
                    else {
                        if (selectedAlbum != null) {
                            WallpaperScalingRow(
                                scaling = scaling,
                                onScalingChange = onScalingChange
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
                                    onScheduleWallpaperChanger(totalMinutes)
                                },
                                showInterval = showInterval.value,
                                animate = true,
                                onShowIntervalChange = { showInterval.value = it }
                            )
                        }
                    }
                    else {
                        if (selectedAlbum != null) {
                            TimeSliders(
                                timeInMinutes = interval,
                                onTimeChange = { days, hours, minutes ->
                                    val totalMinutes = 24 * days * 60 + hours * 60 + minutes
                                    onScheduleWallpaperChanger(totalMinutes)
                                },
                                showInterval = showInterval.value,
                                animate = false,
                                onShowIntervalChange = { showInterval.value = it }
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
                            DarkenSwitchAndSlider(
                                onDarkCheck = onDarkCheck,
                                darken = darken,
                                onDarkenChange = onDarkenPercentage,
                                darkenPercentage = darkenPercentage,
                                animate = true
                            )
                        }
                    }
                    else {
                        if (selectedAlbum != null) {
                            DarkenSwitchAndSlider(
                                onDarkCheck = onDarkCheck,
                                darken = darken,
                                onDarkenChange = onDarkenPercentage,
                                darkenPercentage = darkenPercentage,
                                animate = false
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
                        openBottomSheet = false
                        onSelectAlbum(album)
                        onScheduleWallpaperChanger(interval)
                    },
                    animate = animate
                )
            }
        },
    )
}

fun isLiveWallpaperSet(context: Context): Boolean {
    val wallpaperManager = WallpaperManager.getInstance(context)
    return wallpaperManager.wallpaperInfo != null
}