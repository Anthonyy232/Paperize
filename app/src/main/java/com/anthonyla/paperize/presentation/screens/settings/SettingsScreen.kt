package com.anthonyla.paperize.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.common.components.SettingSwitchItem
import com.anthonyla.paperize.presentation.theme.AppSpacing
import com.anthonyla.paperize.core.util.BatteryOptimizationUtil
import com.anthonyla.paperize.core.util.BatteryOptimizationUtil.isIgnoringBatteryOptimizations
import com.anthonyla.paperize.core.util.BatteryOptimizationUtil.requestIgnoreBatteryOptimizations

/**
 * Settings screen with Material 3 Expressive design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val appSettings by viewModel.appSettings.collectAsState()
    val wallpaperMode by viewModel.wallpaperMode.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showModeChangeDialog by remember { mutableStateOf(false) }
    var pendingMode by remember { mutableStateOf<com.anthonyla.paperize.core.WallpaperMode?>(null) }
    val context = LocalContext.current

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(BatteryOptimizationUtil.isIgnoringBatteryOptimizations(context))
    }

    // Refresh battery optimization status when returning to the screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations = BatteryOptimizationUtil.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_screen)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.large)
        ) {
            Spacer(modifier = Modifier.height(AppSpacing.large))

            // Wallpaper Mode Section
            SectionHeader(
                icon = Icons.Filled.Wallpaper,
                title = stringResource(R.string.wallpaper_mode_setting)
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.large)
                ) {
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
                                text = when (wallpaperMode) {
                                    com.anthonyla.paperize.core.WallpaperMode.STATIC -> stringResource(R.string.mode_static)
                                    com.anthonyla.paperize.core.WallpaperMode.LIVE -> stringResource(R.string.mode_live_wallpaper)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                pendingMode = when (wallpaperMode) {
                                    com.anthonyla.paperize.core.WallpaperMode.STATIC -> com.anthonyla.paperize.core.WallpaperMode.LIVE
                                    com.anthonyla.paperize.core.WallpaperMode.LIVE -> com.anthonyla.paperize.core.WallpaperMode.STATIC
                                }
                                showModeChangeDialog = true
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
            }

            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

            // Appearance Section
            SectionHeader(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.appearance)
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            SettingSwitchItem(
                title = stringResource(R.string.dark_mode),
                description = stringResource(R.string.easier_on_the_eyes),
                checked = appSettings?.darkMode ?: false,
                onCheckedChange = { viewModel.updateDarkMode(it) }
            )

            Spacer(modifier = Modifier.height(AppSpacing.extraSmall))

            SettingSwitchItem(
                title = stringResource(R.string.dynamic_theming),
                description = stringResource(R.string.material_you),
                checked = appSettings?.dynamicTheming ?: true,
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

            // Reliability Section
            SectionHeader(
                icon = Icons.Default.Build,
                title = stringResource(R.string.reliability)
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.large)
                ) {
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
                                    BatteryOptimizationUtil.requestIgnoreBatteryOptimizations(context)
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
            }

            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

            // About Section
            SectionHeader(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.about)
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            // Privacy Policy Card with enhanced styling
            Card(
                onClick = onNavigateToPrivacy,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.large)
                ) {
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
            }

            Spacer(modifier = Modifier.height(AppSpacing.large))

            // Reset Data Button with enhanced styling
            FilledTonalButton(
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
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(R.string.reset_to_default)) },
                text = { Text(stringResource(R.string.are_you_sure_you_want_to_reset_all_settings_and_data_to_default)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetAllData()
                            showResetDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (showModeChangeDialog && pendingMode != null) {
            AlertDialog(
                onDismissRequest = {
                    showModeChangeDialog = false
                    pendingMode = null
                },
                title = { 
                    val modeName = if (pendingMode == com.anthonyla.paperize.core.WallpaperMode.LIVE) 
                        stringResource(R.string.mode_live_wallpaper) 
                    else 
                        stringResource(R.string.mode_static)
                    Text(stringResource(R.string.switch_mode_dialog_title, modeName)) 
                },
                text = {
                    Text(stringResource(R.string.switch_mode_dialog_description))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingMode?.let { viewModel.switchWallpaperMode(it) }
                            showModeChangeDialog = false
                            pendingMode = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.switch_mode_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showModeChangeDialog = false
                        pendingMode = null
                    }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

/**
 * Section header with icon and title for better visual hierarchy
 */
@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
