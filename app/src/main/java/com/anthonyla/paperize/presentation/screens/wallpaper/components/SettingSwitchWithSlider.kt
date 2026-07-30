package com.anthonyla.paperize.presentation.screens.wallpaper.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.presentation.theme.AppSpacing
import kotlin.math.roundToInt

/**
 * Effect control with either one switch/slider or independent HOME and LOCK controls.
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
    modifier: Modifier = Modifier,
    homeChecked: Boolean = checked,
    lockChecked: Boolean = checked,
    onHomeCheckedChange: (Boolean) -> Unit = onCheckedChange,
    onLockCheckedChange: (Boolean) -> Unit = onCheckedChange
) {
    var homeValue by remember(homePercentage) {
        mutableFloatStateOf(homePercentage.toFloat())
    }
    var lockValue by remember(lockPercentage) {
        mutableFloatStateOf(lockPercentage.toFloat())
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall)),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(AppSpacing.large),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.large)
        ) {
            if (bothEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)) {
                    Text(
                        text = stringResource(title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ScreenEffectControl(
                    label = R.string.home,
                    checked = homeChecked,
                    onCheckedChange = onHomeCheckedChange,
                    value = homeValue,
                    onValueChange = {
                        homeValue = it
                        onPercentageChange(it.roundToInt(), lockValue.roundToInt())
                    }
                )
                ScreenEffectControl(
                    label = R.string.lock,
                    checked = lockChecked,
                    onCheckedChange = onLockCheckedChange,
                    value = lockValue,
                    onValueChange = {
                        lockValue = it
                        onPercentageChange(homeValue.roundToInt(), it.roundToInt())
                    }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {},
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

                if (checked) {
                    PercentageSlider(
                        label = description,
                        value = homeValue,
                        onValueChange = {
                            homeValue = it
                            onPercentageChange(it.roundToInt(), it.roundToInt())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenEffectControl(
    @StringRes label: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = AppSpacing.small),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
        if (checked) {
            PercentageSlider(
                label = label,
                value = value,
                onValueChange = onValueChange
            )
        }
    }
}

@Composable
private fun PercentageSlider(
    @StringRes label: Int,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${value.roundToInt()}%",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = AppSpacing.medium)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = Constants.MIN_EFFECT_PERCENTAGE.toFloat()..
                Constants.MAX_EFFECT_PERCENTAGE.toFloat(),
            steps = Constants.SLIDER_EFFECT_STEPS
        )
    }
}
