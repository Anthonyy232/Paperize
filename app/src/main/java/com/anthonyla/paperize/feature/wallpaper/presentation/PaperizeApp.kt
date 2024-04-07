package com.anthonyla.paperize.feature.wallpaper.presentation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_view_screen.WallpaperViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperScreenViewModel
import com.anthonyla.paperize.feature.wallpaper.util.navigation.NavScreens
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun PaperizeApp(
    albumsViewModel: AlbumsViewModel,
    settingsViewModel: SettingsViewModel,
    wallpaperScreenViewModel: WallpaperScreenViewModel
) {
    val albumState = albumsViewModel.state.collectAsStateWithLifecycle()
    val selectedState = wallpaperScreenViewModel.state.collectAsStateWithLifecycle()

    // React to albumState changes and change selectedAlbum's details to keep it from being stale
    LaunchedEffect(albumState.value.albumsWithWallpapers) {
        selectedState.value.selectedAlbum?.let { selectedAlbum ->
            albumState.value.albumsWithWallpapers.find { it.album.initialAlbumName == selectedAlbum.album.initialAlbumName }
                ?.let { foundAlbum -> wallpaperScreenViewModel.onEvent(WallpaperEvent.UpdateSelectedAlbum(foundAlbum)) }
                ?: wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset)
        }
    }

    val navController = rememberNavController()

    // Contact author popup with email
    var toContact by rememberSaveable { mutableStateOf(false) }
    if (toContact) {
        Contact(LocalContext.current)
        toContact = false
    }

    NavHost(
        navController = navController,
        startDestination = NavScreens.Home.route,
        modifier = Modifier.navigationBarsPadding()
    ) {
        // Navigate to the home screen with the navbar and top bar
        composable(
            route = NavScreens.Home.route,
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
            }
        ) {
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
        // Navigate to the add album screen to create a new album and add wallpapers to it
        composable(
            route = NavScreens.AddEdit.route.plus("/{initialAlbumName}"),
            arguments = listOf(navArgument("initialAlbumName") { type = NavType.StringType })
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
        // Navigate to wallpaper view screen to view individual wallpapers in full screen
        composable(
            route = NavScreens.WallpaperView.route.plus("/{wallpaperUri}"),
            arguments = listOf(navArgument("wallpaperUri") { type = NavType.StringType })
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
        // Navigate to folder view screen to view wallpapers in a particular folder
        composable(
            route = NavScreens.FolderView.route.plus("/{folderUri}/{folderName}/{wallpapers}"),
            arguments = listOf(
                navArgument("folderUri") { type = NavType.StringType },
                navArgument("folderName") { type = NavType.StringType },
                navArgument("wallpapers") { type = NavType.StringType }
            )
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
        // Navigate to the album view screen to view folders and wallpapers in an album
        composable(
            route = NavScreens.AlbumView.route.plus("/{initialAlbumName}"),
            arguments = listOf(
                navArgument("initialAlbumName") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val initialAlbumName = backStackEntry.arguments?.getString("initialAlbumName")
            val albumWithWallpaper = albumState.value.albumsWithWallpapers.find { it.album.initialAlbumName == initialAlbumName }
            if (initialAlbumName != null && albumWithWallpaper != null) {
                AlbumViewScreen(
                    album = albumWithWallpaper,
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
                    },
                    onSelectionDeleted = {
                        albumsViewModel.onEvent(AlbumsEvent.RefreshAlbums)
                    }
                )
            }
        }
        // Navigate to the settings screen to change app settings
        composable(
            route = NavScreens.Settings.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                )
            }
        ) {
            SettingsScreen(
                settingsState = settingsViewModel.state,
                onBackClick = { navController.navigateUp() },
                onDarkModeClick = {
                    settingsViewModel.onEvent(SettingsEvent.SetDarkMode(it))
                },
                onDynamicThemingClick = {
                    settingsViewModel.onEvent(SettingsEvent.SetDynamicTheming(it))
                }
            )
        }
    }
}

// Encode an URI so it can be passed with navigation
suspend fun encodeUri(uri: String): String =
    withContext(Dispatchers.IO) {
        URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
}