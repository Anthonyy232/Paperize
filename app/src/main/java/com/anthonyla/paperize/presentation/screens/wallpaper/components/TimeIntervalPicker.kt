package com.anthonyla.paperize.presentation.screens.wallpaper.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    // Decompose minutes into days, hours, minutes
    var dayValue by remember { mutableIntStateOf(0) }
    var hourValue by remember { mutableIntStateOf(0) }
    var minuteValue by remember { mutableIntStateOf(15) } // Default 15 minutes

    var dayInput by remember { mutableStateOf("0") }
    var hourInput by remember { mutableStateOf("0") }
    var minuteInput by remember { mutableStateOf("15") }

    // Initialize from incoming minutes value only once
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(minutes) {
        if (!initialized && minutes != 15) {
            // Only decompose if not the default value
            val days = minutes / 1440
            val remainingAfterDays = minutes % 1440
            val hours = remainingAfterDays / 60
            val mins = remainingAfterDays % 60

            dayValue = days
            hourValue = hours
            minuteValue = mins

            dayInput = days.toString()
            hourInput = hours.toString()
            minuteInput = mins.toString()
        }
        initialized = true
    }

    fun updateTotalMinutes() {
        val total = (dayValue * 1440) + (hourValue * 60) + minuteValue
        // Clamp between 15 minutes and 30 days
        val clamped = min(max(total, 15), 43200)
        onMinutesChange(clamped)
    }

    Surface(
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = 8.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W500
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Days input
                OutlinedTextField(
                    value = dayInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 3)) {
                            dayInput = newValue
                            dayValue = newValue.toIntOrNull() ?: 0
                            updateTotalMinutes()
                        }
                    },
                    label = { Text("Days") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                // Hours input
                OutlinedTextField(
                    value = hourInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 2)) {
                            hourInput = newValue
                            hourValue = newValue.toIntOrNull() ?: 0
                            updateTotalMinutes()
                        }
                    },
                    label = { Text("Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                // Minutes input
                OutlinedTextField(
                    value = minuteInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 2)) {
                            minuteInput = newValue
                            minuteValue = newValue.toIntOrNull() ?: 0
                            updateTotalMinutes()
                        }
                    },
                    label = { Text("Mins") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = "Total: ${formatInterval(minutes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatInterval(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes min"
        minutes < 1440 -> {
            val hours = minutes / 60
            val remainingMins = minutes % 60
            if (remainingMins == 0) "$hours hour${if (hours > 1) "s" else ""}"
            else "$hours hour${if (hours > 1) "s" else ""} $remainingMins min"
        }
        else -> {
            val days = minutes / 1440
            val remainingMinutes = minutes % 1440
            val remainingHours = remainingMinutes / 60
            val finalMins = remainingMinutes % 60
            buildString {
                append("$days day${if (days > 1) "s" else ""}")
                if (remainingHours > 0) append(" $remainingHours hour${if (remainingHours > 1) "s" else ""}")
                if (finalMins > 0) append(" $finalMins min")
            }
        }
    }
}
