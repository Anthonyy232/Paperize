package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.core.content.ContextCompat
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.AlbumBottomSheet
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.BlurSwitchAndSlider
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.ChangerSelectionRow
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.CurrentAndNextChange
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.CurrentSelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.DarkenSwitchAndSlider
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.TimeSliders
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.WallpaperPreviewAndScale
import kotlinx.coroutines.launch

@Composable
fun WallpaperScreen(
    albums: List<AlbumWithWallpaperAndFolder>,
    animate: Boolean,
    darken: Boolean,
    darkenPercentage: Int,
    enableChanger: Boolean,
    homeEnabled: Boolean,
    interval1: Int,
    interval2: Int,
    lastSetTime: String?,
    lockEnabled: Boolean,
    nextSetTime: String?,
    onDarkCheck: (Boolean) -> Unit,
    onDarkenPercentage: (Int) -> Unit,
    onHomeCheckedChange: (Boolean) -> Unit,
    onLockCheckedChange: (Boolean) -> Unit,
    scheduleSeparately: Boolean,
    onScheduleSeparatelyChange: (Boolean) -> Unit,
    onScheduleWallpaperChanger1: (Int) -> Unit,
    onScheduleWallpaperChanger2: (Int) -> Unit,
    onScalingChange: (ScalingConstants) -> Unit,
    onSelectAlbum: (AlbumWithWallpaperAndFolder) -> Unit,
    onTimeChange1: (Int) -> Unit,
    onTimeChange2: (Int) -> Unit,
    onStop: () -> Unit,
    onToggleChanger: (Boolean) -> Unit,
    scaling: ScalingConstants,
    selectedAlbum: SelectedAlbum?,
    blur: Boolean,
    onBlurPercentageChange: (Int) -> Unit,
    onBlurChange: (Boolean) -> Unit,
    blurPercentage: Int
) {
    val shouldShowScreen = homeEnabled || lockEnabled
    val shouldShowSettings = shouldShowScreen && selectedAlbum != null
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val showInterval = rememberSaveable { mutableStateOf(false) }

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
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ChangerSelectionRow(
                    homeEnabled = homeEnabled,
                    lockEnabled = lockEnabled,
                    onHomeCheckedChange = onHomeCheckedChange,
                    onLockCheckedChange = onLockCheckedChange
                )
                if (homeEnabled || lockEnabled) {
                    CurrentSelectedAlbum(
                        selectedAlbum = selectedAlbum,
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
                        },
                        onOpenBottomSheet = {
                            if (albums.firstOrNull() != null) {
                                openBottomSheet = true
                            } else {
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
                                        message = context.getString(
                                            R.string.has_been_unselected,
                                            selectedAlbum.album.displayedAlbumName
                                        ),
                                        actionLabel = context.getString(R.string.dismiss),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                onStop()
                            }
                        }
                    )
                    if (shouldShowSettings) {
                        CurrentAndNextChange(lastSetTime, nextSetTime)
                        WallpaperPreviewAndScale(
                            currentHomeWallpaper = selectedAlbum!!.album.currentHomeWallpaper,
                            currentLockWallpaper = selectedAlbum!!.album.currentLockWallpaper,
                            animate = animate,
                            darken = darken,
                            darkenPercentage = darkenPercentage,
                            scaling = scaling,
                            onScalingChange = onScalingChange,
                            homeEnabled = homeEnabled,
                            lockEnabled = lockEnabled,
                            blur = blur,
                            blurPercentage = blurPercentage
                        )
                        TimeSliders(
                            timeInMinutes1 = interval1,
                            timeInMinutes2 = interval2,
                            onTimeChange1 = { days, hours, minutes ->
                                val totalMinutes = 24 * days * 60 + hours * 60 + minutes
                                onTimeChange1(totalMinutes)
                            },
                            onTimeChange2 = { days, hours, minutes ->
                                val totalMinutes = 24 * days * 60 + hours * 60 + minutes
                                onTimeChange2(totalMinutes)
                            },
                            showInterval = showInterval.value,
                            animate = animate,
                            onShowIntervalChange = { showInterval.value = it },
                            scheduleSeparately = scheduleSeparately,
                            onScheduleSeparatelyChange = onScheduleSeparatelyChange,
                            lockEnabled = lockEnabled,
                            homeEnabled = homeEnabled
                        )
                        DarkenSwitchAndSlider(
                            onDarkCheck = onDarkCheck,
                            darken = darken,
                            onDarkenChange = onDarkenPercentage,
                            darkenPercentage = darkenPercentage,
                            animate = animate
                        )
                        BlurSwitchAndSlider(
                            onBlurPercentageChange = onBlurPercentageChange,
                            onBlurChange = onBlurChange,
                            blur = blur,
                            blurPercentage = blurPercentage,
                            animate = animate
                        )
                    }
                }
            }
            if (shouldShowScreen && openBottomSheet) {
                AlbumBottomSheet(
                    albums = albums,
                    currentSelectedAlbum = selectedAlbum,
                    onDismiss = { openBottomSheet = false },
                    onSelect = { album ->
                        openBottomSheet = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
                            if (alarmManager?.canScheduleExactAlarms() == false) {
                                Intent().also { intent ->
                                    intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                    context.startActivity(intent)
                                }
                            }
                            else {
                                onSelectAlbum(album)
                                onScheduleWallpaperChanger1(interval1)
                            }
                        }
                        else {
                            onSelectAlbum(album)
                            onScheduleWallpaperChanger1(interval1)
                        }
                    },
                    animate = animate
                )
            }
        },
    )
}