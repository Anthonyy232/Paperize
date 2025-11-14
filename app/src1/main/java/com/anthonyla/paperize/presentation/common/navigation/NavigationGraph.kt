package com.anthonyla.paperize.presentation.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.anthonyla.paperize.presentation.screens.home.HomeScreen
import com.anthonyla.paperize.presentation.screens.privacy.PrivacyScreen
import com.anthonyla.paperize.presentation.screens.settings.SettingsScreen
import com.anthonyla.paperize.presentation.screens.startup.StartupScreen

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
                    onFirstLaunchComplete()
                    navController.navigate(HomeRoute) {
                        popUpTo<StartupRoute> { inclusive = true }
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
                },
                onNavigateToFolder = { folderId ->
                    navController.navigate(FolderRoute(folderId))
                }
            )
        }

        // Album screen
        composable<AlbumRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AlbumRoute>()
            // AlbumScreen(albumId = route.albumId, onNavigateBack = { navController.popBackStack() })
        }

        // Folder screen
        composable<FolderRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<FolderRoute>()
            // FolderScreen(folderId = route.folderId, onNavigateBack = { navController.popBackStack() })
        }

        // Wallpaper preview screen
        composable<WallpaperRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<WallpaperRoute>()
            // WallpaperScreen(wallpaperId = route.wallpaperId, onNavigateBack = { navController.popBackStack() })
        }

        // Sort screen
        composable<SortRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<SortRoute>()
            // SortScreen(albumId = route.albumId, onNavigateBack = { navController.popBackStack() })
        }

        // Settings screen
        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate(PrivacyRoute) }
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
