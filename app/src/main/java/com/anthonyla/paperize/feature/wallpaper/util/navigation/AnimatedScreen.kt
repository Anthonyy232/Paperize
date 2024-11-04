package com.anthonyla.paperize.feature.wallpaper.util.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import com.anthonyla.paperize.feature.wallpaper.util.navigation.NavConstants.INITIAL_OFFSET
import kotlin.reflect.KType

inline fun <reified T : Any> NavGraphBuilder.animatedScreen(
    typeMap: Map<KType, NavType<*>> = emptyMap(),
    animate: Boolean = true,
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) = composable<T>(
    typeMap = typeMap,
    deepLinks = deepLinks,
    enterTransition = {
        if (animate) {
            sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
        } else { null }
    },
    exitTransition = {
        if (animate) {
            sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
        } else { null }
    },
    popEnterTransition = {
        if (animate) {
            sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
        } else { null }
    },
    popExitTransition = {
        if (animate) {
            sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
        } else { null }
    },
    content = content
)