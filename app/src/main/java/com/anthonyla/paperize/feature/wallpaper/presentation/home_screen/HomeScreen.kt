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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumDialog
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components.HomeTopBar
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components.getTabItems
import com.anthonyla.paperize.feature.wallpaper.presentation.library_screen.LibraryScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperScreen

@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    navigateToAddWallpaperScreen: (String) -> Unit,
    onAlbumViewClick: (String) -> Unit,
    onScheduleWallpaperChanger: (Int) -> Unit,
    onSetLockWithHome: (Boolean) -> Unit,
    onToggleChanger: (Boolean) -> Unit,
    onSelectAlbum: (AlbumWithWallpaperAndFolder) -> Unit,
    onStop: () -> Unit,
    animate : Boolean,
    interval: Int,
    setLockWithHome: Boolean,
    lastSetTime: String?,
    nextSetTime: String?,
    selectedAlbum: SelectedAlbum?,
    enableChanger: Boolean
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabItems = getTabItems()
    val pagerState = rememberPagerState { tabItems.size }
    var addAlbumDialog by rememberSaveable { mutableStateOf(false) }
    if (addAlbumDialog) AddAlbumDialog(
        onDismissRequest = { addAlbumDialog = false },
        onConfirmation = { navigateToAddWallpaperScreen(it) }
    )

    Scaffold (
        topBar = {
            HomeTopBar(
                showSelectionModeAppBar = false,
                selectionCount = 0,
                onSettingsClick = onSettingsClick,
            )
        }
    ) { padding ->
        LaunchedEffect(tabIndex) {
            if (tabIndex in tabItems.indices) {
                pagerState.animateScrollToPage(tabIndex)
            }
        }
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                if (page in tabItems.indices) {
                    tabIndex = page
                }
            }
        }
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = tabIndex,
                indicator = { tabPositions ->
                    if (tabIndex < tabPositions.size) {
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
                                if (index == tabIndex)
                                    item.filledIcon
                                else
                                    item.unfilledIcon,
                                contentDescription = item.title
                            )
                        }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = true,
            ) { index ->
                when(index) {
                    0 -> WallpaperScreen(
                        onScheduleWallpaperChanger = onScheduleWallpaperChanger,
                        onStop = onStop,
                        animate = animate,
                        interval = interval,
                        setLockWithHome = setLockWithHome,
                        lastSetTime = lastSetTime,
                        nextSetTime = nextSetTime,
                        onSetLockWithHome = onSetLockWithHome,
                        selectedAlbum = selectedAlbum,
                        enableChanger = enableChanger,
                        onToggleChanger = onToggleChanger,
                        onSelectAlbum = onSelectAlbum
                    )
                    1 -> LibraryScreen(
                        onAddNewAlbumClick = { addAlbumDialog = true },
                        onViewAlbum = onAlbumViewClick,
                        animate = animate
                    )
                }
            }
        }
    }
}