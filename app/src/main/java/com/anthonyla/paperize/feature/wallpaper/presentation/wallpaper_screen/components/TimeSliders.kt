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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TimeSliders is a composable that displays sliders for the user to select the time interval for changing wallpaper
 */
@Composable
fun TimeSliders(
    onTimeChange: (Int, Int, Int) -> Unit,
    timeInMinutes: Int,
    showInterval: Boolean,
    animate: Boolean,
    onShowIntervalChange: (Boolean) -> Unit
) {
    val view = LocalView.current
    var days by rememberSaveable { mutableFloatStateOf((timeInMinutes / (24 * 60)).toFloat()) }
    var hours by rememberSaveable { mutableFloatStateOf(((timeInMinutes % (24 * 60)) / 60).toFloat()) }
    var minutes by rememberSaveable { mutableFloatStateOf((timeInMinutes % 60).toFloat()) }
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
            val totalMinutes = (days.toInt() * 24 * 60) + (hours.toInt() * 60) + minutes.toInt()
            val displayDays = totalMinutes / (24 * 60)
            val displayHours = (totalMinutes % (24 * 60)) / 60
            val displayMinutes = totalMinutes % 60
            val formattedDays = if (displayDays > 0) {
                context.resources.getQuantityString(R.plurals.days, displayDays, displayDays)
            } else {""}

            val formattedHours = if (displayHours > 0) {
                context.resources.getQuantityString(R.plurals.hours, displayHours, displayHours)
            } else {""}

            val formattedMinutes = if (displayMinutes > 0) {
                context.resources.getQuantityString(R.plurals.minutes, displayMinutes, displayMinutes)
            } else {""}

            val formattedTime = stringResource(
                R.string.interval,
                listOf(formattedDays, formattedHours, formattedMinutes).filter { it.isNotEmpty() }.joinToString(", ")
            )

            Row (
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.width(32.dp))
                Text(
                    text = formattedTime,
                    fontWeight = FontWeight.W500,
                )
                IconButton(
                    onClick = { onShowIntervalChange(!showInterval) },
                ) {
                    Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "Show interval sliders")
                }
            }

            if (animate) {
                AnimatedVisibility(
                    visible = showInterval,
                    enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
                    exit = fadeOut(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearOutSlowInEasing
                        )
                    )
                ) {
                    Column {
                        Slider(
                            value = days,
                            onValueChange = { newDays ->
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                days = newDays
                                val totalMinute = (newDays.toInt() * 24 * 60) + (hours.toInt() * 60) + minutes.toInt()
                                if (totalMinute < 15) {
                                    minutes = 15f - (hours.toInt() * 60)
                                }
                                job?.cancel()
                                job = scope.launch {
                                    delay(500)
                                    onTimeChange(newDays.toInt(), hours.toInt(), minutes.toInt())
                                }
                            },
                            valueRange = 0f..30f,
                            steps = 30,
                            modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                        )

                        Slider(
                            value = hours,
                            onValueChange = { newHours ->
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                hours = newHours
                                val totalMinute = (days.toInt() * 24 * 60) + (newHours.toInt() * 60) + minutes.toInt()
                                if (totalMinute < 15) {
                                    minutes = 15f - (newHours.toInt() * 60)
                                }
                                job?.cancel()
                                job = scope.launch {
                                    delay(500)
                                    onTimeChange(days.toInt(), newHours.toInt(), minutes.toInt())
                                }
                            },
                            valueRange = 0f..24f,
                            steps = 24,
                            modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                        )

                        Slider(
                            value = minutes,
                            onValueChange = { newMinutes ->
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                minutes = if (days.toInt() == 0 && hours.toInt() == 0 && newMinutes < 15) {
                                    15f
                                } else {
                                    newMinutes
                                }
                                job?.cancel()
                                job = scope.launch {
                                    delay(500)
                                    onTimeChange(days.toInt(), hours.toInt(), minutes.toInt())
                                }
                            },
                            valueRange = 0f..60f,
                            steps = 60,
                            modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            else {
                if (showInterval) {
                    Slider(
                        value = days,
                        onValueChange = { newDays ->
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            days = newDays
                            val totalMinute = (newDays.toInt() * 24 * 60) + (hours.toInt() * 60) + minutes.toInt()
                            if (totalMinute < 15) {
                                minutes = 15f - (hours.toInt() * 60)
                            }
                            job?.cancel()
                            job = scope.launch {
                                delay(500)
                                onTimeChange(newDays.toInt(), hours.toInt(), minutes.toInt())
                            }
                        },
                        valueRange = 0f..30f,
                        steps = 30,
                        modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                    )

                    Slider(
                        value = hours,
                        onValueChange = { newHours ->
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            hours = newHours
                            val totalMinute = (days.toInt() * 24 * 60) + (newHours.toInt() * 60) + minutes.toInt()
                            if (totalMinute < 15) {
                                minutes = 15f - (newHours.toInt() * 60)
                            }
                            job?.cancel()
                            job = scope.launch {
                                delay(500)
                                onTimeChange(days.toInt(), newHours.toInt(), minutes.toInt())
                            }
                        },
                        valueRange = 0f..24f,
                        steps = 24,
                        modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                    )

                    Slider(
                        value = minutes,
                        onValueChange = { newMinutes ->
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            minutes = if (days.toInt() == 0 && hours.toInt() == 0 && newMinutes < 15) {
                                15f
                            } else {
                                newMinutes
                            }
                            job?.cancel()
                            job = scope.launch {
                                delay(500)
                                onTimeChange(days.toInt(), hours.toInt(), minutes.toInt())
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