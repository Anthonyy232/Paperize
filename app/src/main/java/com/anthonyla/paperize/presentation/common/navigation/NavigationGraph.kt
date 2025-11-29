package com.anthonyla.paperize.presentation.common.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
import com.anthonyla.paperize.presentation.screens.sort.SortViewScreen
import com.anthonyla.paperize.presentation.screens.startup.StartupScreen
import com.anthonyla.paperize.presentation.screens.storage.StoragePermissionScreen
import com.anthonyla.paperize.presentation.screens.wallpaper_view.WallpaperViewScreen

/**
 * Navigation graph for Paperize
 */
@Composable
fun NavigationGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Any = StartupRoute,
    animate: Boolean = true,
    onFirstLaunchComplete: () -> Unit = {}
) {
    // Conditional transitions based on animate setting
    val enterForward: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        if (animate) { { enterTransitionForward() } } else { { EnterTransition.None } }
    val exitForward: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        if (animate) { { exitTransitionForward() } } else { { ExitTransition.None } }
    val enterBackward: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        if (animate) { { enterTransitionBackward() } } else { { EnterTransition.None } }
    val exitBackward: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        if (animate) { { exitTransitionBackward() } } else { { ExitTransition.None } }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Startup screen (first launch)
        composable<StartupRoute>(
            enterTransition = enterForward,
            exitTransition = exitForward,
            popEnterTransition = enterBackward,
            popExitTransition = exitBackward
        ) {
            StartupScreen(
                onAgree = {
                    navController.navigate(WallpaperModeSelectionRoute) {
                        popUpTo<StartupRoute> { inclusive = true }
                    }
                }
            )
        }

        // Wallpaper mode selection screen
        composable<WallpaperModeSelectionRoute>(
            enterTransition = enterForward,
            exitTransition = exitForward,
            popEnterTransition = enterBackward,
            popExitTransition = exitBackward
        ) {
            com.anthonyla.paperize.presentation.screens.wallpaper_mode_selection.WallpaperModeSelectionScreen(
                onModeSelected = {
                    navController.navigate(NotificationRoute) {
                        popUpTo<WallpaperModeSelectionRoute> { inclusive = true }
                    }
                }
            )
        }

        // Notification permission screen
        composable<NotificationRoute>(
            enterTransition = enterForward,
            exitTransition = exitForward,
            popEnterTransition = enterBackward,
            popExitTransition = exitBackward
        ) {
            NotificationPermissionScreen(
                onContinue = {
                    navController.navigate(StoragePermissionRoute) {
                        popUpTo<NotificationRoute> { inclusive = true }
                    }
                }
            )
        }

        // Storage permission screen
        composable<StoragePermissionRoute>(
            enterTransition = enterForward,
            exitTransition = exitForward,
            popEnterTransition = enterBackward,
            popExitTransition = exitBackward
        ) {
            StoragePermissionScreen(
                onContinue = {
                    onFirstLaunchComplete()
                    navController.navigate(HomeRoute) {
                        popUpTo<StoragePermissionRoute> { inclusive = true }
                    }
                }
            )
        }

        // Home screen (main screen with tabs)
        composable<HomeRoute>(
            enterTransition = enterForward,
            exitTransition = exitForward,
            popEnterTransition = enterBackward,
            popExitTransition = exitBackward
        ) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(SettingsRoute)
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(AlbumRoute(albumId))
                }
            )
        }

        // Album screen
        composable<AlbumRoute>(
            enterTransition = enterForward,
            exitTransition = exitForward,
            popEnterTransition = enterBackward,
            popExitTransition = exitBackward
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<AlbumRoute>()
            AlbumViewScreen(
                albumId = route.albumId,
                onBackClick = { navController.popBackStack() },
                onNavigateToFolder = { folderId ->
                    navController.navigate(FolderRoute(folderId))
                },
                onNavigateToWallpaperView = { wallpaperUri, wallpaperName ->
                    navController.navigate(WallpaperViewRoute(wallpaperUri, wallpaperName))
                },
                onNavigateToSort = {
                    navController.navigate(SortRoute(route.albumId))
                }
            )
        }

        // Sort screen
        composable<SortRoute>(
            enterTransition = enterForward,
            exitTransition = exitForward,
            popEnterTransition = enterBackward,
            popExitTransition = exitBackward
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<SortRoute>()
            SortViewScreen(
                albumId = route.albumId,
                onSaveClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Folder screen
        composable<FolderRoute>(
            enterTransition = enterForward,
            exitTransition = exitForward,
            popEnterTransition = enterBackward,
            popExitTransition = exitBackward
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<FolderRoute>()
            FolderViewScreen(
                folderId = route.folderId,
                onBackClick = { navController.popBackStack() },
                onNavigateToWallpaperView = { wallpaperUri, wallpaperName ->
                    navController.navigate(WallpaperViewRoute(wallpaperUri, wallpaperName))
                }
            )
        }

        // Wallpaper preview screen
        composable<WallpaperViewRoute>(
            enterTransition = enterForward,
            exitTransition = exitForward,
            popEnterTransition = enterBackward,
            popExitTransition = exitBackward
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<WallpaperViewRoute>()
            WallpaperViewScreen(
                wallpaperUri = route.wallpaperUri,
                wallpaperName = route.wallpaperName,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Settings screen
        composable<SettingsRoute>(
            enterTransition = enterForward,
            exitTransition = exitForward,
            popEnterTransition = enterBackward,
            popExitTransition = exitBackward
        ) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate(PrivacyRoute) }
            )
        }

        // Privacy screen
        composable<PrivacyRoute>(
            enterTransition = enterForward,
            exitTransition = exitForward,
            popEnterTransition = enterBackward,
            popExitTransition = exitBackward
        ) {
            PrivacyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
