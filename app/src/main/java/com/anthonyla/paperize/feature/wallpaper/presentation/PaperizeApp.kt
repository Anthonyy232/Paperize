package com.anthonyla.paperize.feature.wallpaper.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.AlbumViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen.FolderViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.HomeScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_view_screen.WallpaperViewScreen
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
    albumsViewModel: AlbumsViewModel = hiltViewModel()
) {
    val state = albumsViewModel.state.collectAsStateWithLifecycle()
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
                },
                onAlbumViewClick = {
                    navController.navigate("${NavScreens.AlbumView.route}/$it")
                }
            )
        }
        composable(
            route = NavScreens.AddEdit.route.plus("/{initialAlbumName}"),
            arguments = listOf(navArgument("initialAlbumName") { type = NavType.StringType }),
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = LinearEasing))
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
                        onBackClick = { navController.navigateUp() },
                        onConfirmation = { navController.navigateUp() },
                        onShowWallpaperView = { wallpaper ->
                            val encodedWallpaper = runBlocking { encodeUri(uri = wallpaper) }
                            navController.navigate("${NavScreens.WallpaperView.route}/$encodedWallpaper")
                        },
                        onShowFolderView = { folder, folderName, wallpapers ->
                            val encodedFolder = runBlocking { encodeUri(uri = folder) }
                            val encodedWallpapers = runBlocking { encodeUri(uri = Gson().toJson(wallpapers)) }
                            navController.navigate("${NavScreens.FolderView.route}/$encodedFolder/$folderName/$encodedWallpapers")
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
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300, easing = LinearEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300, easing = LinearEasing))
            }
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("wallpaperUri").let { wallpaper ->
                if (wallpaper != null) {
                    WallpaperViewScreen(
                        wallpaperUri = wallpaper,
                        onBackClick = { navController.navigateUp() },
                    )
                }
            }
        }
        composable(
            route = NavScreens.FolderView.route.plus("/{folderUri}/{folderName}/{wallpapers}"),
            arguments = listOf(
                navArgument("folderUri") { type = NavType.StringType },
                navArgument("folderName") { type = NavType.StringType },
                navArgument("wallpapers") { type = NavType.StringType }
            ),
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = LinearEasing))
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
            route = NavScreens.AlbumView.route.plus("/{initialAlbumName}"),
            arguments = listOf(
                navArgument("initialAlbumName") { type = NavType.StringType },
            ),
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = LinearEasing))
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
        ) { backStackEntry ->
            val initialAlbumName = backStackEntry.arguments?.getString("initialAlbumName")
            val albumWithWallpaper = state.value.albumWithWallpapers.find { it.album.initialAlbumName == initialAlbumName }
            if (initialAlbumName != null) {
                if (albumWithWallpaper != null) {
                    AlbumViewScreen(
                        albumWithWallpaper = albumWithWallpaper,
                        onBackClick = { navController.navigateUp() },
                        onShowWallpaperView = { wallpaper ->
                            val encodedWallpaper = runBlocking { encodeUri(uri = wallpaper) }
                            navController.navigate("${NavScreens.WallpaperView.route}/$encodedWallpaper")
                        },
                        onShowFolderView = { folder, folderName, wallpapers ->
                            val encodedFolder = runBlocking { encodeUri(uri = folder) }
                            val encodedWallpapers = runBlocking { encodeUri(uri = Gson().toJson(wallpapers)) }
                            navController.navigate("${NavScreens.FolderView.route}/$encodedFolder/$folderName/$encodedWallpapers")
                        },
                        onDeleteAlbum = {
                            albumsViewModel.onEvent(AlbumsEvent.DeleteAlbumWithWallpapers(albumWithWallpaper))
                            navController.navigateUp()
                        },
                        onTitleChange = { title, originalAlbumWithWallpaper ->
                            albumsViewModel.onEvent(AlbumsEvent.ChangeAlbumName(title, originalAlbumWithWallpaper))
                        }
                    )
                }
            }
        }
        composable(
            route = NavScreens.Settings.route,
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = LinearEasing))
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
        ) {
            SettingsScreen(onBackClick = { navController.navigateUp() })
        }
    }
}

suspend fun encodeUri(uri: String): String =
    withContext(Dispatchers.IO) {
        URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
}