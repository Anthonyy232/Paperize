package com.anthonyla.paperize.feature.wallpaper.presentation.home_screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
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
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumDialog
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components.HomeTopBar
import com.anthonyla.paperize.feature.wallpaper.presentation.library_screen.LibraryScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.tabItems
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperScreen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    navigateToAddWallpaperScreen: (String) -> Unit,
    onAlbumViewClick: (String) -> Unit,
    onScheduleWallpaperChanger: (Int) -> Unit,
    onStop: () -> Unit
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
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
        LaunchedEffect(tabIndex) { pagerState.animateScrollToPage(tabIndex) }
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page -> tabIndex = page }
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
                outOfBoundsPageCount = 0,
                userScrollEnabled = true,
            ) { index ->
                when(index) {
                    0 -> WallpaperScreen(
                        onScheduleWallpaperChanger = onScheduleWallpaperChanger,
                        onStop = onStop
                    )
                    1 -> LibraryScreen(
                        onAddNewAlbumClick = { addAlbumDialog = true },
                        onViewAlbum = onAlbumViewClick
                    )
                }
            }
        }
    }
}