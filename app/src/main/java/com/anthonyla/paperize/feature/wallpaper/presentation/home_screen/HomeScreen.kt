package com.anthonyla.paperize.feature.wallpaper.presentation.home_screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumDialog
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components.HomeTopBar
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components.getTabItems
import com.anthonyla.paperize.feature.wallpaper.presentation.library_screen.LibraryScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState.EffectSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState.ScheduleSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState.ThemeSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState.WallpaperSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    albums: List<AlbumWithWallpaperAndFolder>,
    homeSelectedAlbum: AlbumWithWallpaperAndFolder?,
    lockSelectedAlbum: AlbumWithWallpaperAndFolder?,
    themeSettings: ThemeSettings,
    wallpaperSettings: WallpaperSettings,
    scheduleSettings: ScheduleSettings,
    effectSettings: EffectSettings,
    onNavigateAddWallpaper: (String) -> Unit,
    onViewAlbum: (String) -> Unit,
    onDarkCheck: (Boolean) -> Unit,
    onDarkenPercentage: (Int, Int) -> Unit,
    onHomeCheckedChange: (Boolean) -> Unit,
    onLockCheckedChange: (Boolean) -> Unit,
    onScalingChange: (ScalingConstants) -> Unit,
    onSelectAlbum: (AlbumWithWallpaperAndFolder, Boolean, Boolean) -> Unit,
    onScheduleSeparatelyChange: (Boolean) -> Unit,
    onHomeTimeChange: (Int) -> Unit,
    onLockTimeChange: (Int) -> Unit,
    onSettingsClick: () -> Unit,
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
    onRefreshChange: (Boolean) -> Unit
) {
    val tabItems = getTabItems()
    val pagerState = rememberPagerState(0) { tabItems.size }
    var tabIndex by rememberSaveable { mutableIntStateOf(pagerState.currentPage) }
    var addAlbumDialog by rememberSaveable { mutableStateOf(false) }
    if (addAlbumDialog) AddAlbumDialog(
        onDismissRequest = { addAlbumDialog = false },
        onConfirmation = { onNavigateAddWallpaper(it) }
    )

    LaunchedEffect(tabIndex) {
        pagerState.animateScrollToPage(tabIndex)
    }
    LaunchedEffect(pagerState.currentPage) {
        tabIndex = pagerState.currentPage
    }

    Scaffold (
        topBar = {
            HomeTopBar(
                showSelectionModeAppBar = false,
                selectionCount = 0,
                onSettingsClick = onSettingsClick,
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            PrimaryTabRow(
                selectedTabIndex = tabIndex,
            ) {
                tabItems.forEachIndexed { index, item ->
                    Tab(
                        selected = (index == tabIndex),
                        onClick = { tabIndex = index },
                        text = { Text(text = item.title) },
                        icon = {
                            Icon(
                                imageVector = if (index == tabIndex) item.filledIcon else item.unfilledIcon,
                                contentDescription = item.title
                            )
                        }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1
            ) { index ->
                when (index.coerceIn(tabItems.indices)) {
                    0 -> WallpaperScreen(
                        albums = albums,
                        homeSelectedAlbum = homeSelectedAlbum,
                        lockSelectedAlbum = lockSelectedAlbum,
                        themeSettings = themeSettings,
                        wallpaperSettings = wallpaperSettings,
                        scheduleSettings = scheduleSettings,
                        effectSettings = effectSettings,
                        onDarkCheck = onDarkCheck,
                        onDarkenPercentage = onDarkenPercentage,
                        onHomeCheckedChange = onHomeCheckedChange,
                        onLockCheckedChange = onLockCheckedChange,
                        onScalingChange = onScalingChange,
                        onScheduleSeparatelyChange = onScheduleSeparatelyChange,
                        onSelectAlbum = onSelectAlbum,
                        onHomeTimeChange = onHomeTimeChange,
                        onLockTimeChange = onLockTimeChange,
                        onDeselect = onDeselect,
                        onToggleChanger = onToggleChanger,
                        onBlurPercentageChange = onBlurPercentageChange,
                        onBlurChange = onBlurChange,
                        onVignettePercentageChange = onVignettePercentageChange,
                        onVignetteChange = onVignetteChange,
                        onGrayscalePercentageChange = onGrayscalePercentageChange,
                        onGrayscaleChange = onGrayscaleChange,
                        onChangeStartTimeToggle = onChangeStartTimeToggle,
                        onStartTimeChange = onStartTimeChange,
                        onShuffleCheck = onShuffleCheck,
                        onRefreshChange = onRefreshChange
                    )
                    else -> LibraryScreen(
                        albums = albums,
                        onAddNewAlbumClick = { addAlbumDialog = true },
                        onViewAlbum = onViewAlbum,
                    )
                }
            }
        }
    }
}