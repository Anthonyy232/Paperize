package com.anthonyla.paperize.presentation.common.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anthonyla.paperize.core.util.PermissionUtil
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.presentation.common.navigation.util.enterTransitionBackward
import com.anthonyla.paperize.presentation.common.navigation.util.enterTransitionForward
import com.anthonyla.paperize.presentation.common.navigation.util.exitTransitionBackward
import com.anthonyla.paperize.presentation.common.navigation.util.exitTransitionForward
import com.anthonyla.paperize.presentation.screens.album_view.AlbumViewScreen
import com.anthonyla.paperize.presentation.screens.folder_view.FolderViewScreen
import com.anthonyla.paperize.presentation.screens.home.HomeScreen
import com.anthonyla.paperize.presentation.screens.notification.NotificationPermissionScreen
import com.anthonyla.paperize.presentation.screens.privacy.PrivacyScreen
import com.anthonyla.paperize.presentation.screens.settings.SettingsScreen
import com.anthonyla.paperize.presentation.screens.startup.StartupScreen
import com.anthonyla.paperize.presentation.screens.wallpaper_mode_selection.WallpaperModeSelectionScreen
import com.anthonyla.paperize.presentation.screens.wallpaper_view.WallpaperViewScreen

@Composable
fun NavigationGraph(
    startDestination: Any,
    animate: Boolean,
    onFirstLaunchComplete: () -> Unit
) {
    val enterForward: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        if (animate) { { enterTransitionForward() } } else { { EnterTransition.None } }
    val exitForward: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        if (animate) { { exitTransitionForward() } } else { { ExitTransition.None } }
    val enterBackward: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        if (animate) { { enterTransitionBackward() } } else { { EnterTransition.None } }
    val exitBackward: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        if (animate) { { exitTransitionBackward() } } else { { ExitTransition.None } }
    val context = LocalContext.current
    val navController = rememberNavController()
    val viewWallpaper: (Wallpaper) -> Unit = { wallpaper ->
        navController.navigate(
            WallpaperViewRoute(wallpaper.id, wallpaper.uri, wallpaper.displayFileName)
        )
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = enterForward,
        exitTransition = exitForward,
        popEnterTransition = enterBackward,
        popExitTransition = exitBackward
    ) {
        composable<StartupRoute> {
            StartupScreen(
                onAgree = {
                    navController.navigate(WallpaperModeSelectionRoute) {
                        popUpTo<StartupRoute> { inclusive = true }
                    }
                },
                onPrivacyClick = {
                    navController.navigate(PrivacyRoute)
                }
            )
        }

        composable<WallpaperModeSelectionRoute> {
            WallpaperModeSelectionScreen(
                onModeSelected = {
                    if (!PermissionUtil.hasNotificationPermission(context)) {
                        navController.navigate(NotificationRoute) {
                            popUpTo<WallpaperModeSelectionRoute> { inclusive = true }
                        }
                    } else {
                        onFirstLaunchComplete()
                        navController.navigate(HomeRoute) {
                            popUpTo<WallpaperModeSelectionRoute> { inclusive = true }
                        }
                    }
                }
            )
        }

        composable<NotificationRoute> {
            NotificationPermissionScreen(
                onContinue = {
                    onFirstLaunchComplete()
                    navController.navigate(HomeRoute) {
                        popUpTo<NotificationRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<HomeRoute> {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(SettingsRoute)
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(AlbumRoute(albumId))
                }
            )
        }

        composable<AlbumRoute> {
            AlbumViewScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToFolder = { folderId ->
                    navController.navigate(FolderRoute(folderId))
                },
                onNavigateToWallpaperView = viewWallpaper
            )
        }

        composable<FolderRoute> {
            FolderViewScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToWallpaperView = viewWallpaper
            )
        }

        composable<WallpaperViewRoute> {
            WallpaperViewScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate(PrivacyRoute) }
            )
        }

        composable<PrivacyRoute> {
            PrivacyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
