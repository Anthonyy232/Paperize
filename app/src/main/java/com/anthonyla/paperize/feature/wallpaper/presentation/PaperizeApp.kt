package com.anthonyla.paperize.feature.wallpaper.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anthonyla.paperize.data.Contact
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen.FolderViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.HomeScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_view_screen.WallpaperViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsScreen
import com.anthonyla.paperize.feature.wallpaper.util.navigation.NavScreens
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun PaperizeApp(
    addAlbumViewModel: AddAlbumViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    //albumsViewModel: AlbumsViewModel = hiltViewModel()
) {
    val addAlbumState = addAlbumViewModel.state.collectAsStateWithLifecycle()
    val settingsState = settingsViewModel.state.value
    val navController = rememberNavController()
    var toContact by rememberSaveable { mutableStateOf(false) }
    if (toContact) { Contact(LocalContext.current) }

    NavHost(
        navController = navController,
        startDestination = NavScreens.Home.route,
        modifier = Modifier.navigationBarsPadding()
    ) {
        composable(route = NavScreens.Home.route) {
            HomeScreen(
                onSettingsClick = { navController.navigate(NavScreens.Settings.route) },
                onContactClick = { toContact = true },
                navigateToAddWallpaperScreen = {
                    navController.navigate("${NavScreens.AddEdit.route}/$it")
                }
            )
        }
        composable(
            route = NavScreens.AddEdit.route.plus("/{initialAlbumName}"),
            arguments = listOf(navArgument("initialAlbumName") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300, easing = LinearEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300, easing = LinearEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300, easing = LinearEasing))
            }
        ) {backStackEntry ->
            backStackEntry.arguments?.getString("initialAlbumName").let {
                if (it != null) {
                    AddAlbumScreen(
                        initialAlbumName = it,
                        onBackClick = {
                            navController.navigateUp()
                            addAlbumViewModel.onEvent(AddAlbumEvent.ClearState)
                        },
                        onConfirmation = {
                            addAlbumViewModel.onEvent(AddAlbumEvent.SaveAlbum)
                        },
                        onShowWallpaperView = { wallpaper ->
                            val encodedWallpaper = runBlocking { encodeUri(uri = wallpaper) }
                            navController.navigate("${NavScreens.WallpaperView.route}/$encodedWallpaper")
                        },
                        onShowFolderView = { folder, folderName, wallpapers ->
                            val encodedFolder = runBlocking { encodeUri(uri = folder) }
                            val encodedWallpapers = runBlocking { encodeUri(uri = Gson().toJson(wallpapers)) }
                            navController.navigate("${NavScreens.AlbumView.route}/$encodedFolder/$folderName/$encodedWallpapers")
                        }
                    )
                }
            }
        }
        composable(
            route = NavScreens.WallpaperView.route.plus("/{wallpaperUri}"),
            arguments = listOf(navArgument("wallpaperUri") { type = NavType.StringType }),
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = LinearEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300, easing = LinearEasing))
            }
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("wallpaperUri").let { wallpaper ->
                if (wallpaper != null) {
                    WallpaperViewScreen(
                        wallpaper = wallpaper,
                        onBackClick = { navController.navigateUp() },
                    )
                }
            }
        }
        composable(
            route = NavScreens.AlbumView.route.plus("/{folderUri}/{folderName}/{wallpapers}"),
            arguments = listOf(
                navArgument("folderUri") { type = NavType.StringType },
                navArgument("folderName") { type = NavType.StringType },
                navArgument("wallpapers") { type = NavType.StringType }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) { backStackEntry ->
            val folderUri = backStackEntry.arguments?.getString("folderUri")
            val folderName = backStackEntry.arguments?.getString("folderName")
            val wallpapers = Gson().fromJson(
                backStackEntry.arguments?.getString("wallpapers"),
                Array<String>::class.java
            ).toList()

            if (folderUri != null) {
                FolderViewScreen(
                    folder = folderUri,
                    folderName = folderName,
                    wallpapers = wallpapers,
                    onBackClick = { navController.navigateUp() },
                    onShowWallpaperView = { wallpaper ->
                        val encodedWallpaper = runBlocking { encodeUri(uri = wallpaper) }
                        navController.navigate("${NavScreens.WallpaperView.route}/$encodedWallpaper")
                    }
                )
            }
        }
        composable(
            route = NavScreens.Settings.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(300)
                )
            }
        ) {
            SettingsScreen(
                settingsState = settingsState,
                onBackClick = { navController.navigateUp() },
                onDynamicThemingClick = {
                    settingsViewModel.onEvent(SettingsEvent.SetDynamicTheming(it))
                },
                onDarkModeClick = {
                    settingsViewModel.onEvent(SettingsEvent.SetDarkMode(it))
                }
            )
        }
    }
}

suspend fun encodeUri(uri: String): String = withContext(Dispatchers.IO) {
    URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
}