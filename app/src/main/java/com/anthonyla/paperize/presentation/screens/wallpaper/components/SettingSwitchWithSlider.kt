package com.anthonyla.paperize.presentation.screens.wallpaper.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import kotlin.math.roundToInt

/**
 * Setting switch with percentage sliders for home and lock screens
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

    Surface(
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp))
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with title and switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W500
                    )
                    if (!checked) {
                        Text(
                            text = stringResource(description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.lock_screen),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${lockValue.roundToInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = lockValue,
                            onValueChange = {
                                lockValue = it
                                onPercentageChange(homeValue.roundToInt(), it.roundToInt())
                            },
                            valueRange = 0f..100f,
                            steps = 99
                        )
                    }

                    // Home screen slider
                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.home),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${homeValue.roundToInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = homeValue,
                            onValueChange = {
                                homeValue = it
                                onPercentageChange(it.roundToInt(), lockValue.roundToInt())
                            },
                            valueRange = 0f..100f,
                            steps = 99
                        )
                    }
                } else {
                    // Single slider (for whichever screen is enabled)
                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(description),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${homeValue.roundToInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = homeValue,
                            onValueChange = {
                                homeValue = it
                                onPercentageChange(it.roundToInt(), it.roundToInt())
                            },
                            valueRange = 0f..100f,
                            steps = 99
                        )
                    }
                }
            }
        }
    }
}
