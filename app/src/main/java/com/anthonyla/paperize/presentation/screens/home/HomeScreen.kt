package com.anthonyla.paperize.presentation.screens.home

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.screens.library.LibraryScreen
import com.anthonyla.paperize.presentation.screens.wallpaper.WallpaperScreen
import com.anthonyla.paperize.service.livewallpaper.PaperizeLiveWallpaperService
import kotlinx.coroutines.launch

private class HomeTab(
    @StringRes val title: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val homeTabs = listOf(
    HomeTab(R.string.wallpaper, Icons.Outlined.Image, Icons.Filled.Image),
    HomeTab(R.string.library, Icons.Outlined.PhotoLibrary, Icons.Filled.PhotoLibrary)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val scheduleSettings by viewModel.scheduleSettings.collectAsStateWithLifecycle()
    val animate by viewModel.animate.collectAsStateWithLifecycle()
    val wallpaperMode by viewModel.wallpaperMode.collectAsStateWithLifecycle()
    val showLiveWallpaperPrompt by viewModel.showLiveWallpaperPrompt.collectAsStateWithLifecycle()
    val currentHomeWallpaperUri by viewModel.currentHomeWallpaperUri.collectAsStateWithLifecycle()
    val currentLockWallpaperUri by viewModel.currentLockWallpaperUri.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val pagerState = rememberPagerState { homeTabs.size }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_screen)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = modifier.padding(paddingValues)) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage
            ) {
                homeTabs.forEachIndexed { index, tab ->
                    val selected = index == pagerState.currentPage
                    val title = stringResource(tab.title)
                    Tab(
                        selected = selected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(text = title) },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.icon,
                                contentDescription = title
                            )
                        }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1
            ) { index ->
                when (index) {
                    0 -> {
                        val mode = wallpaperMode
                        if (mode == null) {
                            Box(modifier = Modifier.fillMaxSize())
                        } else {
                            WallpaperScreen(
                                albums = albums,
                                persistedScheduleSettings = scheduleSettings,
                                animate = animate,
                                wallpaperMode = mode,
                                onToggleChanger = viewModel::setChangerEnabled,
                                onSelectAlbum = viewModel::selectAlbum,
                                onUpdateScheduleSettings = viewModel::updateScheduleSettings,
                                onUpdateScheduleSettingsDebounced = viewModel::updateScheduleSettingsDebounced,
                                onChangeWallpaperNow = viewModel::changeWallpaperNow,
                                homeWallpaperUri = currentHomeWallpaperUri,
                                lockWallpaperUri = currentLockWallpaperUri
                            )
                        }
                    }
                    else -> LibraryScreen(
                        albums = albums,
                        onViewAlbum = onNavigateToAlbum,
                        onCreateAlbum = viewModel::createAlbum
                    )
                }
            }
        }
    }

    if (showLiveWallpaperPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLiveWallpaperPrompt() },
            title = {
                Text(
                    text = stringResource(R.string.select_live_wallpaper),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.select_live_wallpaper_instruction),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.open_wallpaper_picker_instruction_newline),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissLiveWallpaperPrompt()
                        openLiveWallpaperPicker(context)
                    }
                ) {
                    Text(stringResource(R.string.open_wallpaper_picker))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLiveWallpaperPrompt() }) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }
}

/** Opens the most specific wallpaper picker the device offers. */
private fun openLiveWallpaperPicker(context: Context) {
    val pickers = listOf(
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(context, PaperizeLiveWallpaperService::class.java)
        ),
        Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER),
        Intent(Settings.ACTION_SETTINGS)
    )
    for (picker in pickers) {
        try {
            context.startActivity(picker)
            return
        } catch (_: ActivityNotFoundException) {
            // Fall through to the next, more generic screen.
        }
    }
}
