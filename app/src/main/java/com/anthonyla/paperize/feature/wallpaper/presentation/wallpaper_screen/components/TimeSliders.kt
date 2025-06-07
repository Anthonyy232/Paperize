package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.SettingsConstants.WALLPAPER_CHANGE_INTERVAL_MIN
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * TimeSliders is a composable that displays sliders for the user to select the time interval for changing wallpaper
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSliders(
    homeInterval: Int,
    lockInterval: Int,
    showInterval: Boolean,
    animate: Boolean,
    onShowIntervalChange: (Boolean) -> Unit,
    scheduleSeparately: Boolean,
    onHomeIntervalChange: (Int, Int, Int) -> Unit,
    onLockIntervalChange: (Int, Int, Int) -> Unit,
    lockEnabled: Boolean,
    homeEnabled: Boolean,
    startingTime: Pair<Int, Int>,
    changeStartTime: Boolean,
    onChangeStartTimeToggle: (Boolean) -> Unit,
    onStartTimeChange: (TimePickerState) -> Unit
) {
    val view = LocalView.current
    var days1 by rememberSaveable { mutableFloatStateOf((homeInterval / (24 * 60)).toFloat()) }
    var hours1 by rememberSaveable { mutableFloatStateOf(((homeInterval % (24 * 60)) / 60).toFloat()) }
    var minutes1 by rememberSaveable { mutableFloatStateOf((homeInterval % 60).toFloat()) }
    var days2 by rememberSaveable { mutableFloatStateOf((lockInterval / (24 * 60)).toFloat()) }
    var hours2 by rememberSaveable { mutableFloatStateOf(((lockInterval % (24 * 60)) / 60).toFloat()) }
    var minutes2 by rememberSaveable { mutableFloatStateOf((lockInterval % 60).toFloat()) }
    val scope = rememberCoroutineScope()
    val context = LocalView.current
    var job by remember { mutableStateOf<Job?>(null) }
    val shouldShowDialog = remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = startingTime.first,
        initialMinute = startingTime.second,
        is24Hour = false,
    )

    if (shouldShowDialog.value) {
        val initialHour = startingTime.first
        val initialMinute = startingTime.second
        Dialog(
            onDismissRequest = {
                timePickerState.hour = initialHour
                timePickerState.minute = initialMinute
                shouldShowDialog.value = false
            }
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .height(IntrinsicSize.Min)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        text = stringResource(R.string.select_starting_time),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.primaryContainer,
                            clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            clockDialUnselectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            selectorColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            periodSelectorBorderColor = MaterialTheme.colorScheme.outline,
                            periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surface,
                            periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                            timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surface,
                            timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { shouldShowDialog.value = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) { Text(stringResource(R.string.dismiss)) }
                        Button(
                            onClick = { onStartTimeChange(timePickerState); shouldShowDialog.value = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) { Text(stringResource(R.string.confirm)) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { onChangeStartTimeToggle(false); shouldShowDialog.value = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) { Text(stringResource(R.string.reset_to_default)) }
                }
            }
        }
    }

    val columnModifier = remember {
        if (animate) {
            Modifier.animateContentSize(
                animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
            )
        } else {
            Modifier
        }
    }

    Surface(
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp))
    ) {
        Column(
            modifier = columnModifier
        ) {
            val totalMinutes1 = (days1.toInt() * 24 * 60) + (hours1.toInt() * 60) + minutes1.toInt()
            val displayDays1 = totalMinutes1 / (24 * 60)
            val displayHours1 = (totalMinutes1 % (24 * 60)) / 60
            val displayMinutes1 = totalMinutes1 % 60

            val formattedDays1 = if (displayDays1 > 0) {
                context.resources.getQuantityString(R.plurals.days, displayDays1, displayDays1)
            } else { "" }

            val formattedHours1 = if (displayHours1 > 0) {
                context.resources.getQuantityString(R.plurals.hours, displayHours1, displayHours1)
            } else { "" }

            val formattedMinutes1 = if (displayMinutes1 > 0) {
                context.resources.getQuantityString(
                    R.plurals.minutes,
                    displayMinutes1,
                    displayMinutes1
                )
            } else { "" }

            val formattedTime1 = stringResource(
                if (scheduleSeparately)
                    R.string.interval_home
                else
                    R.string.interval,
                listOf(
                    formattedDays1,
                    formattedHours1,
                    formattedMinutes1
                ).filter { it.isNotEmpty() }.joinToString(", ")
            )

            val totalMinutes2 = (days2.toInt() * 24 * 60) + (hours2.toInt() * 60) + minutes2.toInt()
            val displayDays2 = totalMinutes2 / (24 * 60)
            val displayHours2 = (totalMinutes2 % (24 * 60)) / 60
            val displayMinutes2 = totalMinutes2 % 60

            val formattedDays2 = if (displayDays2 > 0) {
                context.resources.getQuantityString(R.plurals.days, displayDays2, displayDays2)
            } else {
                ""
            }

            val formattedHours2 = if (displayHours2 > 0) {
                context.resources.getQuantityString(R.plurals.hours, displayHours2, displayHours2)
            } else { "" }

            val formattedMinutes2 = if (displayMinutes2 > 0) {
                context.resources.getQuantityString(
                    R.plurals.minutes,
                    displayMinutes2,
                    displayMinutes2
                )
            } else { "" }

            val formattedTime2 = stringResource(
                if (scheduleSeparately)
                    R.string.interval_lock
                else
                    R.string.interval,
                listOf(
                    formattedDays2,
                    formattedHours2,
                    formattedMinutes2
                ).filter { it.isNotEmpty() }.joinToString(", ")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.width(32.dp))
                Text(
                    text = if (showInterval) {
                        formattedTime1
                    } else {
                        if (scheduleSeparately) {
                            stringResource(R.string.interval_text)
                        } else {
                            formattedTime1
                        }
                    },
                    fontWeight = FontWeight.W500,
                )
                IconButton(
                    onClick = { onShowIntervalChange(!showInterval) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.show_interval_sliders)
                    )
                }
            }

            if (animate) {
                AnimatedVisibility(
                    visible = showInterval,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    exit = fadeOut(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearOutSlowInEasing
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Days Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.days_txt),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = days1.toInt().toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = days1,
                            onValueChange = { newDays ->
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                days1 = newDays
                                val totalMinute =
                                    (newDays.toInt() * 24 * 60) + (hours1.toInt() * 60) + minutes1.toInt()
                                if (totalMinute < WALLPAPER_CHANGE_INTERVAL_MIN) {
                                    minutes1 =
                                        WALLPAPER_CHANGE_INTERVAL_MIN.toFloat() - (hours1.toInt() * 60)
                                }
                                job?.cancel()
                                job = scope.launch {
                                    delay(500)
                                    onHomeIntervalChange(
                                        newDays.toInt(),
                                        hours1.toInt(),
                                        minutes1.toInt()
                                    )
                                }
                            },
                            valueRange = 0f..30f,
                            steps = 30,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Hours Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.hours_txt),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = hours1.toInt().toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = hours1,
                            onValueChange = { newHours ->
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                hours1 = newHours
                                val totalMinute =
                                    (days1.toInt() * 24 * 60) + (newHours.toInt() * 60) + minutes1.toInt()
                                if (totalMinute < WALLPAPER_CHANGE_INTERVAL_MIN) {
                                    minutes1 =
                                        WALLPAPER_CHANGE_INTERVAL_MIN.toFloat() - (newHours.toInt() * 60)
                                }
                                job?.cancel()
                                job = scope.launch {
                                    delay(500)
                                    onHomeIntervalChange(
                                        days1.toInt(),
                                        newHours.toInt(),
                                        minutes1.toInt()
                                    )
                                }
                            },
                            valueRange = 0f..24f,
                            steps = 24,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Minutes Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.minutes_txt),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = minutes1.toInt().toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = minutes1,
                            onValueChange = { newMinutes ->
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                minutes1 =
                                    if (days1.toInt() == 0 && hours1.toInt() == 0 && newMinutes < WALLPAPER_CHANGE_INTERVAL_MIN) {
                                        WALLPAPER_CHANGE_INTERVAL_MIN.toFloat()
                                    } else {
                                        newMinutes
                                    }
                                job?.cancel()
                                job = scope.launch {
                                    delay(500)
                                    onHomeIntervalChange(
                                        days1.toInt(),
                                        hours1.toInt(),
                                        minutes1.toInt()
                                    )
                                }
                            },
                            valueRange = 0f..60f,
                            steps = 60,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )

                        if (homeEnabled && lockEnabled) {
                            AnimatedVisibility(
                                visible = scheduleSeparately,
                                enter = expandVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ),
                                exit = shrinkVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(horizontal = 32.dp),
                                        thickness = 2.dp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = formattedTime2,
                                        fontWeight = FontWeight.W500,
                                    )
                                    // Days Slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.days_txt),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = days2.toInt().toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Slider(
                                        value = days2,
                                        onValueChange = { newDays ->
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            days2 = newDays
                                            val totalMinute =
                                                (newDays.toInt() * 24 * 60) + (hours2.toInt() * 60) + minutes2.toInt()
                                            if (totalMinute < WALLPAPER_CHANGE_INTERVAL_MIN) {
                                                minutes2 =
                                                    WALLPAPER_CHANGE_INTERVAL_MIN.toFloat() - (hours2.toInt() * 60)
                                            }
                                            job?.cancel()
                                            job = scope.launch {
                                                delay(500)
                                                onLockIntervalChange(
                                                    newDays.toInt(),
                                                    hours2.toInt(),
                                                    minutes2.toInt()
                                                )
                                            }
                                        },
                                        valueRange = 0f..30f,
                                        steps = 30,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Hours Slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.hours_txt),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = hours2.toInt().toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Slider(
                                        value = hours2,
                                        onValueChange = { newHours ->
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            hours2 = newHours
                                            val totalMinute =
                                                (days2.toInt() * 24 * 60) + (newHours.toInt() * 60) + minutes2.toInt()
                                            if (totalMinute < WALLPAPER_CHANGE_INTERVAL_MIN) {
                                                minutes2 =
                                                    WALLPAPER_CHANGE_INTERVAL_MIN.toFloat() - (newHours.toInt() * 60)
                                            }
                                            job?.cancel()
                                            job = scope.launch {
                                                delay(500)
                                                onLockIntervalChange(
                                                    days2.toInt(),
                                                    newHours.toInt(),
                                                    minutes2.toInt()
                                                )
                                            }
                                        },
                                        valueRange = 0f..24f,
                                        steps = 24,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Minutes Slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.minutes_txt),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = minutes2.toInt().toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Slider(
                                        value = minutes2,
                                        onValueChange = { newMinutes ->
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            minutes2 =
                                                if (days2.toInt() == 0 && hours2.toInt() == 0 && newMinutes < WALLPAPER_CHANGE_INTERVAL_MIN) {
                                                    WALLPAPER_CHANGE_INTERVAL_MIN.toFloat()
                                                } else {
                                                    newMinutes
                                                }
                                            job?.cancel()
                                            job = scope.launch {
                                                delay(500)
                                                onLockIntervalChange(
                                                    days2.toInt(),
                                                    hours2.toInt(),
                                                    minutes2.toInt()
                                                )
                                            }
                                        },
                                        valueRange = 0f..60f,
                                        steps = 60,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = { shouldShowDialog.value = true },
                            modifier = Modifier
                                .padding(12.dp)
                                .align(Alignment.CenterHorizontally)
                                .then(
                                    if (animate) {
                                        Modifier.animateContentSize(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                val (hour, minute, period) = if (changeStartTime) {
                                    val hour = if (timePickerState.hour == 0) 12 else timePickerState.hour % 12
                                    val period = if (timePickerState.hour < 12) "AM" else "PM"
                                    Triple(hour, timePickerState.minute, period)
                                } else {
                                    val currentTime = Calendar.getInstance()
                                    val hour = if (currentTime.get(Calendar.HOUR) == 0) 12 else currentTime.get(Calendar.HOUR) % 12
                                    val period = if (currentTime.get(Calendar.AM_PM) == 0) "AM" else "PM"
                                    Triple(hour, currentTime.get(Calendar.MINUTE), period)
                                }
                                val formattedMinutes = String.format(Locale.getDefault(), "%02d", minute)
                                Text(
                                    text = "$hour:$formattedMinutes $period",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            } else {
                if (showInterval) {
                    Slider(
                        value = days1,
                        onValueChange = { newDays ->
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            days1 = newDays
                            val totalMinute =
                                (newDays.toInt() * 24 * 60) + (hours1.toInt() * 60) + minutes1.toInt()
                            if (totalMinute < WALLPAPER_CHANGE_INTERVAL_MIN) {
                                minutes1 =
                                    WALLPAPER_CHANGE_INTERVAL_MIN.toFloat() - (hours1.toInt() * 60)
                            }
                            job?.cancel()
                            job = scope.launch {
                                delay(500)
                                onHomeIntervalChange(
                                    newDays.toInt(),
                                    hours1.toInt(),
                                    minutes1.toInt()
                                )
                            }
                        },
                        valueRange = 0f..30f,
                        steps = 30,
                        modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                    )

                    Slider(
                        value = hours1,
                        onValueChange = { newHours ->
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            hours1 = newHours
                            val totalMinute =
                                (days1.toInt() * 24 * 60) + (newHours.toInt() * 60) + minutes1.toInt()
                            if (totalMinute < WALLPAPER_CHANGE_INTERVAL_MIN) {
                                minutes1 =
                                    WALLPAPER_CHANGE_INTERVAL_MIN.toFloat() - (newHours.toInt() * 60)
                            }
                            job?.cancel()
                            job = scope.launch {
                                delay(500)
                                onHomeIntervalChange(
                                    days1.toInt(),
                                    newHours.toInt(),
                                    minutes1.toInt()
                                )
                            }
                        },
                        valueRange = 0f..24f,
                        steps = 24,
                        modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                    )

                    Slider(
                        value = minutes1,
                        onValueChange = { newMinutes ->
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            minutes1 =
                                if (days1.toInt() == 0 && hours1.toInt() == 0 && newMinutes < WALLPAPER_CHANGE_INTERVAL_MIN) {
                                    WALLPAPER_CHANGE_INTERVAL_MIN.toFloat()
                                } else {
                                    newMinutes
                                }
                            job?.cancel()
                            job = scope.launch {
                                delay(500)
                                onHomeIntervalChange(
                                    days1.toInt(),
                                    hours1.toInt(),
                                    minutes1.toInt()
                                )
                            }
                        },
                        valueRange = 0f..60f,
                        steps = 60,
                        modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                    )
                }
            }
        }
    }
}