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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * Time interval picker with number input and unit selector
 */
@Composable
fun TimeIntervalPicker(
    title: String,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Convert minutes to the best unit
    val (value, unit) = remember(minutes) {
        when {
            minutes >= 1440 && minutes % 1440 == 0 -> Pair(minutes / 1440, 2) // Days
            minutes >= 60 && minutes % 60 == 0 -> Pair(minutes / 60, 1) // Hours
            else -> Pair(minutes, 0) // Minutes
        }
    }

    var inputValue by remember(minutes) { mutableStateOf(value.toString()) }
    var selectedUnit by remember(minutes) { mutableIntStateOf(unit) }

    val unitLabels = listOf("Min", "Hour", "Day")
    val unitMultipliers = listOf(1, 60, 1440)

    Surface(
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = 8.dp, vertical = 4.dp))
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            inputValue = newValue
                            val numValue = newValue.toIntOrNull() ?: 0
                            val totalMinutes = numValue * unitMultipliers[selectedUnit]
                            // Clamp between 15 minutes and 30 days
                            val clampedMinutes = min(max(totalMinutes, 15), 43200)
                            onMinutesChange(clampedMinutes)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.width(8.dp))

                SingleChoiceSegmentedButtonRow {
                    unitLabels.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = unitLabels.size
                            ),
                            onClick = {
                                selectedUnit = index
                                val currentValue = inputValue.toIntOrNull() ?: 1
                                val totalMinutes = currentValue * unitMultipliers[index]
                                // Clamp between 15 minutes and 30 days
                                val clampedMinutes = min(max(totalMinutes, 15), 43200)
                                onMinutesChange(clampedMinutes)
                            },
                            selected = index == selectedUnit
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            Text(
                text = "Interval: ${formatInterval(minutes)}",
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
            val remainingHours = (minutes % 1440) / 60
            if (remainingHours == 0) "$days day${if (days > 1) "s" else ""}"
            else "$days day${if (days > 1) "s" else ""} $remainingHours hour${if (remainingHours > 1) "s" else ""}"
        }
    }
}
