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

private val FADE_OUT_MILLIS = (NavConstants.NAVIGATION_TIME * NavConstants.FADE_OUT_FRACTION).toInt()

/** Slides in with a delayed fade so the outgoing screen fades first. */
private fun sharedXTransitionIn(initial: (fullWidth: Int) -> Int): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(
            durationMillis = NavConstants.NAVIGATION_TIME,
            easing = FastOutSlowInEasing
        ),
        initialOffsetX = initial
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = NavConstants.NAVIGATION_TIME - FADE_OUT_MILLIS,
            delayMillis = FADE_OUT_MILLIS,
            easing = LinearOutSlowInEasing
        )
    )

/** Slides out while fading immediately to make room for the incoming screen. */
private fun sharedXTransitionOut(target: (fullWidth: Int) -> Int): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(
            durationMillis = NavConstants.NAVIGATION_TIME,
            easing = FastOutSlowInEasing
        ),
        targetOffsetX = target
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = FADE_OUT_MILLIS,
            easing = FastOutLinearInEasing
        )
    )

fun enterTransitionForward(): EnterTransition = sharedXTransitionIn(
    initial = { fullWidth -> (fullWidth * NavConstants.INITIAL_OFFSET).toInt() }
)

fun exitTransitionForward(): ExitTransition = sharedXTransitionOut(
    target = { fullWidth -> -(fullWidth * NavConstants.INITIAL_OFFSET).toInt() }
)

fun enterTransitionBackward(): EnterTransition = sharedXTransitionIn(
    initial = { fullWidth -> -(fullWidth * NavConstants.INITIAL_OFFSET).toInt() }
)

fun exitTransitionBackward(): ExitTransition = sharedXTransitionOut(
    target = { fullWidth -> (fullWidth * NavConstants.INITIAL_OFFSET).toInt() }
)
