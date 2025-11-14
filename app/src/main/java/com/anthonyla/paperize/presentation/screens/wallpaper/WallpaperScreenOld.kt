package com.anthonyla.paperize.presentation.screens.wallpaper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.presentation.common.components.SettingClickableItem
import com.anthonyla.paperize.presentation.common.components.SettingSliderItem
import com.anthonyla.paperize.presentation.common.components.SettingSwitchItem
import com.anthonyla.paperize.presentation.screens.wallpaper.components.AlbumSelectionBottomSheet

@Composable
fun WallpaperScreen(
    albums: List<Album>,
    selectedAlbums: List<Album>,
    scheduleSettings: ScheduleSettings,
    animate: Boolean,
    onToggleChanger: (Boolean) -> Unit,
    onSelectAlbum: (Album) -> Unit,
    onUpdateScheduleSettings: (ScheduleSettings) -> Unit,
    onChangeWallpaperNow: (ScreenType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAlbumSelectionSheet by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
            onClick = { showAlbumSelectionSheet = true },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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
            onCheckedChange = onToggleChanger,
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

    if (showAlbumSelectionSheet) {
        AlbumSelectionBottomSheet(
            albums = albums,
            selectedAlbums = selectedAlbums,
            onAlbumSelect = onSelectAlbum,
            onDismiss = { showAlbumSelectionSheet = false }
        )
    }
}

private fun formatInterval(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes min"
        minutes < 1440 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes / 1440}d ${(minutes % 1440) / 60}h"
    }
}
