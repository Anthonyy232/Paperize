package com.anthonyla.paperize.feature.wallpaper.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.AlbumViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen.FolderViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.HomeScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.licenses_screen.LicensesScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_view_screen.WallpaperViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.notifications_screen.NotificationScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.privacy_screen.PrivacyScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.startup_screen.StartupScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperScreenViewModel
import com.anthonyla.paperize.feature.wallpaper.util.navigation.NavConstants.INITIAL_OFFSET
import com.anthonyla.paperize.feature.wallpaper.util.navigation.NavScreens
import com.anthonyla.paperize.feature.wallpaper.util.navigation.sharedXTransitionIn
import com.anthonyla.paperize.feature.wallpaper.util.navigation.sharedXTransitionOut
import com.anthonyla.paperize.feature.wallpaper.wallpaperservice.WallpaperService
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun PaperizeApp(
    firstLaunch: Boolean,
    albumsViewModel: AlbumsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    wallpaperScreenViewModel: WallpaperScreenViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val albumState = albumsViewModel.state.collectAsStateWithLifecycle()
    val selectedState = wallpaperScreenViewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // React to albumState changes and change selectedAlbum's details to keep it from being stale
    LaunchedEffect(albumState.value.albumsWithWallpapers) {
        albumState.value.albumsWithWallpapers.asSequence().forEach { albumWithWallpapers ->
            if (!albumWithWallpapers.album.initialized && (albumWithWallpapers.wallpapers.isNotEmpty() || albumWithWallpapers.folders.isNotEmpty())) {
                albumsViewModel.onEvent(AlbumsEvent.InitializeAlbum(albumWithWallpapers))
            } else if (albumWithWallpapers.album.initialized && albumWithWallpapers.wallpapers.isEmpty() && albumWithWallpapers.folders.isEmpty()) {
                if (navController.currentDestination?.route != NavScreens.Home.route) {
                    navController.popBackStack(NavScreens.Home.route, false)
                }
                albumsViewModel.onEvent(AlbumsEvent.DeleteAlbumWithWallpapers(albumWithWallpapers))
            }
        }

        selectedState.value.selectedAlbum?.let { selectedAlbum ->
            albumState.value.albumsWithWallpapers.find { it.album.initialAlbumName == selectedAlbum.album.initialAlbumName }
                ?.let { foundAlbum ->
                    val albumNameHashCode = foundAlbum.album.initialAlbumName.hashCode()
                    val wallpapers: List<Wallpaper> =
                        foundAlbum.wallpapers + foundAlbum.folders.flatMap { folder ->
                            folder.wallpapers.map { wallpaper ->
                                val wallpaperHashCode = wallpaper.hashCode()
                                Wallpaper(
                                    initialAlbumName = foundAlbum.album.initialAlbumName,
                                    wallpaperUri = wallpaper,
                                    key = wallpaperHashCode + albumNameHashCode,
                                )
                            }
                        }
                    val wallpaperUriSet = wallpapers.map { it.wallpaperUri }.toSet()
                    val newSelectedAlbum = SelectedAlbum(
                        album = foundAlbum.album.copy(
                            wallpapersInQueue = selectedAlbum.album.wallpapersInQueue.filter { it in wallpaperUriSet }
                        ),
                        wallpapers = wallpapers
                    )
                    wallpaperScreenViewModel.onEvent(
                        WallpaperEvent.UpdateSelectedAlbum(newSelectedAlbum)
                    )
                }
                ?: run {
                    wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset)
                    Intent(context, WallpaperService::class.java).also {
                        it.action = WallpaperService.Actions.STOP.toString()
                        context.startForegroundService(it)
                    }
                }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (firstLaunch) NavScreens.Startup.route else NavScreens.Home.route,
        modifier = Modifier.navigationBarsPadding()
    ) {
        // Navigate to the startup screen to show the privacy policy and notification screen
        composable(
            route = NavScreens.Startup.route,
            enterTransition = {
                sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
            },
            exitTransition = {
                sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
            },
            popEnterTransition = {
                sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
            },
            popExitTransition = {
                sharedXTransitionOut(target = { (it * INITIAL_OFFSET).toInt() })
            },
        ) {
            StartupScreen(
                onAgree = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        settingsViewModel.onEvent(SettingsEvent.SetFirstLaunch)
                        navController.navigate(NavScreens.Home.route) {
                            popUpTo(NavScreens.Startup.route) { inclusive = true }
                            popUpTo(NavScreens.Notification.route) { inclusive = true }
                        }
                    }
                    else {
                        navController.navigate(NavScreens.Notification.route)
                    }
                }
            )
        }
        // Navigate to the notification screen to ask for notification permission
        composable(
            route = NavScreens.Notification.route,
            enterTransition = {
                sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
            },
            exitTransition = {
                sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
            },
            popEnterTransition = {
                sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
            },
            popExitTransition = {
                sharedXTransitionOut(target = { (it * INITIAL_OFFSET).toInt() })
            },
        ) {
            NotificationScreen(
                onAgree = {
                    settingsViewModel.onEvent(SettingsEvent.SetFirstLaunch)
                    navController.navigate(NavScreens.Home.route) {
                        popUpTo(NavScreens.Startup.route) { inclusive = true }
                        popUpTo(NavScreens.Notification.route) { inclusive = true }
                    }
                }
            )
        }
        // Navigate to the home screen to view all albums and wallpapers
        composable(
            route = NavScreens.Home.route,
            enterTransition = {
                sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
            },
            exitTransition = {
                sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
            },
            popEnterTransition = {
                sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
            },
            popExitTransition = {
                sharedXTransitionOut(target = { (it * INITIAL_OFFSET).toInt() })
            },
        ) {
            HomeScreen(
                onSettingsClick = { navController.navigate(NavScreens.Settings.route) },
                navigateToAddWallpaperScreen = {
                    navController.navigate("${NavScreens.AddEdit.route}/$it")
                },
                onAlbumViewClick = {
                    navController.navigate("${NavScreens.AlbumView.route}/$it")
                },
                onScheduleWallpaperChanger = { timeInMinutes ->
                    settingsViewModel.onEvent(SettingsEvent.SetWallpaperInterval(timeInMinutes))
                    val intent = Intent(context, WallpaperService::class.java).apply {
                        action = WallpaperService.Actions.START.toString()
                        putExtra("timeInMinutes", timeInMinutes)
                    }
                    context.startForegroundService(intent)
                },
                onStop = {
                    Intent(context, WallpaperService::class.java).also {
                        it.action = WallpaperService.Actions.STOP.toString()
                        context.startForegroundService(it)
                    }
                },
            )
        }
        // Navigate to the add album screen to create a new album and add wallpapers to it
        composable(
            route = NavScreens.AddEdit.route.plus("/{initialAlbumName}"),
            arguments = listOf(navArgument("initialAlbumName") { type = NavType.StringType }),
            enterTransition = {
                sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
            },
            exitTransition = {
                sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
            },
            popEnterTransition = {
                sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
            },
            popExitTransition = {
                sharedXTransitionOut(target = { (it * INITIAL_OFFSET).toInt() })
            }
        ) {backStackEntry ->
            backStackEntry.arguments?.getString("initialAlbumName").let {
                if (it != null) {
                    AddAlbumScreen(
                        initialAlbumName = it,
                        onBackClick = { navController.navigateUp() },
                        onConfirmation = {
                            navController.navigateUp()
                        },
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
            arguments = listOf(navArgument("wallpaperUri") { type = NavType.StringType }),
            enterTransition = {
                sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
            },
            exitTransition = {
                sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
            },
            popEnterTransition = {
                sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
            },
            popExitTransition = {
                sharedXTransitionOut(target = { (it * INITIAL_OFFSET).toInt() })
            },
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
            ),
            enterTransition = {
                sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
            },
            exitTransition = {
                sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
            },
            popEnterTransition = {
                sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
            },
            popExitTransition = {
                sharedXTransitionOut(target = { (it * INITIAL_OFFSET).toInt() })
            },
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
            ),
            enterTransition = {
                sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
            },
            exitTransition = {
                sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
            },
            popEnterTransition = {
                sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
            },
            popExitTransition = {
                sharedXTransitionOut(target = { (it * INITIAL_OFFSET).toInt() })
            },
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
                sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
            },
            exitTransition = {
                sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
            },
            popEnterTransition = {
                sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
            },
            popExitTransition = {
                sharedXTransitionOut(target = { (it * INITIAL_OFFSET).toInt() })
            },
        ) {
            SettingsScreen(
                settingsState = settingsViewModel.state,
                onBackClick = { navController.navigateUp() },
                onDarkModeClick = {
                    settingsViewModel.onEvent(SettingsEvent.SetDarkMode(it))
                },
                onDynamicThemingClick = {
                    settingsViewModel.onEvent(SettingsEvent.SetDynamicTheming(it))
                },
                onAnimateClick = {
                    settingsViewModel.onEvent(SettingsEvent.SetAnimate(it))
                },
                onPrivacyClick = {
                    navController.navigate(NavScreens.Privacy.route)
                },
                onLicenseClick = {
                    navController.navigate(NavScreens.Licenses.route)
                },
            )
        }
        // Navigate to the privacy screen to view the privacy policy
        composable(
            route = NavScreens.Privacy.route,
            enterTransition = {
                sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
            },
            exitTransition = {
                sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
            },
            popEnterTransition = {
                sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
            },
            popExitTransition = {
                sharedXTransitionOut(target = { (it * INITIAL_OFFSET).toInt() })
            },
        ) {
            PrivacyScreen(
                onBackClick = { navController.navigateUp() },
            )
        }
        // Navigate to the licenses screen to view the licenses of the libraries used
        composable(
            route = NavScreens.Licenses.route,
            enterTransition = {
                sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
            },
            exitTransition = {
                sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
            },
            popEnterTransition = {
                sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
            },
            popExitTransition = {
                sharedXTransitionOut(target = { (it * INITIAL_OFFSET).toInt() })
            },
        ) {
            LicensesScreen(
                onBackClick = { navController.navigateUp() },
            )
        }
    }
}

// Encode an URI so it can be passed with navigation
suspend fun encodeUri(uri: String): String =
    withContext(Dispatchers.IO) {
        URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
}