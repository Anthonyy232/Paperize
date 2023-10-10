package com.anthonyla.paperize.feature.wallpaper.presentation.home_screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components.HomeTopBar
import com.anthonyla.paperize.feature.wallpaper.presentation.library_screen.LibraryScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.tabItems
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsState
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper.WallpaperState
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperScreen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    wallpaperState: WallpaperState,
    settingsState: SettingsState,
    onSettingsClick: () -> Unit,
    onContactClick: () -> Unit,
    onLaunchImagePhotoPicker: () -> Unit,
    onDeleteImagesClick: (List<String>) -> Unit
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState { tabItems.size }
    var showSelectionModeAppBar by remember { mutableStateOf(false) }
    var selectionCount by rememberSaveable { mutableIntStateOf(0) }
    var allSelected by rememberSaveable { mutableStateOf(false) }
    var selectAll by rememberSaveable { mutableStateOf(false) }
    var deleteImages by rememberSaveable { mutableStateOf(false) }


    LaunchedEffect(tabIndex) { pagerState.animateScrollToPage(tabIndex) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page -> tabIndex = page }
    }

    Scaffold (
        topBar = {
            HomeTopBar(
                onSettingsClick = onSettingsClick,
                onContactClick = onContactClick,
                showSelectionModeAppBar = showSelectionModeAppBar,
                selectionCount = selectionCount,
                allSelected = allSelected,
                onSelectAllClick = { selectAll = true },
                deleteImagesOnClick = {
                    deleteImages = true
                    showSelectionModeAppBar = false
                }
            )
        }
    ) {padding ->
        LaunchedEffect(tabIndex) { pagerState.animateScrollToPage(tabIndex) }
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page -> tabIndex = page }
        }
        Column(
            modifier = Modifier.padding(padding)
        ) {
            SecondaryTabRow(
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
            HorizontalPager(state = pagerState) { index ->
                when(index) {
                    0 -> WallpaperScreen(
                        settingsState = settingsState,
                        wallpaperState = wallpaperState
                    )
                    1 -> LibraryScreen(
                        settingsState = settingsState,
                        wallpaperState = wallpaperState,
                        onLaunchImagePhotoPicker = onLaunchImagePhotoPicker,
                        onSelectionMode = { showSelectionModeAppBar = it },
                        onUpdateItemCount = { selectionCount = it },
                        onAllSelected = {allSelected = it},
                        selectAll = selectAll,
                        selectAllDone = { selectAll = false },
                        deleteImages = deleteImages,
                        onDeleteImagesClick = {
                            onDeleteImagesClick(it)
                            deleteImages = false
                        }
                    )
                }
            }
        }
    }
}