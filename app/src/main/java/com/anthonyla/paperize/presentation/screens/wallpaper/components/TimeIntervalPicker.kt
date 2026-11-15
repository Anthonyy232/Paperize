package com.anthonyla.paperize.presentation.screens.wallpaper.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.presentation.common.components.SettingsRow
import com.anthonyla.paperize.presentation.common.theme.AppSpacing
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay

@Composable
fun TimeIntervalPicker(
    title: String,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    minimumMinutes: Int = Constants.MIN_INTERVAL_MINUTES,
    note: String? = null
) {
    val (initialDays, initialHours, initialMins) = splitInterval(minutes)

    var dayInput by remember(minutes) { mutableStateOf(initialDays.toString()) }
    var hourInput by remember(minutes) { mutableStateOf(initialHours.toString()) }
    var minuteInput by remember(minutes) { mutableStateOf(initialMins.toString()) }

    var edited by remember(minutes) { mutableStateOf(false) }
    val onChange by rememberUpdatedState(onMinutesChange)
    LaunchedEffect(dayInput, hourInput, minuteInput, edited, minimumMinutes) {
        if (edited) {
            delay(Constants.DEBOUNCE_DELAY_MS)
            val total = (dayInput.toIntOrNull() ?: 0).days +
                (hourInput.toIntOrNull() ?: 0).hours + (minuteInput.toIntOrNull() ?: 0).minutes
            onChange(total.inWholeMinutes.toInt().coerceIn(minimumMinutes, Constants.MAX_INTERVAL_MINUTES))
        }
    }

    SettingsRow(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberField(dayInput, Constants.MAX_DAYS_INPUT_LENGTH, R.string.days_txt) {
                dayInput = it
                edited = true
            }
            NumberField(hourInput, Constants.MAX_HOURS_MINUTES_INPUT_LENGTH, R.string.hours_txt) {
                hourInput = it
                edited = true
            }
            NumberField(minuteInput, Constants.MAX_HOURS_MINUTES_INPUT_LENGTH, R.string.mins) {
                minuteInput = it
                edited = true
            }
        }

        Text(
            text = stringResource(R.string.total_interval, formatIntervalComposable(minutes)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun formatIntervalComposable(minutes: Int): String {
    val (d, h, m) = splitInterval(minutes)
    return listOfNotNull(
        if (d > 0) pluralStringResource(R.plurals.time_unit_days, d, d) else null,
        if (h > 0) pluralStringResource(R.plurals.time_unit_hours, h, h) else null,
        if (m > 0 || d == 0 && h == 0) stringResource(R.string.time_unit_min, m) else null
    ).joinToString(" ")
}

private fun splitInterval(totalMinutes: Int): Triple<Int, Int, Int> =
    totalMinutes.minutes.toComponents { days, hours, minutes, _, _ -> Triple(days.toInt(), hours, minutes) }

@Composable
private fun RowScope.NumberField(value: String, maxLength: Int, @StringRes label: Int, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all(Char::isDigit) && it.length <= maxLength) onValueChange(it) },
        label = { Text(stringResource(label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.weight(1f),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}
