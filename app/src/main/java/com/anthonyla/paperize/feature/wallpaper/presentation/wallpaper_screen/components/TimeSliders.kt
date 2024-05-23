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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TimeSliders is a composable that displays sliders for the user to select the time interval for changing wallpaper
 */
@Composable
fun TimeSliders(
    timeInMinutes1: Int,
    timeInMinutes2: Int,
    showInterval: Boolean,
    animate: Boolean,
    onShowIntervalChange: (Boolean) -> Unit,
    scheduleSeparately: Boolean,
    onScheduleSeparatelyChange: (Boolean) -> Unit,
    onTimeChange1: (Int, Int, Int) -> Unit,
    onTimeChange2: (Int, Int, Int) -> Unit,
    lockEnabled: Boolean,
    homeEnabled: Boolean,
) {
    val view = LocalView.current
    var days1 by rememberSaveable { mutableFloatStateOf((timeInMinutes1 / (24 * 60)).toFloat()) }
    var hours1 by rememberSaveable { mutableFloatStateOf(((timeInMinutes1 % (24 * 60)) / 60).toFloat()) }
    var minutes1 by rememberSaveable { mutableFloatStateOf((timeInMinutes1 % 60).toFloat()) }
    var days2 by rememberSaveable { mutableFloatStateOf((timeInMinutes2 / (24 * 60)).toFloat()) }
    var hours2 by rememberSaveable { mutableFloatStateOf(((timeInMinutes2 % (24 * 60)) / 60).toFloat()) }
    var minutes2 by rememberSaveable { mutableFloatStateOf((timeInMinutes2 % 60).toFloat()) }
    val scope = rememberCoroutineScope()
    val context = LocalView.current
    var job by remember { mutableStateOf<Job?>(null) }

    val columnModifier = remember {
        if (animate) {
            Modifier.animateContentSize(
                animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
            )
        } else { Modifier }
    }

    Surface(
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp))
    ) {
        Column (
            modifier = columnModifier
        ) {
            val totalMinutes1 = (days1.toInt() * 24 * 60) + (hours1.toInt() * 60) + minutes1.toInt()
            val displayDays1 = totalMinutes1 / (24 * 60)
            val displayHours1 = (totalMinutes1 % (24 * 60)) / 60
            val displayMinutes1 = totalMinutes1 % 60

            val formattedDays1 = if (displayDays1 > 0) {
                context.resources.getQuantityString(R.plurals.days, displayDays1, displayDays1)
            } else {""}

            val formattedHours1 = if (displayHours1 > 0) {
                context.resources.getQuantityString(R.plurals.hours, displayHours1, displayHours1)
            } else {""}

            val formattedMinutes1 = if (displayMinutes1 > 0) {
                context.resources.getQuantityString(R.plurals.minutes, displayMinutes1, displayMinutes1)
            } else {""}

            val formattedTime1 = stringResource(
                if (scheduleSeparately)
                    R.string.interval_home
                else
                    R.string.interval,
                listOf(formattedDays1, formattedHours1, formattedMinutes1).filter { it.isNotEmpty() }.joinToString(", ")
            )

            val totalMinutes2 = (days2.toInt() * 24 * 60) + (hours2.toInt() * 60) + minutes2.toInt()
            val displayDays2 = totalMinutes2 / (24 * 60)
            val displayHours2 = (totalMinutes2 % (24 * 60)) / 60
            val displayMinutes2 = totalMinutes2 % 60

            val formattedDays2 = if (displayDays2 > 0) {
                context.resources.getQuantityString(R.plurals.days, displayDays2, displayDays2)
            } else {""}

            val formattedHours2 = if (displayHours2 > 0) {
                context.resources.getQuantityString(R.plurals.hours, displayHours2, displayHours2)
            } else {""}

            val formattedMinutes2 = if (displayMinutes2 > 0) {
                context.resources.getQuantityString(R.plurals.minutes, displayMinutes2, displayMinutes2)
            } else {""}

            val formattedTime2 = stringResource(
                if (scheduleSeparately)
                    R.string.interval_lock
                else
                    R.string.interval,
                listOf(formattedDays2, formattedHours2, formattedMinutes2).filter { it.isNotEmpty() }.joinToString(", ")
            )

            Row (
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
                    Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.show_interval_sliders))
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
                    Column {
                        Slider(
                            value = days1,
                            onValueChange = { newDays ->
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                days1 = newDays
                                val totalMinute =
                                    (newDays.toInt() * 24 * 60) + (hours1.toInt() * 60) + minutes1.toInt()
                                if (totalMinute < WALLPAPER_CHANGE_INTERVAL_DEFAULT) {
                                    minutes1 = WALLPAPER_CHANGE_INTERVAL_DEFAULT.toFloat() - (hours1.toInt() * 60)
                                }
                                job?.cancel()
                                job = scope.launch {
                                    delay(500)
                                    onTimeChange1(newDays.toInt(), hours1.toInt(), minutes1.toInt())
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
                                if (totalMinute < WALLPAPER_CHANGE_INTERVAL_DEFAULT) {
                                    minutes1 = WALLPAPER_CHANGE_INTERVAL_DEFAULT.toFloat() - (newHours.toInt() * 60)
                                }
                                job?.cancel()
                                job = scope.launch {
                                    delay(500)
                                    onTimeChange1(days1.toInt(), newHours.toInt(), minutes1.toInt())
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
                                    if (days1.toInt() == 0 && hours1.toInt() == 0 && newMinutes < WALLPAPER_CHANGE_INTERVAL_DEFAULT) {
                                        WALLPAPER_CHANGE_INTERVAL_DEFAULT.toFloat()
                                    } else {
                                        newMinutes
                                    }
                                job?.cancel()
                                job = scope.launch {
                                    delay(500)
                                    onTimeChange1(days1.toInt(), hours1.toInt(), minutes1.toInt())
                                }
                            },
                            valueRange = 0f..60f,
                            steps = 60,
                            modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
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
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = formattedTime2,
                                        fontWeight = FontWeight.W500,
                                    )
                                    Slider(
                                        value = days2,
                                        onValueChange = { newDays ->
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            days2 = newDays
                                            val totalMinute =
                                                (newDays.toInt() * 24 * 60) + (hours2.toInt() * 60) + minutes2.toInt()
                                            if (totalMinute < WALLPAPER_CHANGE_INTERVAL_DEFAULT) {
                                                minutes2 = WALLPAPER_CHANGE_INTERVAL_DEFAULT.toFloat() - (hours2.toInt() * 60)
                                            }
                                            job?.cancel()
                                            job = scope.launch {
                                                delay(500)
                                                onTimeChange2(
                                                    newDays.toInt(),
                                                    hours2.toInt(),
                                                    minutes2.toInt()
                                                )
                                            }
                                        },
                                        valueRange = 0f..30f,
                                        steps = 30,
                                        modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                                    )

                                    Slider(
                                        value = hours2,
                                        onValueChange = { newHours ->
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            hours2 = newHours
                                            val totalMinute =
                                                (days2.toInt() * 24 * 60) + (newHours.toInt() * 60) + minutes2.toInt()
                                            if (totalMinute < WALLPAPER_CHANGE_INTERVAL_DEFAULT) {
                                                minutes2 = WALLPAPER_CHANGE_INTERVAL_DEFAULT.toFloat() - (newHours.toInt() * 60)
                                            }
                                            job?.cancel()
                                            job = scope.launch {
                                                delay(500)
                                                onTimeChange2(
                                                    days2.toInt(),
                                                    newHours.toInt(),
                                                    minutes2.toInt()
                                                )
                                            }
                                        },
                                        valueRange = 0f..24f,
                                        steps = 24,
                                        modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                                    )

                                    Slider(
                                        value = minutes2,
                                        onValueChange = { newMinutes ->
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            minutes2 =
                                                if (days2.toInt() == 0 && hours2.toInt() == 0 && newMinutes < WALLPAPER_CHANGE_INTERVAL_DEFAULT) {
                                                    WALLPAPER_CHANGE_INTERVAL_DEFAULT.toFloat()
                                                } else {
                                                    newMinutes
                                                }
                                            job?.cancel()
                                            job = scope.launch {
                                                delay(500)
                                                onTimeChange2(
                                                    days2.toInt(),
                                                    hours2.toInt(),
                                                    minutes2.toInt()
                                                )
                                            }
                                        },
                                        valueRange = 0f..60f,
                                        steps = 60,
                                        modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = stringResource(R.string.individual_scheduling), fontWeight = FontWeight.W500)
                                Spacer(modifier = Modifier.width(16.dp))
                                Switch(
                                    checked = scheduleSeparately,
                                    onCheckedChange = onScheduleSeparatelyChange
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            else {
                if (showInterval) {
                    Slider(
                        value = days1,
                        onValueChange = { newDays ->
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            days1 = newDays
                            val totalMinute = (newDays.toInt() * 24 * 60) + (hours1.toInt() * 60) + minutes1.toInt()
                            if (totalMinute < WALLPAPER_CHANGE_INTERVAL_DEFAULT) {
                                minutes1 = WALLPAPER_CHANGE_INTERVAL_DEFAULT.toFloat() - (hours1.toInt() * 60)
                            }
                            job?.cancel()
                            job = scope.launch {
                                delay(500)
                                onTimeChange1(newDays.toInt(), hours1.toInt(), minutes1.toInt())
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
                            val totalMinute = (days1.toInt() * 24 * 60) + (newHours.toInt() * 60) + minutes1.toInt()
                            if (totalMinute < WALLPAPER_CHANGE_INTERVAL_DEFAULT) {
                                minutes1 = WALLPAPER_CHANGE_INTERVAL_DEFAULT.toFloat() - (newHours.toInt() * 60)
                            }
                            job?.cancel()
                            job = scope.launch {
                                delay(500)
                                onTimeChange1(days1.toInt(), newHours.toInt(), minutes1.toInt())
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
                            minutes1 = if (days1.toInt() == 0 && hours1.toInt() == 0 && newMinutes < WALLPAPER_CHANGE_INTERVAL_DEFAULT) {
                                WALLPAPER_CHANGE_INTERVAL_DEFAULT.toFloat()
                            } else {
                                newMinutes
                            }
                            job?.cancel()
                            job = scope.launch {
                                delay(500)
                                onTimeChange1(days1.toInt(), hours1.toInt(), minutes1.toInt())
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