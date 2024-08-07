package com.anthonyla.paperize.feature.wallpaper.presentation.home_screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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

@Composable
fun HomeScreen(
    albums: List<AlbumWithWallpaperAndFolder>,
    animate : Boolean,
    darken: Boolean,
    darkenPercentage: Int,
    enableChanger: Boolean,
    homeEnabled : Boolean,
    interval1: Int,
    interval2: Int,
    lastSetTime: String?,
    lockEnabled : Boolean,
    navigateToAddWallpaperScreen: (String) -> Unit,
    nextSetTime: String?,
    onAlbumViewClick: (String) -> Unit,
    onDarkCheck: (Boolean) -> Unit,
    onDarkenPercentage: (Int) -> Unit,
    onHomeCheckedChange: (Boolean) -> Unit,
    onLockCheckedChange: (Boolean) -> Unit,
    onScalingChange: (ScalingConstants) -> Unit,
    onScheduleWallpaperChanger1: (Int) -> Unit,
    onScheduleWallpaperChanger2: (Int) -> Unit,
    onSelectAlbum: (AlbumWithWallpaperAndFolder) -> Unit,
    onScheduleSeparatelyChange: (Boolean) -> Unit,
    onTimeChange1: (Int) -> Unit,
    onTimeChange2: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onStop: () -> Unit,
    onToggleChanger: (Boolean) -> Unit,
    scaling: ScalingConstants,
    selectedAlbum: SelectedAlbum?,
    scheduleSeparately: Boolean,
    blur: Boolean,
    onBlurPercentageChange: (Int) -> Unit,
    onBlurChange: (Boolean) -> Unit,
    blurPercentage: Int,
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
                                imageVector =
                                if (index == tabIndex) item.filledIcon
                                else item.unfilledIcon,
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
                when(index.coerceIn(tabItems.indices)) {
                    0 -> WallpaperScreen(
                        albums = albums,
                        animate = animate,
                        darken = darken,
                        darkenPercentage = darkenPercentage,
                        enableChanger = enableChanger,
                        homeEnabled = homeEnabled,
                        interval1 = interval1,
                        interval2 = interval2,
                        lastSetTime = lastSetTime,
                        lockEnabled = lockEnabled,
                        nextSetTime = nextSetTime,
                        onDarkCheck = onDarkCheck,
                        onDarkenPercentage = onDarkenPercentage,
                        onHomeCheckedChange = onHomeCheckedChange,
                        onLockCheckedChange = onLockCheckedChange,
                        scheduleSeparately = scheduleSeparately,
                        onScheduleSeparatelyChange = onScheduleSeparatelyChange,
                        onScheduleWallpaperChanger1 = onScheduleWallpaperChanger1,
                        onScheduleWallpaperChanger2 = onScheduleWallpaperChanger2,
                        onScalingChange = onScalingChange,
                        onSelectAlbum = onSelectAlbum,
                        onTimeChange1 = onTimeChange1,
                        onTimeChange2 = onTimeChange2,
                        onStop = onStop,
                        onToggleChanger = onToggleChanger,
                        scaling = scaling,
                        selectedAlbum = selectedAlbum,
                        blur = blur,
                        onBlurPercentageChange = onBlurPercentageChange,
                        onBlurChange = onBlurChange,
                        blurPercentage = blurPercentage
                    )
                    else -> LibraryScreen(
                        albums = albums,
                        onAddNewAlbumClick = { addAlbumDialog = true },
                        onViewAlbum = onAlbumViewClick,
                        animate = animate
                    )
                }
            }
        }
    }
}