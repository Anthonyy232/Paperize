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

/**
 * Shared horizontal transition for entering screens
 *
 * Creates a smooth slide-in animation from the specified offset with a delayed fade-in effect.
 * The fade-in starts after the outgoing screen begins to fade out, creating a seamless transition.
 *
 * @param initial Lambda that calculates the initial horizontal offset based on screen width
 * @param durationMillis Total animation duration in milliseconds (default from NavConstants)
 * @return Combined enter transition with slide and fade animations
 */
fun sharedXTransitionIn(
    initial: (fullWidth: Int) -> Int,
    durationMillis: Int = NavConstants.NAVIGATION_TIME,
): EnterTransition {
    // Calculate timing for outgoing and incoming animations
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

/**
 * Shared horizontal transition for exiting screens
 *
 * Creates a smooth slide-out animation to the specified offset with an immediate fade-out effect.
 * The fade-out starts immediately to make way for the incoming screen.
 *
 * @param target Lambda that calculates the target horizontal offset based on screen width
 * @param durationMillis Total animation duration in milliseconds (default from NavConstants)
 * @return Combined exit transition with slide and fade animations
 */
fun sharedXTransitionOut(
    target: (fullWidth: Int) -> Int,
    durationMillis: Int = NavConstants.NAVIGATION_TIME,
): ExitTransition {
    // Calculate timing for outgoing fade animation
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
