package com.anthonyla.paperize.presentation.screens.wallpaper_mode_selection
import com.anthonyla.paperize.presentation.theme.AppIconSizes
import com.anthonyla.paperize.presentation.components.OnboardingLayout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.presentation.theme.AppSpacing

/**
 * Wallpaper mode selection screen shown during onboarding
 * Allows user to choose between STATIC and LIVE wallpaper modes
 */
@Composable
fun WallpaperModeSelectionScreen(
    onModeSelected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WallpaperModeSelectionViewModel = hiltViewModel()
) {
    var selectedMode by remember { mutableStateOf<WallpaperMode?>(null) }

    OnboardingLayout(
        title = stringResource(R.string.choose_wallpaper_mode),
        modifier = modifier,
        iconContent = {
            Icon(
                imageVector = Icons.Default.Wallpaper,
                contentDescription = null,
                modifier = Modifier.size(AppIconSizes.huge),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Text(
                    text = stringResource(R.string.change_later_in_settings),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(AppSpacing.small))

                // Static Mode Card
                ModeSelectionCard(
                    title = stringResource(R.string.static_mode),
                    description = stringResource(R.string.static_mode_description),
                    icon = Icons.Default.Wallpaper,
                    isSelected = selectedMode == WallpaperMode.STATIC,
                    onClick = { selectedMode = WallpaperMode.STATIC }
                )

                // Live Mode Card
                ModeSelectionCard(
                    title = stringResource(R.string.live_wallpaper_mode),
                    description = stringResource(R.string.live_wallpaper_mode_description),
                    icon = Icons.Default.LiveTv,
                    isSelected = selectedMode == WallpaperMode.LIVE,
                    onClick = { selectedMode = WallpaperMode.LIVE }
                )
            }
        },
        actions = {
            Button(
                onClick = {
                    selectedMode?.let { mode ->
                        viewModel.setWallpaperMode(mode)
                        onModeSelected()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                enabled = selectedMode != null
            ) {
                Text(
                    text = stringResource(R.string.continue_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = AppSpacing.small)
                )
            }
        }
    )
}

@Composable
private fun ModeSelectionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AppIconSizes.large),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(AppSpacing.medium))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(AppSpacing.extraSmall))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(AppSpacing.small))
                RadioButton(
                    selected = true,
                    onClick = onClick
                )
            }
        }
    }
}
