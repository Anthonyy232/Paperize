package com.anthonyla.paperize.presentation.screens.wallpaper.components
import com.anthonyla.paperize.core.constants.Constants

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.theme.AppSpacing
import kotlin.math.roundToInt

/**
 * Setting switch with percentage sliders for home and lock screens
 * Enhanced with Material 3 Expressive design
 */
@Composable
fun SettingSwitchWithSlider(
    @StringRes title: Int,
    @StringRes description: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    bothEnabled: Boolean,
    homePercentage: Int,
    lockPercentage: Int,
    onPercentageChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var homeValue by remember(homePercentage) { mutableFloatStateOf(homePercentage.toFloat()) }
    var lockValue by remember(lockPercentage) { mutableFloatStateOf(lockPercentage.toFloat()) }

    androidx.compose.material3.Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall)),
        shape = MaterialTheme.shapes.medium,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(AppSpacing.large),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.large)
        ) {
            // Header with title and switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)
                ) {
                    Text(
                        text = stringResource(title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!checked) {
                        Text(
                            text = stringResource(description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }

            // Sliders (shown when enabled)
            if (checked) {
                if (bothEnabled) {
                    // Lock screen slider
                    Column(modifier = Modifier.padding(horizontal = AppSpacing.small)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.lock),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${lockValue.roundToInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Slider(
                            value = lockValue,
                            onValueChange = {
                                lockValue = it
                                onPercentageChange(homeValue.roundToInt(), it.roundToInt())
                            },
                            valueRange = Constants.MIN_EFFECT_PERCENTAGE.toFloat()..Constants.MAX_EFFECT_PERCENTAGE.toFloat(),
                            steps = Constants.SLIDER_EFFECT_STEPS
                        )
                    }

                    // Home screen slider
                    Column(modifier = Modifier.padding(horizontal = AppSpacing.small)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.home),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${homeValue.roundToInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Slider(
                            value = homeValue,
                            onValueChange = {
                                homeValue = it
                                onPercentageChange(it.roundToInt(), lockValue.roundToInt())
                            },
                            valueRange = Constants.MIN_EFFECT_PERCENTAGE.toFloat()..Constants.MAX_EFFECT_PERCENTAGE.toFloat(),
                            steps = Constants.SLIDER_EFFECT_STEPS
                        )
                    }
                } else {
                    // Single slider (for whichever screen is enabled)
                    Column(modifier = Modifier.padding(horizontal = AppSpacing.small)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(description),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${homeValue.roundToInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = AppSpacing.medium)
                            )
                        }
                        Slider(
                            value = homeValue,
                            onValueChange = {
                                homeValue = it
                                onPercentageChange(it.roundToInt(), it.roundToInt())
                            },
                            valueRange = Constants.MIN_EFFECT_PERCENTAGE.toFloat()..Constants.MAX_EFFECT_PERCENTAGE.toFloat(),
                            steps = Constants.SLIDER_EFFECT_STEPS
                        )
                    }
                }
            }
        }
    }
}
