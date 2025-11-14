package com.anthonyla.paperize.presentation.screens.home

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anthonyla.paperize.presentation.screens.home.components.HomeTopBar
import com.anthonyla.paperize.presentation.screens.home.components.getTabItems
import com.anthonyla.paperize.presentation.screens.library.LibraryScreen
import com.anthonyla.paperize.presentation.screens.wallpaper.WallpaperScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val albums by viewModel.albums.collectAsState()
    val selectedAlbums by viewModel.selectedAlbums.collectAsState()
    val scheduleSettings by viewModel.scheduleSettings.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    val tabItems = getTabItems()
    val pagerState = rememberPagerState(0) { tabItems.size }
    var tabIndex by rememberSaveable { mutableIntStateOf(pagerState.currentPage) }

    LaunchedEffect(tabIndex) {
        pagerState.animateScrollToPage(tabIndex)
    }
    LaunchedEffect(pagerState.currentPage) {
        tabIndex = pagerState.currentPage
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                showSelectionModeAppBar = false,
                selectionCount = 0,
                onSettingsClick = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        Column(modifier = modifier.padding(paddingValues)) {
            PrimaryTabRow(
                selectedTabIndex = tabIndex
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
                        selectedAlbums = selectedAlbums,
                        scheduleSettings = scheduleSettings,
                        animate = appSettings.animate,
                        onToggleChanger = { viewModel.toggleWallpaperChanger(it) },
                        onSelectAlbum = { album -> viewModel.toggleAlbumSelection(album) },
                        onUpdateScheduleSettings = { viewModel.updateScheduleSettings(it) },
                        onChangeWallpaperNow = { viewModel.changeWallpaperNow(it) }
                    )
                    else -> LibraryScreen(
                        albums = albums,
                        onViewAlbum = onNavigateToAlbum,
                        onCreateAlbum = { name -> viewModel.createAlbum(name) }
                    )
                }
            }
        }
    }
}
