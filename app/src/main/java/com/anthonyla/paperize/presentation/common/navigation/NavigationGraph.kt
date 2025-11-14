package com.anthonyla.paperize.presentation.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.anthonyla.paperize.presentation.screens.album.AlbumScreen
import com.anthonyla.paperize.presentation.screens.effects.WallpaperEffectsScreen
import com.anthonyla.paperize.presentation.screens.folder.FolderScreen
import com.anthonyla.paperize.presentation.screens.home.HomeScreen
import com.anthonyla.paperize.presentation.screens.privacy.PrivacyScreen
import com.anthonyla.paperize.presentation.screens.settings.SettingsScreen
import com.anthonyla.paperize.presentation.screens.startup.StartupScreen
import com.anthonyla.paperize.presentation.screens.wallpaper.WallpaperScreen

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
        composable<AlbumRoute> {
            AlbumScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWallpaper = { wallpaperId ->
                    navController.navigate(WallpaperRoute(wallpaperId))
                },
                onNavigateToFolder = { folderId ->
                    navController.navigate(FolderRoute(folderId))
                }
            )
        }

        // Folder screen
        composable<FolderRoute> {
            FolderScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWallpaper = { wallpaperId ->
                    navController.navigate(WallpaperRoute(wallpaperId))
                }
            )
        }

        // Wallpaper preview screen
        composable<WallpaperRoute> {
            WallpaperScreen(
                onNavigateBack = { navController.popBackStack() }
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
