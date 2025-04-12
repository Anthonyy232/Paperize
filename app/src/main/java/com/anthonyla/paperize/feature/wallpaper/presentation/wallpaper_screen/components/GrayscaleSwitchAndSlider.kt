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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Composable that displays a switch and slider for the grayscale effect
 */
@Composable
fun GrayscaleSwitchAndSlider(
    onGrayscalePercentageChange: (Int, Int) -> Unit,
    onGrayscaleChange: (Boolean) -> Unit,
    grayscale: Boolean,
    bothEnabled: Boolean,
    homeGrayscalePercentage: Int,
    lockGrayscalePercentage: Int,
    animate: Boolean
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var homePercentage by rememberSaveable { mutableFloatStateOf(homeGrayscalePercentage.toFloat()) }
    var lockPercentage by rememberSaveable { mutableFloatStateOf(lockGrayscalePercentage.toFloat()) }

    Surface(
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp))
    ) {
        val columnModifier = if (animate) {
            Modifier.animateContentSize(animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing))
        } else { Modifier }
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = columnModifier
        ) {
            Row (horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.gray_filter),
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.W500
                )
                Switch(
                    checked = grayscale,
                    onCheckedChange = onGrayscaleChange
                )
            }
            if (animate) {
                AnimatedVisibility(
                    visible = grayscale,
                    enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
                    exit = fadeOut(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearOutSlowInEasing
                        )
                    )
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (bothEnabled) {
                            Text(
                                text = stringResource(R.string.home_screen) + " | " + stringResource(R.string.percentage, homePercentage.roundToInt()),
                                modifier = Modifier.padding(PaddingValues(horizontal = 24.dp)),
                                fontWeight = FontWeight.W400
                            )
                        }
                        else {
                            Text(
                                text = stringResource(R.string.percentage, homePercentage.roundToInt()),
                                modifier = Modifier.padding(PaddingValues(horizontal = 24.dp)),
                                fontWeight = FontWeight.W400
                            )
                        }
                        Slider(
                            value = homePercentage,
                            onValueChange = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                homePercentage = it
                                job?.cancel()
                                job = scope.launch {
                                    delay(500)
                                    if (bothEnabled) {
                                        onGrayscalePercentageChange(it.roundToInt(), lockPercentage.roundToInt())
                                    } else {
                                        onGrayscalePercentageChange(it.roundToInt(), it.roundToInt())
                                    }
                                }
                            },
                            valueRange = 0f..100f,
                            steps = 100,
                            modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                        )
                        if (bothEnabled) {
                            Text(
                                text = stringResource(R.string.lock) + " | " + stringResource(R.string.percentage, lockPercentage.roundToInt()),
                                modifier = Modifier.padding(PaddingValues(horizontal = 24.dp)),
                                fontWeight = FontWeight.W400
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Slider(
                                value = lockPercentage,
                                onValueChange = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    lockPercentage = it
                                    job?.cancel()
                                    job = scope.launch {
                                        delay(500)
                                        onGrayscalePercentageChange(homePercentage.roundToInt(), it.roundToInt())
                                    }
                                },
                                valueRange = 0f..100f,
                                steps = 100,
                                modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                if (grayscale) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (bothEnabled) {
                            Text(
                                text = stringResource(R.string.home_screen) + " | " + stringResource(R.string.percentage, homePercentage.roundToInt()),
                                modifier = Modifier.padding(PaddingValues(horizontal = 24.dp)),
                                fontWeight = FontWeight.W400
                            )
                        }
                        else {
                            Text(
                                text = stringResource(R.string.percentage, homePercentage.roundToInt()),
                                modifier = Modifier.padding(PaddingValues(horizontal = 24.dp)),
                                fontWeight = FontWeight.W400
                            )
                        }
                        Slider(
                            value = homePercentage,
                            onValueChange = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                homePercentage = it
                                job?.cancel()
                                job = scope.launch {
                                    delay(500)
                                    if (bothEnabled) {
                                        onGrayscalePercentageChange(it.roundToInt(), lockPercentage.roundToInt())
                                    } else {
                                        onGrayscalePercentageChange(it.roundToInt(), it.roundToInt())
                                    }
                                }
                            },
                            valueRange = 0f..100f,
                            steps = 100,
                            modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                        )
                        if (bothEnabled) {
                            Text(
                                text = stringResource(R.string.lock) + " | " + stringResource(R.string.percentage, lockPercentage.roundToInt()),
                                modifier = Modifier.padding(PaddingValues(horizontal = 24.dp)),
                                fontWeight = FontWeight.W400
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Slider(
                                value = lockPercentage,
                                onValueChange = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    lockPercentage = it
                                    job?.cancel()
                                    job = scope.launch {
                                        delay(500)
                                        onGrayscalePercentageChange(homePercentage.roundToInt(), it.roundToInt())
                                    }
                                },
                                valueRange = 0f..100f,
                                steps = 100,
                                modifier = Modifier.padding(PaddingValues(horizontal = 30.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}