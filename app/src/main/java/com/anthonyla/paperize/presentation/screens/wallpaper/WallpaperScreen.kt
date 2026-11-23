package com.anthonyla.paperize.presentation.screens.wallpaper

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.MultiChoiceSegmentedButtonRowScope
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.model.WallpaperEffects
import com.anthonyla.paperize.presentation.common.components.AlbumCover
import com.anthonyla.paperize.presentation.common.components.SettingSwitchItem
import com.anthonyla.paperize.presentation.common.components.SettingText
import com.anthonyla.paperize.presentation.common.components.SettingsGroup
import com.anthonyla.paperize.presentation.common.components.SettingsRow
import com.anthonyla.paperize.presentation.common.theme.AppIconSizes
import com.anthonyla.paperize.presentation.common.theme.AppSpacing
import com.anthonyla.paperize.presentation.screens.wallpaper.components.AlbumSelectionBottomSheet
import com.anthonyla.paperize.presentation.screens.wallpaper.components.CurrentWallpaperPreview
import com.anthonyla.paperize.presentation.screens.wallpaper.components.SettingSwitchWithSlider
import com.anthonyla.paperize.presentation.screens.wallpaper.components.TimeIntervalPicker

@Composable
fun WallpaperScreen(
    albums: List<AlbumSummary>,
    persistedScheduleSettings: ScheduleSettings,
    animate: Boolean,
    wallpaperMode: WallpaperMode,
    onToggleChanger: (Boolean) -> Unit,
    onSelectAlbum: (ScreenType, AlbumSummary?) -> Unit,
    onUpdateScheduleSettings: (ScheduleSettings) -> Unit,
    onUpdateScheduleSettingsDebounced: (ScheduleSettings) -> Unit,
    onChangeWallpaperNow: () -> Unit,
    homeWallpaper: Wallpaper?,
    lockWallpaper: Wallpaper?,
    onRemoveWallpaper: (Wallpaper) -> Unit,
    modifier: Modifier = Modifier
) {
    var albumPickerScreen by rememberSaveable { mutableStateOf<ScreenType?>(null) }
    var showEmptyAlbumWarning by rememberSaveable { mutableStateOf(false) }
    var scheduleSettings by remember { mutableStateOf(persistedScheduleSettings) }

    // Keep an immediate local draft so a slider value waiting for the ViewModel debounce
    // is included in a switch or other setting changed before that debounce expires.
    LaunchedEffect(persistedScheduleSettings) {
        scheduleSettings = persistedScheduleSettings
    }

    fun updateSettingsDebounced(newSettings: ScheduleSettings) {
        scheduleSettings = newSettings
        onUpdateScheduleSettingsDebounced(newSettings)
    }

    fun updateSettingsImmediate(newSettings: ScheduleSettings) {
        scheduleSettings = newSettings
        onUpdateScheduleSettings(newSettings)
    }

    val homeEnabled = scheduleSettings.homeEnabled
    val lockEnabled = scheduleSettings.lockEnabled

    val primaryEffects = when {
        wallpaperMode == WallpaperMode.LIVE -> scheduleSettings.liveEffects
        homeEnabled -> scheduleSettings.homeEffects
        else -> scheduleSettings.lockEffects
    }
    val bothEnabled = wallpaperMode == WallpaperMode.STATIC && homeEnabled && lockEnabled

    fun updateEffects(
        home: (WallpaperEffects) -> WallpaperEffects,
        lock: (WallpaperEffects) -> WallpaperEffects = home,
        debounced: Boolean = false
    ) {
        val updated = if (wallpaperMode == WallpaperMode.LIVE) {
            scheduleSettings.copy(liveEffects = home(scheduleSettings.liveEffects))
        } else {
            scheduleSettings.copy(
                homeEffects = if (homeEnabled) home(scheduleSettings.homeEffects) else scheduleSettings.homeEffects,
                lockEffects = if (lockEnabled) lock(scheduleSettings.lockEffects) else scheduleSettings.lockEffects
            )
        }
        if (debounced) updateSettingsDebounced(updated) else updateSettingsImmediate(updated)
    }

    val scalingOptions = listOf(
        ScalingType.FILL to stringResource(R.string.fill),
        ScalingType.FIT to stringResource(R.string.fit),
        ScalingType.STRETCH to stringResource(R.string.stretch),
        ScalingType.NONE to stringResource(R.string.none)
    )
    val selectedScaling = if (wallpaperMode == WallpaperMode.LIVE) scheduleSettings.liveScalingType else scheduleSettings.homeScalingType

    val hasAlbumSelected = scheduleSettings.activeScreens(wallpaperMode).isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.large)
    ) {
        if (wallpaperMode == WallpaperMode.STATIC) {
            CurrentWallpaperPreview(
                homeWallpaper = homeWallpaper,
                lockWallpaper = lockWallpaper,
                animate = animate,
                onRemove = onRemoveWallpaper
            )
            MultiChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ScreenToggle(
                    index = 0, title = stringResource(R.string.lock), icon = Icons.Default.Lock, enabled = lockEnabled,
                    onToggle = { updateSettingsImmediate(scheduleSettings.copy(lockEnabled = it)) }
                )
                ScreenToggle(
                    index = 1, title = stringResource(R.string.home), icon = Icons.Default.Home, enabled = homeEnabled,
                    onToggle = { updateSettingsImmediate(scheduleSettings.copy(homeEnabled = it)) }
                )
            }
        }

        SettingsGroup {
            if (wallpaperMode == WallpaperMode.STATIC) {
                if (lockEnabled) AlbumSelector(
                    albumId = scheduleSettings.lockAlbumId, albums = albums,
                    label = stringResource(R.string.lock_album_label),
                    onClick = { albumPickerScreen = ScreenType.LOCK }
                )
                if (homeEnabled) AlbumSelector(
                    albumId = scheduleSettings.homeAlbumId, albums = albums,
                    label = stringResource(R.string.home_album_label),
                    onClick = { albumPickerScreen = ScreenType.HOME }
                )
            } else {
                AlbumSelector(
                    albumId = scheduleSettings.liveAlbumId, albums = albums,
                    label = stringResource(R.string.currently_selected_album),
                    onClick = { albumPickerScreen = ScreenType.LIVE }
                )
            }
            if (scheduleSettings.hasRequiredAlbums(wallpaperMode)) {
                SettingSwitchItem(
                    title = stringResource(R.string.wallpaper_changer),
                    description = stringResource(R.string.wallpaper_changer_description),
                    checked = scheduleSettings.enableChanger,
                    onCheckedChange = onToggleChanger
                )
            }
            if (scheduleSettings.enableChanger && bothEnabled) {
                SettingSwitchItem(
                    title = stringResource(R.string.individual_scheduling),
                    description = stringResource(R.string.show_interval_sliders),
                    checked = scheduleSettings.separateSchedules,
                    onCheckedChange = { enabled ->
                        updateSettingsImmediate(scheduleSettings.copy(separateSchedules = enabled))
                    }
                )
            }
            if (hasAlbumSelected) {
                if (wallpaperMode == WallpaperMode.STATIC) {
                    if (!(scheduleSettings.separateSchedules && bothEnabled)) {
                        TimeIntervalPicker(
                            title = stringResource(R.string.interval_text),
                            minutes = scheduleSettings.homeIntervalMinutes,
                            onMinutesChange = { minutes ->
                                updateSettingsImmediate(
                                    scheduleSettings.copy(homeIntervalMinutes = minutes, lockIntervalMinutes = minutes)
                                )
                            }
                        )
                    } else {
                        TimeIntervalPicker(
                            title = stringResource(R.string.lock_screen_btn),
                            minutes = scheduleSettings.lockIntervalMinutes,
                            onMinutesChange = { minutes ->
                                updateSettingsImmediate(
                                    scheduleSettings.copy(lockIntervalMinutes = minutes)
                                )
                            }
                        )
                        TimeIntervalPicker(
                            title = stringResource(R.string.home_screen_btn),
                            minutes = scheduleSettings.homeIntervalMinutes,
                            onMinutesChange = { minutes ->
                                updateSettingsImmediate(
                                    scheduleSettings.copy(homeIntervalMinutes = minutes)
                                )
                            }
                        )
                    }
                } else {
                    TimeIntervalPicker(
                        title = stringResource(R.string.interval_text),
                        minutes = scheduleSettings.liveIntervalMinutes,
                        minimumMinutes = Constants.MIN_LIVE_INTERVAL_MINUTES,
                        note = stringResource(R.string.live_short_interval_note),
                        onMinutesChange = { minutes ->
                            updateSettingsImmediate(
                                scheduleSettings.copy(liveIntervalMinutes = minutes)
                            )
                        }
                    )
                }
            }
            SettingSwitchItem(
                title = stringResource(R.string.shuffle),
                description = if (scheduleSettings.shuffleEnabled) null else stringResource(R.string.randomly_shuffle_the_wallpapers),
                checked = scheduleSettings.shuffleEnabled,
                onCheckedChange = { enabled ->
                    updateSettingsImmediate(scheduleSettings.copy(shuffleEnabled = enabled))
                }
            )
        }

        if (hasAlbumSelected) {
            Button(onClick = onChangeWallpaperNow, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.change_wallpaper_now))
            }
        }

        SettingsGroup(title = stringResource(R.string.scaling)) {
            SettingsRow {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    scalingOptions.forEachIndexed { index, (scalingType, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = scalingOptions.size
                            ),
                            onClick = {
                                updateSettingsImmediate(
                                    if (wallpaperMode == WallpaperMode.LIVE) {
                                        scheduleSettings.copy(liveScalingType = scalingType)
                                    } else {
                                        scheduleSettings.copy(
                                            homeScalingType = scalingType,
                                            lockScalingType = scalingType
                                        )
                                    }
                                )
                            },
                            selected = scalingType == selectedScaling
                        ) {
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (wallpaperMode == WallpaperMode.STATIC) {
                SettingSwitchItem(
                    title = stringResource(R.string.horizontal_wallpaper_scrolling),
                    description = stringResource(R.string.horizontal_wallpaper_scrolling_description),
                    checked = scheduleSettings.homeScrollingEnabled,
                    onCheckedChange = { enabled ->
                        updateSettingsImmediate(
                            scheduleSettings.copy(homeScrollingEnabled = enabled)
                        )
                    }
                )
            }
        }

        SettingsGroup(title = stringResource(R.string.visual_effects)) {
            imageEffects.forEach { effect ->
                SettingSwitchWithSlider(
                    title = effect.title,
                    description = effect.description,
                    checked = effect.enabled(primaryEffects),
                    onCheckedChange = { enabled -> updateEffects({ effect.withEnabled(it, enabled) }) },
                    homeChecked = effect.enabled(scheduleSettings.homeEffects),
                    lockChecked = effect.enabled(scheduleSettings.lockEffects),
                    onHomeCheckedChange = { enabled ->
                        updateSettingsImmediate(scheduleSettings.copy(homeEffects = effect.withEnabled(scheduleSettings.homeEffects, enabled)))
                    },
                    onLockCheckedChange = { enabled ->
                        updateSettingsImmediate(scheduleSettings.copy(lockEffects = effect.withEnabled(scheduleSettings.lockEffects, enabled)))
                    },
                    bothEnabled = bothEnabled,
                    homePercentage = effect.percentage(primaryEffects),
                    lockPercentage = effect.percentage(scheduleSettings.lockEffects),
                    onPercentageChange = { home, lock ->
                        updateEffects({ effect.withPercentage(it, home) }, { effect.withPercentage(it, lock) }, debounced = true)
                    }
                )
            }
            SettingSwitchItem(
                title = stringResource(R.string.adaptive_brightness),
                description = if (scheduleSettings.adaptiveBrightness) null else stringResource(R.string.adjust_brightness_based_on_mode),
                checked = scheduleSettings.adaptiveBrightness,
                onCheckedChange = { enabled ->
                    updateSettingsImmediate(scheduleSettings.copy(adaptiveBrightness = enabled))
                }
            )
        }
        if (wallpaperMode == WallpaperMode.LIVE) {
            SettingsGroup(title = stringResource(R.string.interactive_effects)) {
                SettingSwitchItem(
                    title = stringResource(R.string.double_tap_to_change),
                    description = if (scheduleSettings.liveEffects.enableDoubleTap) null else stringResource(R.string.double_tap_wallpaper_to_change_it),
                    checked = scheduleSettings.liveEffects.enableDoubleTap,
                    onCheckedChange = { enabled ->
                        updateSettingsImmediate(
                            scheduleSettings.copy(
                                liveEffects = scheduleSettings.liveEffects.copy(enableDoubleTap = enabled)
                            )
                        )
                    }
                )
                SettingSwitchItem(
                    title = stringResource(R.string.change_on_screen_off),
                    description = if (scheduleSettings.liveEffects.enableChangeOnScreenOff) null else stringResource(R.string.change_wallpaper_when_screen_turns_off),
                    checked = scheduleSettings.liveEffects.enableChangeOnScreenOff,
                    onCheckedChange = { enabled ->
                        updateSettingsImmediate(
                            scheduleSettings.copy(
                                liveEffects = scheduleSettings.liveEffects.copy(enableChangeOnScreenOff = enabled)
                            )
                        )
                    }
                )
                SettingSwitchWithSlider(
                    title = R.string.parallax_effect,
                    description = R.string.wallpaper_moves_with_screen_scroll,
                    checked = scheduleSettings.liveEffects.enableParallax,
                    onCheckedChange = { enabled ->
                        updateSettingsImmediate(
                            scheduleSettings.copy(
                                liveEffects = scheduleSettings.liveEffects.copy(enableParallax = enabled)
                            )
                        )
                    },
                    bothEnabled = false, // Never separate in Live Mode
                    homePercentage = scheduleSettings.liveEffects.parallaxIntensity,
                    lockPercentage = 0,
                    onPercentageChange = { homePercent, _ ->
                        updateSettingsDebounced(
                            scheduleSettings.copy(
                                liveEffects = scheduleSettings.liveEffects.copy(parallaxIntensity = homePercent)
                            )
                        )
                    }
                )
            }
        }
    }

    albumPickerScreen?.let { selection ->
        val selectedId = scheduleSettings.albumId(selection)
        AlbumSelectionBottomSheet(
            albums = albums,
            selectedAlbumId = selectedId,
            onAlbumSelect = { album ->
                when {
                    album.id == selectedId -> onSelectAlbum(selection, null)
                    album.wallpaperCount == 0 -> showEmptyAlbumWarning = true
                    else -> onSelectAlbum(selection, album)
                }
            },
            onDismiss = { albumPickerScreen = null }
        )
    }

    if (showEmptyAlbumWarning) {
        AlertDialog(
            onDismissRequest = { showEmptyAlbumWarning = false },
            title = { Text(stringResource(R.string.empty_album)) },
            text = { Text(stringResource(R.string.empty_album_message)) },
            confirmButton = {
                TextButton(onClick = { showEmptyAlbumWarning = false }) {
                    Text(
                        text = stringResource(R.string.ok),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }
}

@Composable
private fun MultiChoiceSegmentedButtonRowScope.ScreenToggle(
    index: Int,
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    SegmentedButton(
        checked = enabled,
        onCheckedChange = onToggle,
        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
        icon = {
            SegmentedButtonDefaults.Icon(active = enabled) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(SegmentedButtonDefaults.IconSize))
            }
        }
    ) {
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AlbumSelector(albumId: String?, albums: List<AlbumSummary>, label: String, onClick: () -> Unit) {
    val album = albums.find { it.id == albumId }
    SettingsRow(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (album != null) {
                AlbumCover(
                    album = album,
                    thumbnailSize = Constants.LIST_THUMBNAIL_SIZE,
                    placeholderTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(AppIconSizes.large)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            }
            SettingText(
                title = album?.name ?: stringResource(if (albumId == null) R.string.no_album_selected else R.string.loading_placeholder),
                description = label,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One adjustable image effect: a toggle plus a percentage within [WallpaperEffects]. */
private class ImageEffect(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val enabled: (WallpaperEffects) -> Boolean,
    val percentage: (WallpaperEffects) -> Int,
    val withEnabled: (WallpaperEffects, Boolean) -> WallpaperEffects,
    val withPercentage: (WallpaperEffects, Int) -> WallpaperEffects
)

private val imageEffects = listOf(
    ImageEffect(
        R.string.change_brightness, R.string.change_the_image_brightness,
        { it.enableDarken }, { it.darkenPercentage },
        { e, v -> e.copy(enableDarken = v) }, { e, v -> e.copy(darkenPercentage = v) }
    ),
    ImageEffect(
        R.string.change_blur, R.string.add_blur_to_the_image,
        { it.enableBlur }, { it.blurPercentage },
        { e, v -> e.copy(enableBlur = v) }, { e, v -> e.copy(blurPercentage = v) }
    ),
    ImageEffect(
        R.string.change_vignette, R.string.darken_the_edges_of_the_image,
        { it.enableVignette }, { it.vignettePercentage },
        { e, v -> e.copy(enableVignette = v) }, { e, v -> e.copy(vignettePercentage = v) }
    ),
    ImageEffect(
        R.string.gray_filter, R.string.make_the_colors_grayscale,
        { it.enableGrayscale }, { it.grayscalePercentage },
        { e, v -> e.copy(enableGrayscale = v) }, { e, v -> e.copy(grayscalePercentage = v) }
    )
)
