package com.anthonyla.paperize.feature.wallpaper.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.AlbumViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen.FolderViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.HomeScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.licenses_screen.LicensesScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.notifications_screen.NotificationScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.privacy_screen.PrivacyScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.startup_screen.StartupScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperScreenViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_view_screen.WallpaperViewScreen
import com.anthonyla.paperize.feature.wallpaper.util.navigation.AddEdit
import com.anthonyla.paperize.feature.wallpaper.util.navigation.AlbumView
import com.anthonyla.paperize.feature.wallpaper.util.navigation.FolderView
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Home
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Licenses
import com.anthonyla.paperize.feature.wallpaper.util.navigation.NavConstants.INITIAL_OFFSET
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Notification
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Privacy
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Settings
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Startup
import com.anthonyla.paperize.feature.wallpaper.util.navigation.WallpaperView
import com.anthonyla.paperize.feature.wallpaper.util.navigation.sharedXTransitionIn
import com.anthonyla.paperize.feature.wallpaper.util.navigation.sharedXTransitionOut
import com.anthonyla.paperize.feature.wallpaper.wallpaperservice.WallpaperService

@Composable
fun PaperizeApp(
    firstLaunch: Boolean,
    albumsViewModel: AlbumsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    wallpaperScreenViewModel: WallpaperScreenViewModel = hiltViewModel(),
    addAlbumViewModel: AddAlbumViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val albumState = albumsViewModel.state.collectAsStateWithLifecycle()
    val selectedState = wallpaperScreenViewModel.state.collectAsStateWithLifecycle()
    val settingsState = settingsViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // React to albumState changes and change selectedAlbum's details to keep it from being stale
    LaunchedEffect(albumState.value.albumsWithWallpapers) {
        albumState.value.albumsWithWallpapers.asSequence().forEach { albumWithWallpapers ->
            if (!albumWithWallpapers.album.initialized && (albumWithWallpapers.wallpapers.isNotEmpty() || albumWithWallpapers.folders.isNotEmpty())) {
                albumsViewModel.onEvent(AlbumsEvent.InitializeAlbum(albumWithWallpapers))
            } else if (albumWithWallpapers.album.initialized && albumWithWallpapers.wallpapers.isEmpty() && albumWithWallpapers.folders.isEmpty()) {
                if (navController.currentDestination?.route == Home::class.simpleName) {
                    navController.popBackStack<Home>(inclusive = false)
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
                    wallpaperScreenViewModel.onEvent(WallpaperEvent.UpdateSelectedAlbum(newSelectedAlbum))
                    settingsViewModel.onEvent(SettingsEvent.SetWallpaperInterval(settingsState.value.interval))
                    val intent = Intent(context, WallpaperService::class.java).apply {
                        action = WallpaperService.Actions.START.toString()
                        putExtra("timeInMinutes", settingsState.value.interval)
                    }
                    context.startForegroundService(intent)
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
        startDestination = if (firstLaunch) Startup else Home,
        modifier = Modifier.navigationBarsPadding()
    ) {
        // Navigate to the startup screen to show the privacy policy and notification screen
        composable<Startup> (
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
                        navController.navigate(Home) {
                            popUpTo<Startup> { inclusive = true }
                        }
                    }
                    else { navController.navigate(Notification) }
                }
            )
        }
        // Navigate to the notification screen to ask for notification permission
        composable<Notification> (
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
                    navController.navigate(Home) {
                        popUpTo<Notification> { inclusive = true }
                    }
                }
            )
        }
        // Navigate to the home screen to view all albums and wallpapers
        composable<Home> (
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
                onSettingsClick = { navController.navigate(Settings) },
                navigateToAddWallpaperScreen = {
                    navController.navigate(AddEdit(it))
                },
                onAlbumViewClick = {
                    navController.navigate(AlbumView(it))
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
                    wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset)
                    Intent(context, WallpaperService::class.java).also {
                        it.action = WallpaperService.Actions.STOP.toString()
                        context.startForegroundService(it)
                    }
                },
                animate = settingsState.value.animate,
                interval = settingsState.value.interval,
                setLockWithHome = settingsState.value.setLockWithHome,
                lastSetTime = settingsState.value.lastSetTime,
                nextSetTime = settingsState.value.nextSetTime,
                onSetLockWithHome = {
                    settingsViewModel.onEvent(SettingsEvent.SetLockWithHome(it))
                },
                selectedAlbum = selectedState.value.selectedAlbum
            )
        }
        // Navigate to the add album screen to create a new album and add wallpapers to it
        composable<AddEdit>(
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
            val addEdit: AddEdit = backStackEntry.toRoute()
            AddAlbumScreen(
                initialAlbumName = addEdit.wallpaper,
                onBackClick = { navController.navigateUp() },
                onConfirmation = { navController.navigateUp() },
                onShowWallpaperView = { wallpaper ->
                    navController.navigate(WallpaperView(wallpaper))
                },
                onShowFolderView = { folderName, wallpapers ->
                    navController.navigate(FolderView(folderName, wallpapers))
                }
            )
        }
        // Navigate to wallpaper view screen to view individual wallpapers in full screen
        composable<WallpaperView> (
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
            val wallpaperView: WallpaperView = backStackEntry.toRoute()
            WallpaperViewScreen(
                wallpaperUri = wallpaperView.wallpaper,
                onBackClick = { navController.navigateUp() },
                animate = settingsState.value.animate
            )
        }
        // Navigate to folder view screen to view wallpapers in a particular folder
        composable<FolderView> (
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
            val folderView: FolderView = backStackEntry.toRoute()
            FolderViewScreen(
                folderName = folderView.folderName,
                wallpapers = folderView.wallpapers,
                onBackClick = { navController.navigateUp() },
                onShowWallpaperView = {
                    navController.navigate(WallpaperView(it))
                },
                animate = settingsState.value.animate
            )
        }
        // Navigate to the album view screen to view folders and wallpapers in an album
        composable<AlbumView> (
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
            val albumView: AlbumView = backStackEntry.toRoute()
            val albumWithWallpaper = albumState.value.albumsWithWallpapers.find { it.album.initialAlbumName == albumView.initialAlbumName }
            if (albumWithWallpaper != null) {
                AlbumViewScreen(
                    album = albumWithWallpaper,
                    onBackClick = { navController.navigateUp() },
                    onShowWallpaperView = {
                        navController.navigate(WallpaperView(it))
                    },
                    onShowFolderView = { folderName, wallpapers ->
                        navController.navigate(FolderView(folderName, wallpapers))
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
            } else { navController.navigateUp() }
        }
        // Navigate to the settings screen to change app settings
        composable<Settings> (
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
                    navController.navigate(Privacy)
                },
                onLicenseClick = {
                    navController.navigate(Licenses)
                },
                onResetClick = {
                    navController.navigate(Startup) {
                        popUpTo<Settings> { inclusive = true }
                    }
                    settingsViewModel.onEvent(SettingsEvent.Reset)
                    wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset)
                    albumsViewModel.onEvent(AlbumsEvent.Reset)
                    addAlbumViewModel.onEvent(AddAlbumEvent.Reset)
                    Intent(context, WallpaperService::class.java).also {
                        it.action = WallpaperService.Actions.STOP.toString()
                        context.startForegroundService(it)
                    }
                    val contentResolver = context.contentResolver
                    val persistedUris = contentResolver.persistedUriPermissions
                    for (permission in persistedUris) {
                        contentResolver.releasePersistableUriPermission(permission.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                }
            )
        }
        // Navigate to the privacy screen to view the privacy policy
        composable<Privacy> (
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
        composable<Licenses> (
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