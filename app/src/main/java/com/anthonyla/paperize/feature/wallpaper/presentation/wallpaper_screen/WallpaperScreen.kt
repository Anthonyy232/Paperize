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
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.IndividualSchedulingAndToggleRow
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.TimeSliders
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.WallpaperPreviewAndScale
import kotlinx.coroutines.launch

@Composable
fun WallpaperScreen(
    albums: List<AlbumWithWallpaperAndFolder>,
    animate: Boolean,
    darken: Boolean,
    homeDarkenPercentage: Int,
    lockDarkenPercentage: Int,
    enableChanger: Boolean,
    homeEnabled: Boolean,
    homeInterval: Int,
    lockInterval: Int,
    lastSetTime: String?,
    lockEnabled: Boolean,
    nextSetTime: String?,
    currentHomeWallpaper: String?,
    currentLockWallpaper: String?,
    onDarkCheck: (Boolean) -> Unit,
    onDarkenPercentage: (Int, Int) -> Unit,
    onHomeCheckedChange: (Boolean) -> Unit,
    onLockCheckedChange: (Boolean) -> Unit,
    scheduleSeparately: Boolean,
    onScheduleSeparatelyChange: (Boolean) -> Unit,
    onScheduleWallpaperChanger: () -> Unit,
    onScalingChange: (ScalingConstants) -> Unit,
    onSelectAlbum: (AlbumWithWallpaperAndFolder, Boolean, Boolean) -> Unit,
    onHomeTimeChange: (Int) -> Unit,
    onLockTimeChange: (Int) -> Unit,
    onStop: (Boolean, Boolean) -> Unit,
    onToggleChanger: (Boolean) -> Unit,
    scaling: ScalingConstants,
    homeSelectedAlbum: SelectedAlbum?,
    lockSelectedAlbum: SelectedAlbum?,
    blur: Boolean,
    onBlurPercentageChange: (Int, Int) -> Unit,
    onBlurChange: (Boolean) -> Unit,
    homeBlurPercentage: Int,
    lockBlurPercentage: Int
) {
    val shouldShowScreen = homeEnabled || lockEnabled
    val shouldShowSettings = shouldShowScreen && homeSelectedAlbum != null && lockSelectedAlbum != null
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val showInterval = rememberSaveable { mutableStateOf(false) }
    val lock = rememberSaveable { mutableStateOf(false) }
    val home = rememberSaveable { mutableStateOf(false) }

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
                if (homeEnabled && lockEnabled) {
                    IndividualSchedulingAndToggleRow(
                        animate = animate,
                        scheduleSeparately = scheduleSeparately,
                        enableChanger = enableChanger,
                        onToggleChanger = onToggleChanger,
                        onScheduleSeparatelyChange = onScheduleSeparatelyChange
                    )
                }
                if (homeEnabled || lockEnabled) {
                    CurrentSelectedAlbum(
                        homeSelectedAlbum = homeSelectedAlbum,
                        lockSelectedAlbum = lockSelectedAlbum,
                        scheduleSeparately = scheduleSeparately,
                        animate = animate,
                        enableChanger = enableChanger,
                        onToggleChanger = {
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                if (!it) {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.wallpaper_changer_has_been_disabled),
                                        actionLabel = context.getString(R.string.dismiss),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                            onToggleChanger(it)
                        },
                        onOpenBottomSheet = { changeLock, changeHome ->
                            if (albums.firstOrNull() != null) {
                                openBottomSheet = true
                                lock.value = changeLock
                                home.value = changeHome
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
                        onStop = { lock, home ->
                            if (homeSelectedAlbum != null || lockSelectedAlbum != null) {
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(
                                            R.string.has_been_unselected,
                                            when {
                                                home && lock -> homeSelectedAlbum?.album?.displayedAlbumName ?: ""
                                                home -> homeSelectedAlbum?.album?.displayedAlbumName ?: ""
                                                lock -> lockSelectedAlbum?.album?.displayedAlbumName ?: ""
                                                else -> ""
                                            }
                                        ),
                                        actionLabel = context.getString(R.string.dismiss),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                onStop(lock, home)
                            }
                        }
                    )
                    if (shouldShowSettings) {
                        WallpaperPreviewAndScale(
                            currentHomeWallpaper = currentHomeWallpaper,
                            currentLockWallpaper = currentLockWallpaper,
                            darken = darken,
                            homeDarkenPercentage = homeDarkenPercentage,
                            lockDarkenPercentage = lockDarkenPercentage,
                            scaling = scaling,
                            onScalingChange = onScalingChange,
                            homeEnabled = homeEnabled,
                            lockEnabled = lockEnabled,
                            blur = blur,
                            homeBlurPercentage = homeBlurPercentage,
                            lockBlurPercentage = lockBlurPercentage,
                        )
                        CurrentAndNextChange(lastSetTime, nextSetTime)
                        TimeSliders(
                            homeInterval = homeInterval,
                            lockInterval = lockInterval,
                            onHomeIntervalChange = { days, hours, minutes ->
                                val totalMinutes = 24 * days * 60 + hours * 60 + minutes
                                onHomeTimeChange(totalMinutes)
                            },
                            onLockIntervalChange = { days, hours, minutes ->
                                val totalMinutes = 24 * days * 60 + hours * 60 + minutes
                                onLockTimeChange(totalMinutes)
                            },
                            showInterval = showInterval.value,
                            animate = animate,
                            onShowIntervalChange = { showInterval.value = it },
                            scheduleSeparately = scheduleSeparately,
                            lockEnabled = lockEnabled,
                            homeEnabled = homeEnabled
                        )
                        DarkenSwitchAndSlider(
                            onDarkCheck = onDarkCheck,
                            darken = darken,
                            onDarkenChange = onDarkenPercentage,
                            homeDarkenPercentage = homeDarkenPercentage,
                            lockDarkenPercentage = lockDarkenPercentage,
                            animate = animate,
                            bothEnabled = homeEnabled && lockEnabled
                        )
                        BlurSwitchAndSlider(
                            onBlurPercentageChange = onBlurPercentageChange,
                            onBlurChange = onBlurChange,
                            blur = blur,
                            homeBlurPercentage = homeBlurPercentage,
                            lockBlurPercentage = lockBlurPercentage,
                            animate = animate,
                            bothEnabled = homeEnabled && lockEnabled
                        )
                    }
                }
            }
            if (shouldShowScreen && openBottomSheet) {
                AlbumBottomSheet(
                    albums = albums,
                    homeSelectedAlbum = homeSelectedAlbum,
                    lockSelectedAlbum = lockSelectedAlbum,
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
                                onSelectAlbum(album, lock.value, home.value)
                                onScheduleWallpaperChanger()
                            }
                        }
                        else {
                            onSelectAlbum(album, lock.value, home.value)
                            onScheduleWallpaperChanger()
                        }
                    },
                    animate = animate
                )
            }
        },
    )
}