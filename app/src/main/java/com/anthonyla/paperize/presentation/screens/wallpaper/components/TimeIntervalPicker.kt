package com.anthonyla.paperize.presentation.screens.wallpaper.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.presentation.theme.AppSpacing
import kotlinx.coroutines.delay

@Composable
fun TimeIntervalPicker(
    title: String,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minimumMinutes: Int = Constants.MIN_INTERVAL_MINUTES
) {
    val initialDays = minutes / Constants.MINUTES_PER_DAY
    val remainingAfterDays = minutes % Constants.MINUTES_PER_DAY
    val initialHours = remainingAfterDays / Constants.MINUTES_PER_HOUR
    val initialMins = remainingAfterDays % Constants.MINUTES_PER_HOUR

    var dayInput by remember(minutes) { mutableStateOf(initialDays.toString()) }
    var hourInput by remember(minutes) { mutableStateOf(initialHours.toString()) }
    var minuteInput by remember(minutes) { mutableStateOf(initialMins.toString()) }

    var edited by remember(minutes) { mutableStateOf(false) }
    val onChange by rememberUpdatedState(onMinutesChange)
    LaunchedEffect(dayInput, hourInput, minuteInput, edited, minimumMinutes) {
        if (edited) {
            delay(Constants.DEBOUNCE_DELAY_MS)
            val total = (dayInput.toIntOrNull() ?: 0) * Constants.MINUTES_PER_DAY +
                (hourInput.toIntOrNull() ?: 0) * Constants.MINUTES_PER_HOUR + (minuteInput.toIntOrNull() ?: 0)
            onChange(total.coerceIn(minimumMinutes, Constants.MAX_INTERVAL_MINUTES))
        }
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
                OutlinedTextField(
                    value = dayInput,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= Constants.MAX_DAYS_INPUT_LENGTH) {
                            dayInput = newValue
                            edited = true
                        }
                    },
                    label = { Text(stringResource(R.string.days_txt)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                OutlinedTextField(
                    value = hourInput,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= Constants.MAX_HOURS_MINUTES_INPUT_LENGTH) {
                            hourInput = newValue
                            edited = true
                        }
                    },
                    label = { Text(stringResource(R.string.hours_txt)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                OutlinedTextField(
                    value = minuteInput,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= Constants.MAX_HOURS_MINUTES_INPUT_LENGTH) {
                            minuteInput = newValue
                            edited = true
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

    return when {
        minutes < Constants.MINUTES_PER_HOUR -> "$minutes $minUnit"
        minutes < Constants.MINUTES_PER_DAY -> {
            val h = minutes / Constants.MINUTES_PER_HOUR
            val remainingMins = minutes % Constants.MINUTES_PER_HOUR
            val hUnit = pluralStringResource(R.plurals.time_unit_hours, h)
            if (remainingMins == 0) "$h $hUnit"
            else "$h $hUnit $remainingMins $minUnit"
        }
        else -> {
            val d = minutes / Constants.MINUTES_PER_DAY
            val remainingMinutes = minutes % Constants.MINUTES_PER_DAY
            val remainingHours = remainingMinutes / Constants.MINUTES_PER_HOUR
            val finalMins = remainingMinutes % Constants.MINUTES_PER_HOUR
            val dUnit = pluralStringResource(R.plurals.time_unit_days, d)
            buildString {
                append("$d $dUnit")
                if (remainingHours > 0) {
                    val hUnit = pluralStringResource(R.plurals.time_unit_hours, remainingHours)
                    append(" $remainingHours $hUnit")
                }
                if (finalMins > 0) append(" $finalMins $minUnit")
            }
        }
    }
}
