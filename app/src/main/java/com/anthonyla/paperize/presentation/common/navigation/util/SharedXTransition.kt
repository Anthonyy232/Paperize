package com.anthonyla.paperize.presentation.common.navigation.util

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

/** Slides in with a delayed fade so the outgoing screen fades first. */
fun sharedXTransitionIn(
    initial: (fullWidth: Int) -> Int,
    durationMillis: Int = NavConstants.NAVIGATION_TIME,
): EnterTransition {
    val outgoingDuration = (durationMillis * NavConstants.OFFSET_LIMIT).toInt()
    val incomingDuration = durationMillis - outgoingDuration

    return slideInHorizontally(
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        ),
        initialOffsetX = initial
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = incomingDuration,
            delayMillis = outgoingDuration,
            easing = LinearOutSlowInEasing
        )
    )
}

/** Slides out while fading immediately to make room for the incoming screen. */
fun sharedXTransitionOut(
    target: (fullWidth: Int) -> Int,
    durationMillis: Int = NavConstants.NAVIGATION_TIME,
): ExitTransition {
    val outgoingDuration = (durationMillis * NavConstants.OFFSET_LIMIT).toInt()

    return slideOutHorizontally(
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        ),
        targetOffsetX = target
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = outgoingDuration,
            delayMillis = 0,
            easing = FastOutLinearInEasing
        )
    )
}

/**
 * Standard forward navigation enter transition (slide in from right)
 */
fun enterTransitionForward(): EnterTransition = sharedXTransitionIn(
    initial = { fullWidth -> (fullWidth * NavConstants.INITIAL_OFFSET).toInt() }
)

/**
 * Standard forward navigation exit transition (slide out to left)
 */
fun exitTransitionForward(): ExitTransition = sharedXTransitionOut(
    target = { fullWidth -> -(fullWidth * NavConstants.INITIAL_OFFSET).toInt() }
)

/**
 * Standard backward navigation enter transition (slide in from left)
 */
fun enterTransitionBackward(): EnterTransition = sharedXTransitionIn(
    initial = { fullWidth -> -(fullWidth * NavConstants.INITIAL_OFFSET).toInt() }
)

/**
 * Standard backward navigation exit transition (slide out to right)
 */
fun exitTransitionBackward(): ExitTransition = sharedXTransitionOut(
    target = { fullWidth -> (fullWidth * NavConstants.INITIAL_OFFSET).toInt() }
)
