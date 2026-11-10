package com.anthonyla.paperize.presentation.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.util.BatteryOptimizationUtil.isIgnoringBatteryOptimizations
import com.anthonyla.paperize.core.util.BatteryOptimizationUtil.requestIgnoreBatteryOptimizations
import com.anthonyla.paperize.presentation.common.components.BackTopAppBar
import com.anthonyla.paperize.presentation.common.components.ConfirmDialog
import com.anthonyla.paperize.presentation.common.components.SettingSwitchItem
import com.anthonyla.paperize.presentation.common.components.SettingsCard
import com.anthonyla.paperize.presentation.common.theme.AppSpacing

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isResetting by viewModel.isResetting.collectAsStateWithLifecycle()
    val resetFailed by viewModel.resetFailed.collectAsStateWithLifecycle()
    val wallpaperMode by viewModel.wallpaperMode.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    var pendingMode by remember { mutableStateOf<WallpaperMode?>(null) }
    val context = LocalContext.current

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(isIgnoringBatteryOptimizations(context))
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
    }

    Scaffold(
        topBar = {
            BackTopAppBar(title = stringResource(R.string.settings_screen), onBack = onNavigateBack)
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.large)
        ) {
            ResetStatus(resetFailed = resetFailed, isResetting = isResetting)
            Spacer(modifier = Modifier.height(AppSpacing.large))

            SectionHeader(
                icon = Icons.Filled.Wallpaper,
                title = stringResource(R.string.wallpaper_mode_setting)
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.current_mode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                        Text(
                            text = stringResource(wallpaperMode.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    FilledTonalButton(
                        enabled = !isResetting,
                        onClick = {
                            pendingMode = when (wallpaperMode) {
                                WallpaperMode.STATIC -> WallpaperMode.LIVE
                                WallpaperMode.LIVE -> WallpaperMode.STATIC
                            }
                        }
                    ) {
                        Text(stringResource(R.string.switch_button))
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.small))

                Text(
                    text = stringResource(R.string.mode_switch_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

            SectionHeader(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.appearance)
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            SettingSwitchItem(
                title = stringResource(R.string.dark_mode),
                description = stringResource(R.string.easier_on_the_eyes),
                checked = appSettings?.darkMode ?: isSystemInDarkTheme(),
                onCheckedChange = { viewModel.updateDarkMode(it) }
            )

            Spacer(modifier = Modifier.height(AppSpacing.extraSmall))

            SettingSwitchItem(
                title = stringResource(R.string.dynamic_theming),
                description = stringResource(R.string.material_you),
                checked = appSettings?.dynamicTheming ?: false,
                onCheckedChange = { viewModel.updateDynamicTheming(it) }
            )

            Spacer(modifier = Modifier.height(AppSpacing.extraSmall))

            SettingSwitchItem(
                title = stringResource(R.string.animation),
                description = stringResource(R.string.increase_visual_appeal),
                checked = appSettings?.animate ?: true,
                onCheckedChange = { viewModel.updateAnimate(it) }
            )

            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

            SectionHeader(
                icon = Icons.Default.Build,
                title = stringResource(R.string.reliability)
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.battery_optimization),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.extraSmall))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isIgnoringBatteryOptimizations)
                                    Icons.Default.BatteryFull else Icons.Default.BatteryAlert,
                                contentDescription = null,
                                tint = if (isIgnoringBatteryOptimizations)
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isIgnoringBatteryOptimizations)
                                    stringResource(R.string.ignoring)
                                else
                                    stringResource(R.string.optimizing),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isIgnoringBatteryOptimizations)
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (!isIgnoringBatteryOptimizations) {
                        Button(
                            onClick = {
                                requestIgnoreBatteryOptimizations(context)
                            }
                        ) {
                            Text(stringResource(R.string.check_status))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.small))

                Text(
                    text = stringResource(R.string.battery_optimization_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

            SectionHeader(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.about)
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            SettingsCard(onClick = onNavigateToPrivacy) {
                Text(
                    text = stringResource(R.string.privacy_policy),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                Text(
                    text = stringResource(R.string.click_here_to_view_our_privacy_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.large))

            FilledTonalButton(
                enabled = !isResetting,
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(
                    text = stringResource(R.string.reset_all_data),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.large))
        }

        if (showResetDialog) {
            ConfirmDialog(
                title = stringResource(R.string.reset_to_default),
                text = stringResource(R.string.are_you_sure_you_want_to_reset_all_settings_and_data_to_default),
                onConfirm = {
                    viewModel.resetAllData()
                    showResetDialog = false
                },
                onDismiss = { showResetDialog = false }
            )
        }

        pendingMode?.let { mode ->
            ConfirmDialog(
                title = stringResource(R.string.switch_mode_dialog_title, stringResource(mode.labelRes)),
                text = stringResource(R.string.switch_mode_dialog_description),
                onConfirm = {
                    viewModel.switchWallpaperMode(mode)
                    pendingMode = null
                },
                onDismiss = { pendingMode = null },
                confirmLabel = stringResource(R.string.switch_mode_button),
                confirmColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** Reset progress and failure, shared with onboarding where the same reset picks the first mode. */
@Composable
fun ResetStatus(resetFailed: Boolean, isResetting: Boolean) {
    if (resetFailed) Text(stringResource(R.string.reset_failed), color = MaterialTheme.colorScheme.error)
    if (isResetting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
}

@get:StringRes
private val WallpaperMode.labelRes: Int
    get() = when (this) {
        WallpaperMode.STATIC -> R.string.mode_static
        WallpaperMode.LIVE -> R.string.mode_live_wallpaper
    }

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppSpacing.large,
                vertical = AppSpacing.medium
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
