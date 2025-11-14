package com.anthonyla.paperize.presentation.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.anthonyla.paperize.presentation.screens.album_view.AlbumViewScreen
import com.anthonyla.paperize.presentation.screens.effects.WallpaperEffectsScreen
import com.anthonyla.paperize.presentation.screens.folder_view.FolderViewScreen
import com.anthonyla.paperize.presentation.screens.home.HomeScreen
import com.anthonyla.paperize.presentation.screens.notification.NotificationPermissionScreen
import com.anthonyla.paperize.presentation.screens.privacy.PrivacyScreen
import com.anthonyla.paperize.presentation.screens.settings.SettingsScreen
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
    onFirstLaunchComplete: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Startup screen (first launch)
        composable<StartupRoute> {
            StartupScreen(
                onAgree = {
                    navController.navigate(NotificationRoute) {
                        popUpTo<StartupRoute> { inclusive = true }
                    }
                }
            )
        }

        // Notification permission screen
        composable<NotificationRoute> {
            NotificationPermissionScreen(
                onContinue = {
                    navController.navigate(StoragePermissionRoute) {
                        popUpTo<NotificationRoute> { inclusive = true }
                    }
                }
            )
        }

        // Storage permission screen
        composable<StoragePermissionRoute> {
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

        // Album screen
        composable<AlbumRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AlbumRoute>()
            AlbumViewScreen(
                albumId = route.albumId,
                onBackClick = { navController.popBackStack() },
                onNavigateToFolder = { folderId ->
                    navController.navigate(FolderRoute(folderId))
                },
                onNavigateToWallpaperView = { wallpaperUri, wallpaperName ->
                    navController.navigate(WallpaperViewRoute(wallpaperUri, wallpaperName))
                }
            )
        }

        // Folder screen
        composable<FolderRoute> { backStackEntry ->
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
        composable<WallpaperViewRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<WallpaperViewRoute>()
            WallpaperViewScreen(
                wallpaperUri = route.wallpaperUri,
                wallpaperName = route.wallpaperName,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Effects screen
        composable<EffectsRoute> {
            WallpaperEffectsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings screen
        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate(PrivacyRoute) },
                onNavigateToEffects = { navController.navigate(EffectsRoute) }
            )
        }

        // Privacy screen
        composable<PrivacyRoute> {
            PrivacyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
