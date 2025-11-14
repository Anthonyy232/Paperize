package com.anthonyla.paperize.presentation.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.presentation.common.components.*

/**
 * Enhanced Home screen with full features
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val albums by viewModel.albums.collectAsState()
    val selectedAlbums by viewModel.selectedAlbums.collectAsState()
    val scheduleSettings by viewModel.scheduleSettings.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddAlbumDialog by remember { mutableStateOf(false) }
    var showAlbumSelectionSheet by remember { mutableStateOf(false) }
    var showDeleteAlbumDialog by remember { mutableStateOf<Album?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
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
        },
        floatingActionButton = {
            if (selectedTabIndex == 1) {
                FloatingActionButton(
                    onClick = { showAddAlbumDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_album)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(R.string.wallpaper_screen)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(stringResource(R.string.library_screen)) }
                )
            }

            // Tab content
            when (selectedTabIndex) {
                0 -> WallpaperTab(
                    scheduleSettings = scheduleSettings,
                    selectedAlbums = selectedAlbums,
                    onToggleWallpaperChanger = { viewModel.toggleWallpaperChanger(it) },
                    onUpdateScheduleSettings = { viewModel.updateScheduleSettings(it) },
                    onShowAlbumSelection = { showAlbumSelectionSheet = true },
                    onChangeWallpaperNow = { viewModel.changeWallpaperNow(it) }
                )
                1 -> LibraryTab(
                    albums = albums,
                    onAlbumClick = onNavigateToAlbum,
                    onAlbumLongClick = { showDeleteAlbumDialog = it }
                )
            }
        }
    }

    // Dialogs and Bottom Sheets
    if (showAddAlbumDialog) {
        AddAlbumDialog(
            onDismiss = { showAddAlbumDialog = false },
            onConfirm = { name ->
                viewModel.createAlbum(name)
                showAddAlbumDialog = false
            }
        )
    }

    if (showAlbumSelectionSheet) {
        AlbumSelectionBottomSheet(
            albums = albums,
            selectedAlbums = selectedAlbums,
            onAlbumSelect = { album ->
                viewModel.toggleAlbumSelection(album)
            },
            onDismiss = { showAlbumSelectionSheet = false }
        )
    }

    showDeleteAlbumDialog?.let { album ->
        DeleteAlbumDialog(
            albumName = album.name,
            onDismiss = { showDeleteAlbumDialog = null },
            onConfirm = {
                viewModel.deleteAlbum(album.id)
                showDeleteAlbumDialog = null
            }
        )
    }
}

@Composable
private fun WallpaperTab(
    scheduleSettings: ScheduleSettings,
    selectedAlbums: List<Album>,
    onToggleWallpaperChanger: (Boolean) -> Unit,
    onUpdateScheduleSettings: (ScheduleSettings) -> Unit,
    onShowAlbumSelection: () -> Unit,
    onChangeWallpaperNow: (ScreenType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selected Album Card
        SettingClickableItem(
            title = if (selectedAlbums.isEmpty()) {
                stringResource(R.string.no_album_selected)
            } else {
                selectedAlbums.first().name
            },
            description = if (selectedAlbums.isNotEmpty()) {
                stringResource(R.string.currently_selected_album)
            } else {
                stringResource(R.string.click_to_select_a_different_album)
            },
            onClick = onShowAlbumSelection,
            trailingContent = {
                Icon(
                    imageVector = ArrowForward,
                    contentDescription = null
                )
            }
        )

        // Enable wallpaper changer
        SettingSwitchItem(
            title = stringResource(R.string.enable_wallpaper_changer),
            description = if (selectedAlbums.isEmpty()) {
                stringResource(R.string.no_album_selected)
            } else null,
            checked = scheduleSettings.enableChanger,
            onCheckedChange = onToggleWallpaperChanger,
            enabled = selectedAlbums.isNotEmpty()
        )

        // Manual wallpaper change buttons
        if (scheduleSettings.enableChanger && selectedAlbums.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.change_wallpaper),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onChangeWallpaperNow(ScreenType.HOME) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.home))
                        }
                        Button(
                            onClick = { onChangeWallpaperNow(ScreenType.LOCK) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.lock))
                        }
                    }
                }
            }
        }

        // Interval settings
        if (scheduleSettings.enableChanger) {
            // Separate schedules toggle
            SettingSwitchItem(
                title = stringResource(R.string.individual_scheduling),
                description = stringResource(R.string.show_interval_sliders),
                checked = scheduleSettings.separateSchedules,
                onCheckedChange = { enabled ->
                    onUpdateScheduleSettings(scheduleSettings.copy(separateSchedules = enabled))
                }
            )

            // Home interval
            SettingSliderItem(
                title = if (scheduleSettings.separateSchedules) {
                    stringResource(R.string.home_screen_btn)
                } else {
                    stringResource(R.string.interval_text)
                },
                value = scheduleSettings.homeIntervalMinutes.toFloat(),
                onValueChange = { value ->
                    onUpdateScheduleSettings(
                        scheduleSettings.copy(homeIntervalMinutes = value.toInt())
                    )
                },
                valueRange = Constants.MIN_INTERVAL_MINUTES.toFloat()..1440f,
                steps = 50,
                valueLabel = formatInterval(scheduleSettings.homeIntervalMinutes)
            )

            // Lock interval (if separate schedules)
            if (scheduleSettings.separateSchedules) {
                SettingSliderItem(
                    title = stringResource(R.string.lock_screen_btn),
                    value = scheduleSettings.lockIntervalMinutes.toFloat(),
                    onValueChange = { value ->
                        onUpdateScheduleSettings(
                            scheduleSettings.copy(lockIntervalMinutes = value.toInt())
                        )
                    },
                    valueRange = Constants.MIN_INTERVAL_MINUTES.toFloat()..1440f,
                    steps = 50,
                    valueLabel = formatInterval(scheduleSettings.lockIntervalMinutes)
                )
            }
        }
    }
}

@Composable
private fun LibraryTab(
    albums: List<Album>,
    onAlbumClick: (String) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.no_albums_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(albums, key = { it.id }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album.id) },
                    onLongClick = { onAlbumLongClick(album) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                if (album.isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.currently_selected_album),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = pluralStringResource(R.plurals.wallpaper_count, album.totalWallpaperCount, album.totalWallpaperCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (album.folders.isNotEmpty()) {
                    Text(
                        text = "${album.folders.size} ${stringResource(R.string.folders)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatInterval(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes min"
        minutes < 1440 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes / 1440}d ${(minutes % 1440) / 60}h"
    }
}
