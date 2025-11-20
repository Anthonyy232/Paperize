package com.anthonyla.paperize.presentation.screens.wallpaper.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.presentation.theme.AppSpacing
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/**
 * Time interval picker with separate inputs for days, hours, and minutes
 */
@Composable
fun TimeIntervalPicker(
    title: String,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Decompose incoming minutes value into days, hours, minutes
    val initialDays = minutes / Constants.MINUTES_PER_DAY
    val remainingAfterDays = minutes % Constants.MINUTES_PER_DAY
    val initialHours = remainingAfterDays / Constants.MINUTES_PER_HOUR
    val initialMins = remainingAfterDays % Constants.MINUTES_PER_HOUR

    // State for user input
    var dayValue by remember(minutes) { mutableIntStateOf(initialDays) }
    var hourValue by remember(minutes) { mutableIntStateOf(initialHours) }
    var minuteValue by remember(minutes) { mutableIntStateOf(initialMins) }

    var dayInput by remember(minutes) { mutableStateOf(initialDays.toString()) }
    var hourInput by remember(minutes) { mutableStateOf(initialHours.toString()) }
    var minuteInput by remember(minutes) { mutableStateOf(initialMins.toString()) }

    // Debounce state - tracks when user last made a change
    var lastChangeTimestamp by remember { mutableStateOf(0L) }

    // Debounced update - only fires after user stops typing
    LaunchedEffect(dayValue, hourValue, minuteValue, lastChangeTimestamp) {
        if (lastChangeTimestamp > 0) {
            delay(Constants.DEBOUNCE_DELAY_MS)
            val total = (dayValue * Constants.MINUTES_PER_DAY) + (hourValue * Constants.MINUTES_PER_HOUR) + minuteValue
            // Clamp between minimum and maximum interval
            val clamped = min(max(total, Constants.MIN_INTERVAL_MINUTES), Constants.MAX_INTERVAL_MINUTES)
            onMinutesChange(clamped)
        }
    }

    fun notifyChange() {
        lastChangeTimestamp = System.currentTimeMillis()
    }

    androidx.compose.material3.Card(
        shape = MaterialTheme.shapes.medium,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall))
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.large),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W500,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Days input
                OutlinedTextField(
                    value = dayInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= Constants.MAX_DAYS_INPUT_LENGTH)) {
                            dayInput = newValue
                            dayValue = newValue.toIntOrNull() ?: 0
                            notifyChange()
                        }
                    },
                    label = { Text(stringResource(R.string.days_txt)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                // Hours input
                OutlinedTextField(
                    value = hourInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= Constants.MAX_HOURS_MINUTES_INPUT_LENGTH)) {
                            hourInput = newValue
                            hourValue = newValue.toIntOrNull() ?: 0
                            notifyChange()
                        }
                    },
                    label = { Text(stringResource(R.string.hours_txt)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                // Minutes input
                OutlinedTextField(
                    value = minuteInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= Constants.MAX_HOURS_MINUTES_INPUT_LENGTH)) {
                            minuteInput = newValue
                            minuteValue = newValue.toIntOrNull() ?: 0
                            notifyChange()
                        }
                    },
                    label = { Text(stringResource(R.string.mins)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = "${stringResource(R.string.total_interval)} ${formatIntervalComposable(minutes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun formatIntervalComposable(minutes: Int): String {
    val minUnit = stringResource(R.string.time_unit_min)
    val hourUnit = pluralStringResource(R.plurals.time_unit_hours, 1)
    val hoursUnit = pluralStringResource(R.plurals.time_unit_hours, 2)
    val dayUnit = pluralStringResource(R.plurals.time_unit_days, 1)
    val daysUnit = pluralStringResource(R.plurals.time_unit_days, 2)

    return when {
        minutes < Constants.MINUTES_PER_HOUR -> "$minutes $minUnit"
        minutes < Constants.MINUTES_PER_DAY -> {
            val hours = minutes / Constants.MINUTES_PER_HOUR
            val remainingMins = minutes % Constants.MINUTES_PER_HOUR
            if (remainingMins == 0) "$hours ${if (hours > 1) hoursUnit else hourUnit}"
            else "$hours ${if (hours > 1) hoursUnit else hourUnit} $remainingMins $minUnit"
        }
        else -> {
            val days = minutes / Constants.MINUTES_PER_DAY
            val remainingMinutes = minutes % Constants.MINUTES_PER_DAY
            val remainingHours = remainingMinutes / Constants.MINUTES_PER_HOUR
            val finalMins = remainingMinutes % Constants.MINUTES_PER_HOUR
            buildString {
                append("$days ${if (days > 1) daysUnit else dayUnit}")
                if (remainingHours > 0) append(" $remainingHours ${if (remainingHours > 1) hoursUnit else hourUnit}")
                if (finalMins > 0) append(" $finalMins $minUnit")
            }
        }
    }
}
