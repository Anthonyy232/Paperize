package com.anthonyla.paperize.feature.wallpaper.presentation.home_screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumDialog
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components.HomeTopBar
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components.getTabItems
import com.anthonyla.paperize.feature.wallpaper.presentation.library_screen.LibraryScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    albums: List<AlbumWithWallpaperAndFolder>,
    animate : Boolean,
    darken: Boolean,
    homeDarkenPercentage: Int,
    lockDarkenPercentage: Int,
    enableChanger: Boolean,
    homeEnabled : Boolean,
    homeInterval: Int,
    lockInterval: Int,
    lastSetTime: String?,
    lockEnabled : Boolean,
    navigateToAddWallpaperScreen: (String) -> Unit,
    nextSetTime: String?,
    onViewAlbum: (String) -> Unit,
    onDarkCheck: (Boolean) -> Unit,
    onDarkenPercentage: (Int, Int) -> Unit,
    onHomeCheckedChange: (Boolean) -> Unit,
    onLockCheckedChange: (Boolean) -> Unit,
    onScalingChange: (ScalingConstants) -> Unit,
    onScheduleWallpaperChanger: () -> Unit,
    onSelectAlbum: (AlbumWithWallpaperAndFolder, Boolean, Boolean) -> Unit,
    onScheduleSeparatelyChange: (Boolean) -> Unit,
    onHomeTimeChange: (Int) -> Unit,
    onLockTimeChange: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onStop: (Boolean, Boolean) -> Unit,
    onToggleChanger: (Boolean) -> Unit,
    scaling: ScalingConstants,
    scheduleSeparately: Boolean,
    blur: Boolean,
    onBlurPercentageChange: (Int, Int) -> Unit,
    onBlurChange: (Boolean) -> Unit,
    homeBlurPercentage: Int,
    lockBlurPercentage: Int,
    currentHomeWallpaper: String?,
    currentLockWallpaper: String?,
    homeSelectedAlbum: SelectedAlbum?,
    lockSelectedAlbum: SelectedAlbum?,
    homeVignettePercentage: Int,
    lockVignettePercentage: Int,
    onVignettePercentageChange: (Int, Int) -> Unit,
    onVignetteChange: (Boolean) -> Unit,
    vignette: Boolean,
    homeGrayscalePercentage: Int,
    lockGrayscalePercentage: Int,
    onGrayscalePercentageChange: (Int, Int) -> Unit,
    onGrayscaleChange: (Boolean) -> Unit,
    grayscale: Boolean,
    changeStartTime: Boolean,
    onChangeStartTimeToggle: (Boolean) -> Unit,
    onStartTimeChange: (TimePickerState) -> Unit,
    startingTime: Pair<Int, Int>
) {
    val tabItems = getTabItems()
    val pagerState = rememberPagerState(0) { tabItems.size }
    var tabIndex by rememberSaveable { mutableIntStateOf(pagerState.currentPage) }
    var addAlbumDialog by rememberSaveable { mutableStateOf(false) }
    if (addAlbumDialog) AddAlbumDialog(
        onDismissRequest = { addAlbumDialog = false },
        onConfirmation = { navigateToAddWallpaperScreen(it) }
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
            TabRow(
                selectedTabIndex = tabIndex,
                indicator = { tabPositions ->
                    if (tabIndex in tabPositions.indices) {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                            shape = RoundedCornerShape(
                                topStart = 6.dp,
                                topEnd = 6.dp,
                                bottomEnd = 0.dp,
                                bottomStart = 0.dp,
                            ),
                            width = 60.dp
                        )
                    }
                }
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
                        animate = animate,
                        darken = darken,
                        homeDarkenPercentage = homeDarkenPercentage,
                        lockDarkenPercentage = lockDarkenPercentage,
                        enableChanger = enableChanger,
                        homeEnabled = homeEnabled,
                        homeInterval = homeInterval,
                        lockInterval = lockInterval,
                        lastSetTime = lastSetTime,
                        lockEnabled = lockEnabled,
                        nextSetTime = nextSetTime,
                        onDarkCheck = onDarkCheck,
                        onDarkenPercentage = onDarkenPercentage,
                        onHomeCheckedChange = onHomeCheckedChange,
                        onLockCheckedChange = onLockCheckedChange,
                        scheduleSeparately = scheduleSeparately,
                        onScheduleSeparatelyChange = onScheduleSeparatelyChange,
                        onScheduleWallpaperChanger = onScheduleWallpaperChanger,
                        onScalingChange = onScalingChange,
                        onSelectAlbum = onSelectAlbum,
                        onHomeTimeChange = onHomeTimeChange,
                        onLockTimeChange = onLockTimeChange,
                        onStop = onStop,
                        onToggleChanger = onToggleChanger,
                        scaling = scaling,
                        homeSelectedAlbum = homeSelectedAlbum,
                        lockSelectedAlbum = lockSelectedAlbum,
                        blur = blur,
                        onBlurPercentageChange = onBlurPercentageChange,
                        onBlurChange = onBlurChange,
                        homeBlurPercentage = homeBlurPercentage,
                        lockBlurPercentage = lockBlurPercentage,
                        currentHomeWallpaper = currentHomeWallpaper,
                        currentLockWallpaper = currentLockWallpaper,
                        homeVignettePercentage = homeVignettePercentage,
                        lockVignettePercentage = lockVignettePercentage,
                        onVignettePercentageChange = onVignettePercentageChange,
                        onVignetteChange = onVignetteChange,
                        vignette = vignette,
                        homeGrayscalePercentage = homeGrayscalePercentage,
                        lockGrayscalePercentage = lockGrayscalePercentage,
                        onGrayscalePercentageChange = onGrayscalePercentageChange,
                        onGrayscaleChange = onGrayscaleChange,
                        grayscale = grayscale,
                        changeStartTime = changeStartTime,
                        onChangeStartTimeToggle = onChangeStartTimeToggle,
                        onStartTimeChange = onStartTimeChange,
                        startingTime = startingTime
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