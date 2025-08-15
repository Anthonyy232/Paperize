package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TimePickerState
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
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState.EffectSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState.ScheduleSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState.ThemeSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState.WallpaperSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.AlbumBottomSheet
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.BlurSwitchAndSlider
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.ChangerSelectionRow
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.CurrentAndNextChange
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.CurrentSelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.DarkenSwitchAndSlider
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.GrayscaleSwitchAndSlider
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.IndividualSchedulingAndToggleRow
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.RefreshSwitch
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.ShuffleSwitch
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.SkipLandscapeSwitch
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.TimeSliders
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.VignetteSwitchAndSlider
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.WallpaperPreviewAndScale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen(
    albums: List<AlbumWithWallpaperAndFolder>,
    homeSelectedAlbum: AlbumWithWallpaperAndFolder?,
    lockSelectedAlbum: AlbumWithWallpaperAndFolder?,
    wallpaperSettings: WallpaperSettings,
    scheduleSettings: ScheduleSettings,
    themeSettings: ThemeSettings,
    effectSettings: EffectSettings,
    onDarkCheck: (Boolean) -> Unit,
    onDarkenPercentage: (Int, Int) -> Unit,
    onHomeCheckedChange: (Boolean) -> Unit,
    onLockCheckedChange: (Boolean) -> Unit,
    onScalingChange: (ScalingConstants) -> Unit,
    onScheduleSeparatelyChange: (Boolean) -> Unit,
    onSelectAlbum: (AlbumWithWallpaperAndFolder, Boolean, Boolean) -> Unit,
    onHomeTimeChange: (Int) -> Unit,
    onLockTimeChange: (Int) -> Unit,
    onDeselect: (Boolean, Boolean) -> Unit,
    onToggleChanger: (Boolean) -> Unit,
    onBlurPercentageChange: (Int, Int) -> Unit,
    onBlurChange: (Boolean) -> Unit,
    onVignettePercentageChange: (Int, Int) -> Unit,
    onVignetteChange: (Boolean) -> Unit,
    onGrayscalePercentageChange: (Int, Int) -> Unit,
    onGrayscaleChange: (Boolean) -> Unit,
    onChangeStartTimeToggle: (Boolean) -> Unit,
    onStartTimeChange: (TimePickerState) -> Unit,
    onShuffleCheck: (Boolean) -> Unit,
    onRefreshChange: (Boolean) -> Unit,
    onSkipLandscapeChange: (Boolean) -> Unit
) {
    val shouldShowScreen = wallpaperSettings.setHomeWallpaper || wallpaperSettings.setLockWallpaper
    val shouldShowSettings = shouldShowScreen && homeSelectedAlbum != null && lockSelectedAlbum != null
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val showInterval = rememberSaveable { mutableStateOf(false) }
    val lockSource = rememberSaveable { mutableStateOf(false) }
    val homeSource = rememberSaveable { mutableStateOf(false) }

    val handleAlbumSelection: (AlbumWithWallpaperAndFolder) -> Unit = { album ->
        openBottomSheet = false
        val alarmManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.getSystemService(context, AlarmManager::class.java)
        } else null

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager?.canScheduleExactAlarms() == false -> {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
            else -> onSelectAlbum(album, lockSource.value, homeSource.value)
        }
    }

    val showSnackBar: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = context.getString(R.string.dismiss),
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { it
        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ChangerSelectionRow(
                onHomeCheckedChange = onHomeCheckedChange,
                onLockCheckedChange = onLockCheckedChange,
                homeEnabled = wallpaperSettings.setHomeWallpaper,
                lockEnabled = wallpaperSettings.setLockWallpaper
            )

            if (wallpaperSettings.setHomeWallpaper && wallpaperSettings.setLockWallpaper) {
                IndividualSchedulingAndToggleRow(
                    onToggleChanger = onToggleChanger,
                    onScheduleSeparatelyChange = onScheduleSeparatelyChange,
                    scheduleSeparately = scheduleSettings.scheduleSeparately,
                    enableChanger = wallpaperSettings.enableChanger,
                    animate = themeSettings.animate
                )
            }

            if (shouldShowScreen) {
                CurrentSelectedAlbum(
                    homeSelectedAlbum = homeSelectedAlbum,
                    lockSelectedAlbum = lockSelectedAlbum,
                    onToggleChanger = {
                        if (!it) showSnackBar(context.getString(R.string.wallpaper_changer_has_been_disabled))
                        onToggleChanger(it)
                    },
                    onOpenBottomSheet = { changeLock, changeHome ->
                        if (albums.firstOrNull() != null) {
                            openBottomSheet = true
                            lockSource.value = changeLock
                            homeSource.value = changeHome
                        } else {
                            showSnackBar(context.getString(R.string.no_albums_found))
                        }
                    },
                    onDeselect = { lock, home ->
                        if (homeSelectedAlbum != null || lockSelectedAlbum != null) {
                            onDeselect(lock, home)
                        }
                    },
                    scheduleSeparately = scheduleSettings.scheduleSeparately,
                    enableChanger = wallpaperSettings.enableChanger,
                    animate = themeSettings.animate
                )

                if (shouldShowSettings) {
                    WallpaperPreviewAndScale(
                        currentHomeWallpaper = wallpaperSettings.currentHomeWallpaper,
                        currentLockWallpaper = wallpaperSettings.currentLockWallpaper,
                        scaling = wallpaperSettings.wallpaperScaling,
                        onScalingChange = onScalingChange,
                        homeBlurPercentage = effectSettings.homeBlurPercentage,
                        lockBlurPercentage = effectSettings.lockBlurPercentage,
                        homeDarkenPercentage = effectSettings.homeDarkenPercentage,
                        lockDarkenPercentage = effectSettings.lockDarkenPercentage,
                        homeVignettePercentage = effectSettings.homeVignettePercentage,
                        lockVignettePercentage = effectSettings.lockVignettePercentage,
                        homeGrayscalePercentage = effectSettings.homeGrayscalePercentage,
                        lockGrayscalePercentage = effectSettings.lockGrayscalePercentage,
                        homeEnabled = wallpaperSettings.setHomeWallpaper,
                        lockEnabled = wallpaperSettings.setLockWallpaper,
                        darken = effectSettings.darken,
                        blur = effectSettings.blur,
                        vignette = effectSettings.vignette,
                        grayscale = effectSettings.grayscale
                    )
                    CurrentAndNextChange(scheduleSettings.lastSetTime, scheduleSettings.nextSetTime)
                    TimeSliders(
                        homeInterval = scheduleSettings.homeInterval,
                        lockInterval = scheduleSettings.lockInterval,
                        startingTime = scheduleSettings.startTime,
                        onHomeIntervalChange = { days, hours, minutes ->
                            val totalMinutes = 24 * days * 60 + hours * 60 + minutes
                            onHomeTimeChange(totalMinutes)
                        },
                        onLockIntervalChange = { days, hours, minutes ->
                            val totalMinutes = 24 * days * 60 + hours * 60 + minutes
                            onLockTimeChange(totalMinutes)
                        },
                        onStartTimeChange = onStartTimeChange,
                        onShowIntervalChange = { showInterval.value = it },
                        onChangeStartTimeToggle = onChangeStartTimeToggle,
                        homeEnabled = wallpaperSettings.setHomeWallpaper,
                        lockEnabled = wallpaperSettings.setLockWallpaper,
                        showInterval = showInterval.value,
                        scheduleSeparately = scheduleSettings.scheduleSeparately,
                        changeStartTime = scheduleSettings.changeStartTime,
                        animate = themeSettings.animate
                    )
                    ShuffleSwitch(
                        shuffle = scheduleSettings.shuffle,
                        onShuffleCheck = { onShuffleCheck(it) },
                    )
                    DarkenSwitchAndSlider(
                        homeDarkenPercentage = effectSettings.homeDarkenPercentage,
                        lockDarkenPercentage = effectSettings.lockDarkenPercentage,
                        onDarkCheck = onDarkCheck,
                        onDarkenChange = onDarkenPercentage,
                        darken = effectSettings.darken,
                        animate = themeSettings.animate,
                        bothEnabled = wallpaperSettings.setHomeWallpaper && wallpaperSettings.setLockWallpaper
                    )
                    BlurSwitchAndSlider(
                        homeBlurPercentage = effectSettings.homeBlurPercentage,
                        lockBlurPercentage = effectSettings.lockBlurPercentage,
                        onBlurPercentageChange = onBlurPercentageChange,
                        onBlurChange = onBlurChange,
                        blur = effectSettings.blur,
                        animate = themeSettings.animate,
                        bothEnabled = wallpaperSettings.setHomeWallpaper && wallpaperSettings.setLockWallpaper
                    )
                    VignetteSwitchAndSlider(
                        homeVignettePercentage = effectSettings.homeVignettePercentage,
                        lockVignettePercentage = effectSettings.lockVignettePercentage,
                        onVignettePercentageChange = onVignettePercentageChange,
                        onVignetteChange = onVignetteChange,
                        vignette = effectSettings.vignette,
                        animate = themeSettings.animate,
                        bothEnabled = wallpaperSettings.setHomeWallpaper && wallpaperSettings.setLockWallpaper
                    )
                    GrayscaleSwitchAndSlider(
                        homeGrayscalePercentage = effectSettings.homeGrayscalePercentage,
                        lockGrayscalePercentage = effectSettings.lockGrayscalePercentage,
                        onGrayscalePercentageChange = onGrayscalePercentageChange,
                        onGrayscaleChange = onGrayscaleChange,
                        grayscale = effectSettings.grayscale,
                        animate = themeSettings.animate,
                        bothEnabled = wallpaperSettings.setHomeWallpaper && wallpaperSettings.setLockWallpaper
                    )
                    RefreshSwitch(
                        refresh = scheduleSettings.refresh,
                        onRefreshChange = onRefreshChange
                    )
                    SkipLandscapeSwitch(
                        skipLandscape = scheduleSettings.skipLandscape,
                        onSkipLandscapeChange = onSkipLandscapeChange
                    )
                }
            }

            if (shouldShowScreen && openBottomSheet) {
                AlbumBottomSheet(
                    albums = albums,
                    homeSelectedAlbum = homeSelectedAlbum,
                    lockSelectedAlbum = lockSelectedAlbum,
                    onSelect = handleAlbumSelection,
                    onDismiss = { openBottomSheet = false },
                    animate = themeSettings.animate
                )
            }
        }
    }
}