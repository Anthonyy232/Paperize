package com.anthonyla.paperize.presentation.screens.home

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.presentation.screens.home.components.HomeTopBar
import com.anthonyla.paperize.presentation.screens.home.components.getTabItems
import com.anthonyla.paperize.presentation.screens.library.LibraryScreen
import com.anthonyla.paperize.presentation.screens.wallpaper.WallpaperScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val albums by viewModel.albums.collectAsState()
    val scheduleSettings by viewModel.scheduleSettings.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val wallpaperMode by viewModel.wallpaperMode.collectAsState()
    val showLiveWallpaperPrompt by viewModel.showLiveWallpaperPrompt.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val tabItems = getTabItems(
        wallpaperTitle = stringResource(R.string.tab_wallpaper),
        libraryTitle = stringResource(R.string.tab_library)
    )

    // Persist the initial tab index across navigation
    val initialTab = rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialTab.intValue) { tabItems.size }

    // Save current tab when it changes
    LaunchedEffect(pagerState.currentPage) {
        initialTab.intValue = pagerState.currentPage
    }

    // Smooth entry animation state (persisted across navigation)
    var showContent by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Only animate on first composition, not when navigating back
        if (!showContent) {
            // Small delay to ensure smooth appearance (respects animation setting)
            if (appSettings.animate) {
                delay(50)  // Minimal delay for smooth entry
            }
            showContent = true
        }
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
        AnimatedVisibility(
            visible = showContent,
            enter = if (appSettings.animate) {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    ),
                    initialOffsetY = { it / 20 }  // Small slide from bottom (5%)
                )
            } else {
                fadeIn(animationSpec = tween(durationMillis = 0))
            }
        ) {
            Column(modifier = modifier.padding(paddingValues)) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage
                ) {
                    tabItems.forEachIndexed { index, item ->
                        Tab(
                            selected = (index == pagerState.currentPage),
                            onClick = {
                                // Animate to the selected page when tab is clicked
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(text = item.title) },
                            icon = {
                                Icon(
                                    imageVector = if (index == pagerState.currentPage) item.filledIcon else item.unfilledIcon,
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
                            scheduleSettings = scheduleSettings,
                            appSettings = appSettings,
                            wallpaperMode = wallpaperMode,
                            onToggleChanger = { viewModel.toggleWallpaperChanger(it, onlyIfNotScheduled = true) },
                            onSelectHomeAlbum = { album -> viewModel.selectHomeAlbum(album) },
                            onSelectLockAlbum = { album -> viewModel.selectLockAlbum(album) },
                            onSelectLiveAlbum = { album -> viewModel.selectLiveAlbum(album) },
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

    // Live Wallpaper Selection Prompt
    if (showLiveWallpaperPrompt && wallpaperMode == WallpaperMode.LIVE) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLiveWallpaperPrompt() },
            title = {
                Text(
                    text = "Select Live Wallpaper",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "To use Paperize in Live Wallpaper mode, you need to select it as your live wallpaper from the system settings.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "\nTap 'Open Wallpaper Picker' below to select Paperize from the list of available live wallpapers.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissLiveWallpaperPrompt()
                        try {
                            // Open live wallpaper chooser with Paperize pre-selected
                            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                putExtra(
                                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                    ComponentName(
                                        context.packageName,
                                        "com.anthonyla.paperize.service.livewallpaper.PaperizeLiveWallpaperService"
                                    )
                                )
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback: Open general wallpaper settings
                            try {
                                val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                                context.startActivity(fallbackIntent)
                            } catch (e2: Exception) {
                                // Last resort: Open wallpaper settings
                                val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                context.startActivity(settingsIntent)
                            }
                        }
                    }
                ) {
                    Text("Open Wallpaper Picker")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLiveWallpaperPrompt() }) {
                    Text("Later")
                }
            }
        )
    }
}
