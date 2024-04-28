package com.anthonyla.paperize.feature.wallpaper.util.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.anthonyla.paperize.feature.wallpaper.util.navigation.NavConstants.NAVIGATION_TIME
import com.anthonyla.paperize.feature.wallpaper.util.navigation.NavConstants.OFFSET_LIMIT

fun sharedXTransitionIn(
    initial: (fullWidth: Int) -> Int,
    durationMillis: Int = NAVIGATION_TIME,
): EnterTransition {
    val outgoingDuration = (durationMillis * OFFSET_LIMIT).toInt()
    val incomingDuration = durationMillis - outgoingDuration

    return slideInHorizontally(
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        ), initialOffsetX = initial
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = incomingDuration,
            delayMillis = outgoingDuration,
            easing = LinearOutSlowInEasing
        )
    )
}

fun sharedXTransitionOut(
    target: (fullWidth: Int) -> Int,
    durationMillis: Int = NAVIGATION_TIME,
): ExitTransition {
    val outgoingDuration = (durationMillis * OFFSET_LIMIT).toInt()

    return slideOutHorizontally(
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        ), targetOffsetX = target
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = outgoingDuration,
            delayMillis = 0,
            easing = FastOutLinearInEasing
        )
    )
}