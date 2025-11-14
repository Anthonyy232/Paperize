package com.anthonyla.paperize.presentation.screens.wallpaper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.AppSettings
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.presentation.common.components.SettingClickableItem
import com.anthonyla.paperize.presentation.common.components.SettingSliderItem
import com.anthonyla.paperize.presentation.common.components.SettingSwitchItem
import com.anthonyla.paperize.presentation.screens.wallpaper.components.AlbumSelectionBottomSheet
import com.anthonyla.paperize.presentation.screens.wallpaper.components.SettingSwitch
import com.anthonyla.paperize.presentation.screens.wallpaper.components.SettingSwitchWithSlider

@Composable
fun WallpaperScreen(
    albums: List<Album>,
    selectedAlbums: List<Album>,
    scheduleSettings: ScheduleSettings,
    appSettings: AppSettings,
    onToggleChanger: (Boolean) -> Unit,
    onSelectAlbum: (Album) -> Unit,
    onUpdateScheduleSettings: (ScheduleSettings) -> Unit,
    onChangeWallpaperNow: (ScreenType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAlbumSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var homeEnabled by remember { mutableStateOf(true) }
    var lockEnabled by remember { mutableStateOf(true) }

    val scalingOptions = listOf(
        stringResource(R.string.fill),
        stringResource(R.string.fit),
        stringResource(R.string.stretch),
        "None"
    )

    var selectedScalingIndex by rememberSaveable {
        mutableIntStateOf(
            when (scheduleSettings.homeScalingType) {
                ScalingType.FILL -> 0
                ScalingType.FIT -> 1
                ScalingType.STRETCH -> 2
                ScalingType.NONE -> 3
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Home and Lock Screen Toggles
        Surface(
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.wallpaper_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W500
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        onClick = { homeEnabled = !homeEnabled }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null)
                            Text(
                                text = stringResource(R.string.home),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (homeEnabled) "Enabled" else "Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (homeEnabled) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        onClick = { lockEnabled = !lockEnabled }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Text(
                                text = stringResource(R.string.lock),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (lockEnabled) "Enabled" else "Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (lockEnabled) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Selected Album
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
                        if (homeEnabled) {
                            Button(
                                onClick = { onChangeWallpaperNow(ScreenType.HOME) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.home))
                            }
                        }
                        if (lockEnabled) {
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
        }

        if (scheduleSettings.enableChanger) {
            // Individual scheduling (only show if both screens enabled)
            if (homeEnabled && lockEnabled) {
                SettingSwitchItem(
                    title = stringResource(R.string.individual_scheduling),
                    description = stringResource(R.string.show_interval_sliders),
                    checked = scheduleSettings.separateSchedules,
                    onCheckedChange = { enabled ->
                        onUpdateScheduleSettings(scheduleSettings.copy(separateSchedules = enabled))
                    }
                )
            }

            // Interval sliders
            if (!scheduleSettings.separateSchedules || !homeEnabled || !lockEnabled) {
                SettingSliderItem(
                    title = stringResource(R.string.interval_text),
                    value = scheduleSettings.homeIntervalMinutes.toFloat(),
                    onValueChange = { value ->
                        onUpdateScheduleSettings(
                            scheduleSettings.copy(
                                homeIntervalMinutes = value.toInt(),
                                lockIntervalMinutes = value.toInt()
                            )
                        )
                    },
                    valueRange = Constants.MIN_INTERVAL_MINUTES.toFloat()..1440f,
                    steps = 50,
                    valueLabel = formatInterval(scheduleSettings.homeIntervalMinutes)
                )
            } else {
                if (homeEnabled) {
                    SettingSliderItem(
                        title = stringResource(R.string.home_screen_btn),
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
                }
                if (lockEnabled) {
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Scaling Options
            Surface(
                tonalElevation = 10.dp,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Scaling",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W500
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        scalingOptions.forEachIndexed { index, label ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = scalingOptions.size
                                ),
                                onClick = {
                                    selectedScalingIndex = index
                                    val scalingType = when (index) {
                                        0 -> ScalingType.FILL
                                        1 -> ScalingType.FIT
                                        2 -> ScalingType.STRETCH
                                        else -> ScalingType.NONE
                                    }
                                    onUpdateScheduleSettings(
                                        scheduleSettings.copy(
                                            homeScalingType = scalingType,
                                            lockScalingType = scalingType
                                        )
                                    )
                                },
                                selected = index == selectedScalingIndex
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Effects Section Header
            Text(
                text = "Wallpaper Effects",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Darken/Brightness
            SettingSwitchWithSlider(
                title = R.string.change_brightness,
                description = R.string.change_the_image_brightness,
                checked = scheduleSettings.homeEffects.enableDarken,
                onCheckedChange = { enabled ->
                    onUpdateScheduleSettings(
                        scheduleSettings.copy(
                            homeEffects = scheduleSettings.homeEffects.copy(enableDarken = enabled),
                            lockEffects = scheduleSettings.lockEffects.copy(enableDarken = enabled)
                        )
                    )
                },
                bothEnabled = homeEnabled && lockEnabled,
                homePercentage = scheduleSettings.homeEffects.darkenPercentage,
                lockPercentage = scheduleSettings.lockEffects.darkenPercentage,
                onPercentageChange = { homePercent, lockPercent ->
                    onUpdateScheduleSettings(
                        scheduleSettings.copy(
                            homeEffects = scheduleSettings.homeEffects.copy(darkenPercentage = homePercent),
                            lockEffects = scheduleSettings.lockEffects.copy(darkenPercentage = lockPercent)
                        )
                    )
                }
            )

            // Blur
            SettingSwitchWithSlider(
                title = R.string.change_blur,
                description = R.string.add_blur_to_the_image,
                checked = scheduleSettings.homeEffects.enableBlur,
                onCheckedChange = { enabled ->
                    onUpdateScheduleSettings(
                        scheduleSettings.copy(
                            homeEffects = scheduleSettings.homeEffects.copy(enableBlur = enabled),
                            lockEffects = scheduleSettings.lockEffects.copy(enableBlur = enabled)
                        )
                    )
                },
                bothEnabled = homeEnabled && lockEnabled,
                homePercentage = scheduleSettings.homeEffects.blurPercentage,
                lockPercentage = scheduleSettings.lockEffects.blurPercentage,
                onPercentageChange = { homePercent, lockPercent ->
                    onUpdateScheduleSettings(
                        scheduleSettings.copy(
                            homeEffects = scheduleSettings.homeEffects.copy(blurPercentage = homePercent),
                            lockEffects = scheduleSettings.lockEffects.copy(blurPercentage = lockPercent)
                        )
                    )
                }
            )

            // Vignette
            SettingSwitchWithSlider(
                title = R.string.change_vignette,
                description = R.string.darken_the_edges_of_the_image,
                checked = scheduleSettings.homeEffects.enableVignette,
                onCheckedChange = { enabled ->
                    onUpdateScheduleSettings(
                        scheduleSettings.copy(
                            homeEffects = scheduleSettings.homeEffects.copy(enableVignette = enabled),
                            lockEffects = scheduleSettings.lockEffects.copy(enableVignette = enabled)
                        )
                    )
                },
                bothEnabled = homeEnabled && lockEnabled,
                homePercentage = scheduleSettings.homeEffects.vignettePercentage,
                lockPercentage = scheduleSettings.lockEffects.vignettePercentage,
                onPercentageChange = { homePercent, lockPercent ->
                    onUpdateScheduleSettings(
                        scheduleSettings.copy(
                            homeEffects = scheduleSettings.homeEffects.copy(vignettePercentage = homePercent),
                            lockEffects = scheduleSettings.lockEffects.copy(vignettePercentage = lockPercent)
                        )
                    )
                }
            )

            // Grayscale
            SettingSwitch(
                title = R.string.gray_filter,
                description = R.string.make_the_colors_grayscale,
                checked = scheduleSettings.homeEffects.enableGrayscale,
                onCheckedChange = { enabled ->
                    onUpdateScheduleSettings(
                        scheduleSettings.copy(
                            homeEffects = scheduleSettings.homeEffects.copy(enableGrayscale = enabled),
                            lockEffects = scheduleSettings.lockEffects.copy(enableGrayscale = enabled)
                        )
                    )
                }
            )

            // Shuffle
            SettingSwitch(
                title = R.string.shuffle,
                description = R.string.randomly_shuffle_the_wallpapers,
                checked = scheduleSettings.shuffleEnabled,
                onCheckedChange = { enabled ->
                    onUpdateScheduleSettings(scheduleSettings.copy(shuffleEnabled = enabled))
                }
            )
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
